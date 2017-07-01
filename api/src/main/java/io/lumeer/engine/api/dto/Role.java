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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Role {
   private final String name;
   private final List<String> users;
   private final List<String> groups;

   public Role(final String name, final DataDocument data) {
      this.name = name;
      this.users = data.getArrayList(LumeerConst.Security.USERS_KEY, String.class);
      this.groups = data.getArrayList(LumeerConst.Security.GROUP_KEY, String.class);
   }

   public List<String> getUsers() {
      return Collections.unmodifiableList(users);
   }

   public List<String> getGroups() {
      return Collections.unmodifiableList(groups);
   }

   public String getName() {
      return name;
   }

   public boolean equals(final Role r) {
      return r.getName().equals(this.getName())
            && r.getGroups().containsAll(this.getGroups())
            && r.getUsers().containsAll(this.getUsers());
   }
}
