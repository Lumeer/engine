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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Group {

   public static final String ID = "id";
   public static final String NAME = "name";

   private String id;
   private String name;

   public Group(String name){
      this.name = name;
   }

   @JsonCreator
   public Group(@JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name) {
      this.id = id;
      this.name = name;
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

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Group)) {
         return false;
      }
      final Group group = (Group) o;
      return Objects.equals(getId(), group.getId()) &&
            Objects.equals(getName(), group.getName());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getId(), getName());
   }

   @Override
   public String toString() {
      return "Group{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            '}';
   }
}
