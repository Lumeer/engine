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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Permission {

   private final String name;
   private final Set<String> roles;

   public Permission(DataDocument document) {
      this.name = document.getString(LumeerConst.Security.USERGROUP_NAME_KEY);
      this.roles = new HashSet<>(document.getArrayList(LumeerConst.Security.USERGROUP_ROLES_KEY, String.class));
   }

   @JsonCreator
   public Permission(final @JsonProperty("name") String name,
         final @JsonProperty("users") Set<String> roles) {
      this.name = name;
      this.roles = roles;
   }

   public String getName() {
      return name;
   }

   public Set<String> getRoles() {
      return roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final Permission that = (Permission) o;

      return name.equals(that.name);
   }

}
