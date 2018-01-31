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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;

public class User {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String EMAIL = "email";
   public static final String GROUPS = "groups";

   private String id;
   private String name;
   private String email;
   private Set<String> groups;

   public User(final String email) {
      this.email = email;
   }

   @JsonCreator
   public User(@JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(EMAIL) final String email,
         @JsonProperty(GROUPS) final Set<String> groups) {
      this.id = id;
      this.name = name;
      this.email = email;
      this.groups = groups;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(final String email) {
      this.email = email;
   }

   public Set<String> getGroups() {
      return groups;
   }

   public void setGroups(final Set<String> groups) {
      this.groups = groups;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof User)) {
         return false;
      }
      final User user = (User) o;
      return Objects.equals(getId(), user.getId()) &&
            Objects.equals(getName(), user.getName()) &&
            Objects.equals(getEmail(), user.getEmail()) &&
            Objects.equals(getGroups(), user.getGroups());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getId(), getName(), getEmail(), getGroups());
   }

   @Override
   public String toString() {
      return "User{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", email='" + email + '\'' +
            ", groups=" + groups +
            '}';
   }
}
