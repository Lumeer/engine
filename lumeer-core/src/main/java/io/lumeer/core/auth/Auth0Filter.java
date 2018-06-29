/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
import java.util.Map;
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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@WebFilter(urlPatterns = "/*")
public class Auth0Filter implements Filter {

   private static final long TOKEN_REFRESH_PERIOD = 10L * 60 * 1000; // 10 minutes

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private ConfigurationFacade configurationFacade;

   private JWTVerifier verifier = null;
   private String domain;
   private String clientId;
   private String clientSecret;

   @Override
   public void init(final FilterConfig filterConfig) throws ServletException {
      domain = filterConfig.getServletContext().getInitParameter("com.auth0.domain");
      clientId = filterConfig.getServletContext().getInitParameter("com.auth0.clientId");
      clientSecret = filterConfig.getServletContext().getInitParameter("com.auth0.clientSecret");
      verifier = AuthenticationControllerProvider.getVerifier(domain);
   }

   @Override
   public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
      final HttpServletRequest req = (HttpServletRequest) servletRequest;
      final HttpServletResponse res = (HttpServletResponse) servletResponse;
      final String accessToken = getAccessToken(req);

      if (!req.getServletPath().startsWith("/rest/paymentNotify")) {
         // we do not have the token at all, or we failed to obtain verifier
         if (accessToken == null || verifier == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
         }

         // we failed to verify the token
         final DecodedJWT jwt = JWT.decode(accessToken);
         try {
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

         // we are safe to go, make sure we have use info
         final AuthenticatedUser.AuthUserInfo authUserInfo = authenticatedUser.getAuthUserInfo();
         if (!accessToken.equals(authUserInfo.accessToken) || authUserInfo.user == null || authUserInfo.lastUpdated + TOKEN_REFRESH_PERIOD <= System.currentTimeMillis()) {
            if (authenticatedUser.getSemaphore().tryAcquire()) { // only one thread must do that at the same time
               try {
                  final AuthenticatedUser.AuthUserInfo newAuthUserInfo = new AuthenticatedUser.AuthUserInfo();
                  try {
                     newAuthUserInfo.user = getUserInfo(accessToken);
                  } catch (Auth0Exception a0e) {
                     res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                     authenticatedUser.getSemaphore().release();
                     return;
                  }
                  newAuthUserInfo.accessToken = accessToken;
                  newAuthUserInfo.lastUpdated = System.currentTimeMillis();
                  authenticatedUser.setAuthUserInfo(newAuthUserInfo);
                  authenticatedUser.checkUser();
               } finally {
                  authenticatedUser.getSemaphore().release();
               }
            } else {
               try {
                  authenticatedUser.getSemaphore().acquire();
                  authenticatedUser.getSemaphore().release();
               } catch (InterruptedException ie) {
                  // do nothing
               }
            }
         }

      }

      filterChain.doFilter(servletRequest, servletResponse);
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

   private User getUserInfo(final String accessToken) throws Auth0Exception {
      final AuthAPI auth0 = new AuthAPI(domain, clientId, clientSecret);
      final Request<UserInfo> info = auth0.userInfo(accessToken);
      final Map<String, Object> values = info.execute().getValues();
      final String nickname = (String) values.get("nickname");
      final String sub = (String) values.get("sub");
      final String name = (String) values.get("name");
      final User user = new User(sub.startsWith("google-oauth2") ? nickname + "@gmail.com" : name);
      user.setAuthId(sub);
      user.setName(name);

      return user;
   }

}
