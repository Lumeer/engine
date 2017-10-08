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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Role {
   private final String name;
   private final List<String> users;
   private final List<String> groups;

   public Role(final String name, final DataDocument data) {
      this(name, data.getArrayList(LumeerConst.Security.USERS_KEY, String.class), data.getArrayList(LumeerConst.Security.GROUP_KEY, String.class));
   }

   @JsonCreator
   public Role(final @JsonProperty("name") String name,
         final @JsonProperty("users") List<String> users,
         final @JsonProperty("groups") List<String> groups) {
      this.name = name;
      this.users = users;
      this.groups = groups;
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
