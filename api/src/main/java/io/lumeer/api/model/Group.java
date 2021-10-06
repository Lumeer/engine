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

import io.lumeer.api.exception.InsaneObjectException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Group implements HealthChecking {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String DESCRIPTION = "description";
   public static final String USERS = "users";

   private String id;
   private String name;
   private String description;
   private String icon;
   private String color;
   private String organizationId;
   private List<String> users;

   public Group(String name){
      this.name = name;
   }

   public Group(String name, List<String> users){
      this.name = name;
      this.users = users;
   }

   public Group(String id, String name){
      this.id = id;
      this.name = name;
   }

   @JsonCreator
   public Group(@JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(USERS) final List<String> users) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.icon = icon;
      this.color = color;
      this.users = users;
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

   public String getDescription() {
      return description;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public String getIcon() {
      return icon;
   }

   public void setIcon(final String icon) {
      this.icon = icon;
   }

   public String getColor() {
      return color;
   }

   public void setColor(final String color) {
      this.color = color;
   }

   public List<String> getUsers() {
      return users;
   }

   public void setUsers(final List<String> users) {
      this.users = users;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Group group = (Group) o;
      return Objects.equals(id, group.id) && Objects.equals(name, group.name) && Objects.equals(description, group.description) && Objects.equals(icon, group.icon) && Objects.equals(color, group.color) && Objects.equals(users, group.users);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, name, description, icon, color, users);
   }

   @Override
   public String toString() {
      return "Group{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", users=" + users +
            '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("icon", icon, MAX_STRING_LENGTH);
      checkStringLength("color", color, MAX_STRING_LENGTH);
      checkStringLength("name", name, MAX_STRING_LENGTH);
      checkStringLength("description", description, MAX_LONG_STRING_LENGTH);
   }
}
