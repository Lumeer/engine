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

import io.lumeer.api.model.Organization;
import io.lumeer.storage.mongodb.model.common.MorphiaResource;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@Entity(MorphiaOrganization.COLLECTION_NAME)
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
