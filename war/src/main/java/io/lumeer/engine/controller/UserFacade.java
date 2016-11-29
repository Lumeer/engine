/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.controller;

import org.keycloak.KeycloakPrincipal;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Holds information about currently logged in user.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class UserFacade implements Serializable {

   @Inject
   private HttpServletRequest request;

   /**
    * Gets the name of currently logged in user.
    *
    * @return The name of currently logged in user.
    */
   public String getUserName() {
      final Optional<KeycloakPrincipal> principal = getPrincipal();
      return principal.isPresent() ? principal.get().getKeycloakSecurityContext().getToken().getName() : "Alan Mathison Turing";
   }

   /**
    * Gets the email of currently logged in user.
    *
    * @return The email of currently logged in user.
    */
   public String getUserEmail() {
      final Optional<KeycloakPrincipal> principal = getPrincipal();
      return principal.isPresent() ? principal.get().getKeycloakSecurityContext().getToken().getEmail() : "aturing@lumeer.io";
   }

   /**
    * Get the user roles of currently logged in user.
    *
    * @return The user roles of currently logged in user.
    */
   public Set<String> getUserRoles() {
      final Optional<KeycloakPrincipal> principal = getPrincipal();
      return principal.isPresent() ? principal.get().getKeycloakSecurityContext().getToken().getRealmAccess().getRoles() : Collections.singleton("scientist");
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
