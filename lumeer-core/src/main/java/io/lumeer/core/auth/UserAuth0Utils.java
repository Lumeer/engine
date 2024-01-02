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

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.jobs.Job;
import com.auth0.json.mgmt.users.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.net.AuthRequest;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

@SessionScoped
public class UserAuth0Utils implements Serializable {

   @Inject
   private AuthenticatedUser authenticatedUser;

   private String redirectUri;
   private String domain;
   private String clientId;
   private String clientSecret;
   private String backendClientId;
   private String backendClientSecret;
   private String audience;

   private JWTVerifier verifier = null;
   private String managementApiToken;

   private boolean initialized = false;
   private boolean skipSecurity = false;

   public void init() {
      skipSecurity = System.getenv("SKIP_SECURITY") != null;
      if (!initialized && StringUtils.isNotEmpty(domain)) {
         verifier = AuthenticationControllerProvider.getVerifier(domain);
         initialized = true;
      }
   }

   public void resendVerificationEmail(final String authId) throws Auth0Exception {
      if (!initialized) {
         init();
      }
      if (!skipSecurity) {
         refreshManagementApiToken();
         final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
         Job job = mApi.jobs().sendVerificationEmail(authId, backendClientId).execute();
      }
   }

   public void renameUser(final String newUserName) throws Auth0Exception {
      if (!initialized) {
         init();
      }
      final String authId = authenticatedUser.getAuthUserInfo().user.getAuthIds().iterator().next();
      refreshManagementApiToken();
      final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
      final User user = new User();
      user.setName(newUserName);
      mApi.users().update(authId, user).execute();
   }

   public void setEmailVerified(final io.lumeer.api.model.User lumeerUser) throws Auth0Exception {
      if (!initialized) {
         init();
      }
      final String authId = lumeerUser.getAuthIds().iterator().next();
      refreshManagementApiToken();
      final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
      final User user = new User();
      user.setEmailVerified(true);
      mApi.users().update(authId, user).execute();
   }

   public void deleteUser(final io.lumeer.api.model.User lumeerUser) throws Auth0Exception {
      if (!initialized) {
         init();
      }
      final String authId = lumeerUser.getAuthIds().iterator().next();
      refreshManagementApiToken();
      final ManagementAPI mApi = new ManagementAPI(domain, managementApiToken);
      mApi.users().delete(authId).execute();
   }

   public TokenHolder userLogin(final String user, final String password, final String audience) throws Auth0Exception {
      final AuthAPI auth0 = new AuthAPI(domain, backendClientId, backendClientSecret);
      return auth0.login(user, password).setScope("openid email profile").setAudience(audience).execute();
   }

   public TokenHolder refreshToken(final String refreshToken) throws Auth0Exception {
      final AuthAPI auth0 = new AuthAPI(domain, clientId, clientSecret);
      return auth0.renewAuth(refreshToken).execute();
   }

   public TokenHolder exchangeCode(final String authorizationCode) throws Auth0Exception {
      final AuthAPI auth0 = new AuthAPI(domain, clientId, clientSecret);
      final String notNullRedirectUri = StringUtils.isNotEmpty(redirectUri) ? redirectUri : "http://localhost:7000/auth";
      return auth0.exchangeCode(authorizationCode, notNullRedirectUri).execute();
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

   public void setDomain(final String domain) {
      this.domain = domain;
   }

   public void setClientId(final String clientId) {
      this.clientId = clientId;
   }

   public void setClientSecret(final String clientSecret) {
      this.clientSecret = clientSecret;
   }

   public void setBackendClientId(final String backendClientId) {
      this.backendClientId = backendClientId;
   }

   public void setBackendClientSecret(final String backendClientSecret) {
      this.backendClientSecret = backendClientSecret;
   }

   public void setAudience(String audience) {
      this.audience = audience;
   }

   public void setRedirectUri(final String redirectUri) { this.redirectUri = redirectUri; }
}
