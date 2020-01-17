/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core.auth;

import io.lumeer.api.model.User;
import io.lumeer.core.facade.ConfigurationFacade;
import io.lumeer.core.facade.SentryFacade;

import com.auth0.SessionUtils;
import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.UserInfo;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.net.Request;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = "/*")
public class Auth0Filter implements Filter {

   private static final long TOKEN_REFRESH_PERIOD = 10L * 60 * 1000; // 10 minutes
   private static final String VIEW_ID = "view_id";
   private static final String CORRELATION_ID = "correlation_id";
   private static final String TIMESTAMP_HEADER = "X-Lumeer-Start-Timestamp";
   private static final String LOCALE_HEADER = "X-Lumeer-Locale";

   @Inject
   private Logger log;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private SentryFacade sentryFacade;

   private Map<String, AuthenticatedUser.AuthUserInfo> authUserCache = new ConcurrentHashMap<>();
   private Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

   private JWTVerifier verifier = null;
   private String domain;
   private String clientId;
   private String clientSecret;

   private ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());

   private AtomicLong lastCheck = new AtomicLong(System.currentTimeMillis());

   @Override
   public void init(final FilterConfig filterConfig) throws ServletException {
      if (System.getenv("SKIP_SECURITY") == null) {
         domain = filterConfig.getServletContext().getInitParameter("com.auth0.domain");
         clientId = filterConfig.getServletContext().getInitParameter("com.auth0.clientId");
         clientSecret = filterConfig.getServletContext().getInitParameter("com.auth0.clientSecret");
         verifier = AuthenticationControllerProvider.getVerifier(domain);
      }
   }

   @Override
   public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
      // clean caches in a background thread, only one task at a time, checks for clean interval of 60s
      executor.submit(this::cleanCache);

      final HttpServletRequest req = (HttpServletRequest) servletRequest;
      final HttpServletResponse res = (HttpServletResponse) servletResponse;

      addCorsHeaders(req, res);

      parseViewId(req);
      parseRequestData(req);
      processStartTimestamp(req, res);
      processUserLocale(req, res);

      if (System.getenv("SKIP_SECURITY") != null) {
         fakeUserLogin(req); // try to consume test user from request header
         filterChain.doFilter(servletRequest, servletResponse);
         return;
      }

      if (req.getMethod().equals("OPTIONS")) {
         filterChain.doFilter(servletRequest, servletResponse);
         return;
      }

      if (req.getPathInfo() == null || !req.getPathInfo().startsWith("/paymentNotify/")) {
         final String accessToken = getAccessToken(req);

         // we do not have the token at all, or we failed to obtain verifier
         if (accessToken == null || verifier == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         // we failed to verify the token
         final DecodedJWT jwt;
         try {
            jwt = JWT.decode(accessToken);
            verifier.verify(jwt.getToken());
         } catch (Exception e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         // the token is expired
         if (Instant.now().isAfter(jwt.getExpiresAt().toInstant())) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         // we are safe to go, make sure we have user info
         final AuthenticatedUser.AuthUserInfo authUserInfo = getAuthenticatedUser(accessToken);

         if (!accessToken.equals(authUserInfo.accessToken) || authUserInfo.user == null || authUserInfo.lastUpdated + TOKEN_REFRESH_PERIOD <= System.currentTimeMillis()) {
            final Semaphore s = semaphores.computeIfAbsent(accessToken, key -> new Semaphore(1));
            if (s.tryAcquire()) { // only one thread must do that at the same time
               try {
                  final AuthenticatedUser.AuthUserInfo newAuthUserInfo = new AuthenticatedUser.AuthUserInfo();

                  // try to get user info 3 times in a row with 500ms delays
                  for (int i = 0; i < 3 && newAuthUserInfo.user == null; i++) {
                     try {
                        newAuthUserInfo.user = getUserInfo(accessToken);
                     } catch (Auth0Exception a0e) {
                        try {
                           Thread.sleep(500);
                        } catch (InterruptedException ie) {
                           // NOP
                        }
                     }
                  }

                  // we still could not get user info
                  if (newAuthUserInfo.user == null) {
                     res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                     s.release();
                     return;
                  }

                  newAuthUserInfo.accessToken = accessToken;
                  newAuthUserInfo.lastUpdated = System.currentTimeMillis();
                  authUserCache.put(accessToken, newAuthUserInfo);
                  authenticatedUser.setAuthUserInfo(newAuthUserInfo);
                  authenticatedUser.checkUser();
               } finally {
                  s.release();
               }
            } else {
               try {
                  s.acquire();
                  s.release();
                  // we might have a different session id for the same user
                  if (authenticatedUser.getAuthUserInfo().user == null) {
                     AuthenticatedUser.AuthUserInfo newAuthUserInfo = authUserCache.get(accessToken);
                     authenticatedUser.setAuthUserInfo(newAuthUserInfo);
                     // we do not need to check the user again, it was already done in the first session
                  }
               } catch (InterruptedException ie) {
                  // do nothing
               }
            }
         }

      }

      try {
         filterChain.doFilter(servletRequest, servletResponse);
      } catch (RuntimeException e) {
         log.log(Level.SEVERE, "Unable to serve request: ", e);
         sentryFacade.reportError(e);
         res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
      }
   }

   private AuthenticatedUser.AuthUserInfo getAuthenticatedUser(final String accessToken) {
      AuthenticatedUser.AuthUserInfo authUserInfo = authenticatedUser.getAuthUserInfo();
      if (authUserInfo.user == null && authUserCache.containsKey(accessToken)) {
         authUserInfo = authUserCache.get(accessToken);
         authenticatedUser.setAuthUserInfo(authUserInfo);
      }
      return authUserInfo;
   }

   private void parseViewId(final HttpServletRequest req) {
      final String viewId = req.getHeader(VIEW_ID);

      // there is no view, by setting it to an empty string, we lock any further view id changes
      permissionsChecker.setViewId(Objects.requireNonNullElse(viewId, ""));
   }

   private void processStartTimestamp(final HttpServletRequest req, final HttpServletResponse res) {
      String tm = req.getHeader(TIMESTAMP_HEADER);
      if (tm != null) {
         res.addHeader(TIMESTAMP_HEADER, tm);
      }
   }

   private void processUserLocale(final HttpServletRequest req, final HttpServletResponse res) {
      String locale = req.getHeader(LOCALE_HEADER);
      if (locale != null) {
         requestDataKeeper.setUserLocale(locale);
      }
   }

   private void parseRequestData(final HttpServletRequest req) {
      final String correlationId = req.getHeader(CORRELATION_ID);

      this.requestDataKeeper.setCorrelationId(Objects.requireNonNullElse(correlationId, ""));
   }

   @Override
   public void destroy() {

   }

   private String getAccessToken(final HttpServletRequest request) {
      final String bearer = request.getHeader("Authorization");
      if (bearer != null) {
         final String accessToken = bearer.substring(bearer.indexOf("Bearer") + 7).trim();
         SessionUtils.set(request, "accessToken", accessToken);
         return accessToken;
      }

      return null;
   }

   private void fakeUserLogin(final HttpServletRequest request) {
      final String userId = request.getHeader("Test-User");
      if (userId != null && !"".equals(userId)) {
         final AuthenticatedUser.AuthUserInfo authUserInfo = getAuthenticatedUser(userId);

         if (authUserInfo.user == null) {
            final AuthenticatedUser.AuthUserInfo newAuthUserInfo = new AuthenticatedUser.AuthUserInfo();
            newAuthUserInfo.user = new User(userId, userId, userId, Collections.emptyMap());
            newAuthUserInfo.user.setAuthIds(Set.of("TEST:" + userId));
            newAuthUserInfo.accessToken = userId;
            newAuthUserInfo.lastUpdated = System.currentTimeMillis();
            authUserCache.put(userId, newAuthUserInfo);
            authenticatedUser.setAuthUserInfo(newAuthUserInfo);
            authenticatedUser.checkUser();
         }
      }
   }

   private User getUserInfo(final String accessToken) throws Auth0Exception {
      final AuthAPI auth0 = new AuthAPI(domain, clientId, clientSecret);
      final Request<UserInfo> info = auth0.userInfo(accessToken);
      final Map<String, Object> values = info.execute().getValues();
      final String nickname = (String) values.get("nickname");
      final String sub = (String) values.get("sub");
      final String name = (String) values.get("name");
      final String email = (String) values.get("email");
      final User user = new User(email == null ? (sub.startsWith("google-oauth2") ? nickname + "@gmail.com" : name) : email);
      user.setAuthIds(new HashSet<>(Arrays.asList(sub)));
      user.setName(name);

      return user;
   }

   private void cleanCache() {
      if (lastCheck.get() + 60_000 < System.currentTimeMillis()) {
         lastCheck.set(System.currentTimeMillis());

         for (final String accessToken : authUserCache.keySet()) {
            final DecodedJWT jwt;
            try {
               jwt = JWT.decode(accessToken);

               if (Instant.now().isAfter(jwt.getExpiresAt().toInstant())) {
                  removeFromCache(accessToken);
               }
            } catch (Exception e) {
               removeFromCache(accessToken);
            }
         }
      }
   }

   private void removeFromCache(final String accessToken) {
      authUserCache.remove(accessToken);
      semaphores.remove(accessToken);
   }

   private void addCorsHeaders(HttpServletRequest req, HttpServletResponse res) {
      if (configurationFacade.getEnvironment() == ConfigurationFacade.DeployEnvironment.DEVEL) {
         res.addHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
         res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH");
         res.addHeader("Access-Control-Allow-Credentials", "true");
         res.addHeader("Access-Control-Expose-Headers", TIMESTAMP_HEADER);
         String reqHeader = req.getHeader("Access-Control-Request-Headers");
         if (reqHeader != null && !reqHeader.isEmpty()) {
            res.addHeader("Access-Control-Allow-Headers", reqHeader);
         }
      }
   }

}
