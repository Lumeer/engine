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
package io.lumeer.core;

import io.lumeer.api.model.User;
import io.lumeer.core.cache.UserCache;

import org.keycloak.KeycloakPrincipal;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@RequestScoped
public class AuthenticatedUser {

   public static final String DEFAULT_USERNAME = "aturing";
   public static final String DEFAULT_EMAIL = "aturing@lumeer.io";

   @Inject
   private HttpServletRequest request;

   @Inject
   private UserCache userCache;

   public User getCurrentUser() {
      String username = getUserEmail();
      return userCache.getUser(username);
   }

   public String getCurrentUsername() {
      return getCurrentUser().getUsername();
   }

   /**
    * Gets the name of currently logged in user.
    *
    * @return The name of currently logged in user.
    */
   private String getUserName() {
      final Optional<KeycloakPrincipal> principal = getPrincipal();
      return principal.isPresent() ? principal.get().getKeycloakSecurityContext().getToken().getName() : DEFAULT_USERNAME;
   }

   /**
    * Gets the email of currently logged in user.
    *
    * @return The email of currently logged in user.
    */
   public String getUserEmail() {
      final Optional<KeycloakPrincipal> principal = getPrincipal();
      return principal.isPresent() ? principal.get().getKeycloakSecurityContext().getToken().getEmail() : DEFAULT_EMAIL;
   }

   /**
    * Get the user roles of currently logged in user.
    *
    * @return The user roles of currently logged in user.
    */
   private Set<String> getUserRoles() {
      final Optional<KeycloakPrincipal> principal = getPrincipal();
      return principal.isPresent() ? principal.get().getKeycloakSecurityContext().getToken().getRealmAccess().getRoles() : Collections.emptySet();
   }

   public String getUserSessionId() {
      return request.getSession().getId();
   }

   /**
    * Obtains Keycloak principal is possible.
    *
    * @return Optionally returns the Keycloak principal if it was available.
    */
   private Optional<KeycloakPrincipal> getPrincipal() {
      try {
         return Optional.ofNullable((KeycloakPrincipal) request.getUserPrincipal());
      } catch (Throwable t) {
         return Optional.empty();
      }
   }
}
