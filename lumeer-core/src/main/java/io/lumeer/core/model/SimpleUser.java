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
package io.lumeer.core.model;

import io.lumeer.api.model.User;

import java.util.Collections;
import java.util.Set;

public class SimpleUser implements User {

   private final String id;
   private final String username;
   private final Set<String> groups;

   public SimpleUser(final String username) {
      this.id = null;
      this.username = username;
      this.groups = Collections.emptySet();
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public Set<String> getGroups() {
      return groups;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof User)) {
         return false;
      }

      final User that = (User) o;

      return getUsername() != null ? getUsername().equals(that.getUsername()) : that.getUsername() == null;
   }

   @Override
   public int hashCode() {
      return getUsername() != null ? getUsername().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "SimpleUser{" +
            "id='" + id + '\'' +
            ", username='" + username + '\'' +
            ", groups=" + groups +
            '}';
   }
}
