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

import io.lumeer.api.model.Organization;
import io.lumeer.storage.mongodb.model.common.MorphiaResource;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@Entity(value = MorphiaOrganization.COLLECTION_NAME, noClassnameStored = true)
@Indexes({
      @Index(fields = { @Field(value = MorphiaOrganization.CODE) }, options = @IndexOptions(unique = true))
})
public class MorphiaOrganization extends MorphiaResource implements Organization {

   public static final String COLLECTION_NAME = "organizations";

   public MorphiaOrganization() {
   }

   public MorphiaOrganization(final Organization organization) {
      super(organization);
   }

   @Override
   public String toString() {
      return "MorphiaOrganization{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            '}';
   }
}
