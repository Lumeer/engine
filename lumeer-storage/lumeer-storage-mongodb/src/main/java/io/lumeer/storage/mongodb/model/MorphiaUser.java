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
