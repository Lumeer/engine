package io.lumeer.core.auth;/*
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

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.jobs.Job;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.net.AuthRequest;

import java.io.IOException;
import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SessionScoped
public class ResendVerificationEmailFilter implements AuthFilter, Serializable {

   @Inject
   private AuthenticatedUser authenticatedUser;

   private String domain;
   private String backendClientId;
   private String backendClientSecret;

   private JWTVerifier verifier = null;
   private String managementApiToken;

   @Override
   public void init(final FilterConfig filterConfig) throws ServletException {
      if (System.getenv("SKIP_SECURITY") == null) {
         domain = filterConfig.getServletContext().getInitParameter("com.auth0.domain");
         backendClientId = filterConfig.getServletContext().getInitParameter("com.auth0.backend.clientId");
         backendClientSecret = filterConfig.getServletContext().getInitParameter("com.auth0.backend.clientSecret");
         verifier = AuthenticationControllerProvider.getVerifier(domain);
      }
   }

   @Override
   public FilterResult doFilter(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ServletException {
      if (req.getPathInfo() != null && req.getPathInfo().startsWith("/users/current/resend")) {
         if (authenticatedUser != null && authenticatedUser.getAuthUserInfo() != null) {
            if (authenticatedUser.getAuthUserInfo().user != null &&
               !authenticatedUser.getAuthUserInfo().user.isEmailVerified() && authenticatedUser.getAuthUserInfo().user.getAuthIds() != null &&
               authenticatedUser.getAuthUserInfo().user.getAuthIds().size() > 0) {
               final String authId = authenticatedUser.getAuthUserInfo().user.getAuthIds().iterator().next();

               try {
                  resendVerificationEmail(authId);
               } catch (Auth0Exception ae) {
                  res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ae.getLocalizedMessage());
                  return FilterResult.BREAK;
               }

               res.setStatus(200);
               return FilterResult.BREAK;
            }

            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return FilterResult.BREAK;
         }
      }

      return FilterResult.CONTINUE;
   }

   private void resendVerificationEmail(final String authId) throws Auth0Exception {
      refreshManagementApiToken();
      final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
      Job job = mApi.jobs().sendVerificationEmail(authId, backendClientId).execute();
   }

   private void refreshManagementApiToken() throws Auth0Exception {
      if (managementApiToken == null) {
         managementApiToken = requestManagementApiToken();
      } else {
         if (!isValidManagementApiToken()) {
            managementApiToken = requestManagementApiToken();

            if (!isValidManagementApiToken()) {
               throw new Auth0Exception("Unable to get management API token.");
            }
         }
      }
   }

   private String requestManagementApiToken() throws Auth0Exception {
      final AuthAPI auth0 = new AuthAPI(domain, backendClientId, backendClientSecret);
      AuthRequest req = auth0.requestToken("https://" + domain + "/api/v2/");
      return req.execute().getAccessToken();
   }

   private boolean isValidManagementApiToken() {
      final DecodedJWT jwt;
      try {
         jwt = JWT.decode(managementApiToken);
         verifier.verify(jwt.getToken());
      } catch (Exception e) {
         return false;
      }

      return true;
   }

}
