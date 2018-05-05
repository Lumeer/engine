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

import io.lumeer.api.view.UserViews;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class User {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String EMAIL = "email";
   public static final String GROUPS = "groups";

   @JsonView(UserViews.DefaultView.class)
   private String id;

   @JsonView(UserViews.DefaultView.class)
   private String name;

   @JsonView(UserViews.DefaultView.class)
   private String email;

   @JsonIgnore
   private String keycloakId;

   @JsonView(UserViews.DefaultView.class)
   private Map<String, Set<String>> groups;

   @JsonView(UserViews.FullView.class)
   private DefaultWorkspace defaultWorkspace;

   @JsonIgnore
   private Map<String, Set<String>> favoriteCollections;

   @JsonIgnore
   private Map<String, Set<String>> favoriteDocuments;

   @JsonView(UserViews.FullView.class)
   private ContentSize contentSize;

   public User(final String email) {
      this.email = email;
      this.groups = new HashMap<>();
      this.favoriteCollections = new HashMap<>();
      this.favoriteDocuments = new HashMap<>();
   }

   @JsonCreator
   public User(@JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(EMAIL) final String email,
         @JsonProperty(GROUPS) final Map<String, Set<String>> groups) {
      this.id = id;
      this.name = name;
      this.email = email;
      this.groups = groups;
      this.favoriteCollections = new HashMap<>();
      this.favoriteDocuments = new HashMap<>();
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

   public String getKeycloakId() {
      return keycloakId;
   }

   public void setKeycloakId(final String keycloakId) {
      this.keycloakId = keycloakId;
   }

   public Map<String, Set<String>> getGroups() {
      return groups;
   }

   public void setGroups(final Map<String, Set<String>> groups) {
      this.groups = groups;
   }

   public DefaultWorkspace getDefaultWorkspace() {
      return defaultWorkspace;
   }

   public void setDefaultWorkspace(final DefaultWorkspace defaultWorkspace) {
      this.defaultWorkspace = defaultWorkspace;
   }

   public Map<String, Set<String>> getFavoriteCollections() {
      return favoriteCollections;
   }

   public void setFavoriteCollections(final Map<String, Set<String>> favoriteCollections) {
      this.favoriteCollections = favoriteCollections;
   }

   public Map<String, Set<String>> getFavoriteDocuments() {
      return favoriteDocuments;
   }

   public void setFavoriteDocuments(final Map<String, Set<String>> favoriteDocuments) {
      this.favoriteDocuments = favoriteDocuments;
   }

   public ContentSize getContentSize() {
      return contentSize;
   }

   public void setContentSize(final ContentSize contentSize) {
      this.contentSize = contentSize;
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
