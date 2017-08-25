/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.api.model;

import java.util.Set;
import java.util.stream.Collectors;

public enum Role {

   MANAGE,
   WRITE,
   SHARE,
   READ,
   CLONE;

   @Override
   public String toString() {
      return name().toLowerCase();
   }

   public static Role fromString(String role) {
      return Role.valueOf(role.toUpperCase());
   }

   public static Set<String> toStringRoles(Set<Role> roles) {
      return roles.stream()
                  .map(Role::toString)
                  .collect(Collectors.toSet());
   }

   public static Set<Role> fromStringRoles(Set<String> roles) {
      return roles.stream()
                  .map(Role::fromString)
                  .collect(Collectors.toSet());
   }
}
