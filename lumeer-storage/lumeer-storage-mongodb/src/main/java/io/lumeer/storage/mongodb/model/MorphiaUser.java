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
package io.lumeer.storage.mongodb.model;

import io.lumeer.api.model.User;
import io.lumeer.storage.mongodb.model.common.MorphiaEntity;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity(MorphiaUser.COLLECTION_NAME)
public class MorphiaUser extends MorphiaEntity implements User {

   public static final String COLLECTION_NAME = "users";

   public static final String USERNAME = "username";
   public static final String GROUPS = "groups";

   @Property(USERNAME)
   @Indexed(options = @IndexOptions(unique = true))
   private String username;

   @Property(GROUPS)
   private Set<String> groups = Collections.emptySet();

   public MorphiaUser() {
   }

   public MorphiaUser(final User user) {
      super(user.getId());

      this.username = user.getUsername();
      this.groups = new HashSet<>(user.getGroups());
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public Set<String> getGroups() {
      return Collections.unmodifiableSet(groups);
   }

   public void setUsername(final String username) {
      this.username = username;
   }

   public void setGroups(final Set<String> groups) {
      this.groups = groups;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final MorphiaUser morphiaUser = (MorphiaUser) o;

      return username != null ? username.equals(morphiaUser.username) : morphiaUser.username == null;
   }

   @Override
   public int hashCode() {
      return username != null ? username.hashCode() : 0;
   }
}
