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

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Holds information about currently logged in user.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class UserFacade implements Serializable {

   @Inject
   private Principal principal;
   // KeycloakPrincipal<KeycloakSecurityContext> kcPrincipal = (KeycloakPrincipal<KeycloakSecurityContext>)(sessionContext.getCallerPrincipal());

   /**
    * Gets the name of currently logged in user.
    *
    * @return The name of currently logged in user.
    */
   public String getUserName() {
      return "Pepa Žblotký";
   }

   /**
    * Gets the email of currently logged in user.
    *
    * @return The email of currently logged in user.
    */
   public String getUserEmail() {
      return "pepa0@zdepa.cz";
   }

   /**
    * Get the user roles of currently logged in user.
    *
    * @return The user roles of currently logged in user.
    */
   public List<String> getUserRoles() {
      return Collections.singletonList("user");
   }
}
