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

import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.AuthenticationControllerProvider;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.jobs.Job;
import com.auth0.json.mgmt.users.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.net.AuthRequest;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

@SessionScoped
public class UserAuth0Utils implements Serializable {

   @Inject
   private AuthenticatedUser authenticatedUser;

   private String domain;
   private String backendClientId;
   private String backendClientSecret;

   private JWTVerifier verifier = null;
   private String managementApiToken;

   private boolean initialized = false;

   @PostConstruct
   public void init(final FilterConfig filterConfig) throws ServletException {
      if (!initialized && System.getenv("SKIP_SECURITY") == null) {
         domain = filterConfig.getServletContext().getInitParameter("com.auth0.domain");
         backendClientId = filterConfig.getServletContext().getInitParameter("com.auth0.backend.clientId");
         backendClientSecret = filterConfig.getServletContext().getInitParameter("com.auth0.backend.clientSecret");
         verifier = AuthenticationControllerProvider.getVerifier(domain);
         initialized = true;
      }
   }

   public void resendVerificationEmail(final String authId) throws Auth0Exception {
      refreshManagementApiToken();
      final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
      Job job = mApi.jobs().sendVerificationEmail(authId, backendClientId).execute();
   }

   public void renameUser(final String newUserName) throws Auth0Exception {
      final String authId = authenticatedUser.getAuthUserInfo().user.getAuthIds().iterator().next();
      refreshManagementApiToken();
      final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
      final User user = mApi.users().get(authId, null).execute();
      user.setName(newUserName);
      mApi.users().update(authId, user).execute();
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
