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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.storage.mongodb.model.common.MorphiaResource;
import io.lumeer.storage.mongodb.model.embedded.MorphiaAttribute;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.utils.IndexType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

@Entity
@Indexes({
      @Index(fields = { @Field(MorphiaCollection.CODE) }, options = @IndexOptions(unique = true)),
      @Index(fields = { @Field(MorphiaCollection.NAME) }, options = @IndexOptions(unique = true)),
      @Index(fields = { @Field(MorphiaCollection.ATTRIBUTES + "." + MorphiaAttribute.NAME) }),
      @Index(fields = {
            @Field(value = MorphiaCollection.CODE, type = IndexType.TEXT),
            @Field(value = MorphiaCollection.NAME, type = IndexType.TEXT),
            @Field(value = MorphiaCollection.ATTRIBUTES + "." + MorphiaAttribute.NAME, type = IndexType.TEXT)
      })
})
public class MorphiaCollection extends MorphiaResource implements Collection {

   public static final String ATTRIBUTES = "attributes";
   public static final String DOCUMENTS_COUNT = "docCount";
   public static final String LAST_TIME_USED = "lastTimeUsed";

   @Embedded(ATTRIBUTES)
   private Set<MorphiaAttribute> attributes;

   @Property(DOCUMENTS_COUNT)
   private Integer documentsCount;

   @Property(LAST_TIME_USED)
   private LocalDateTime lastTimeUsed;

   public MorphiaCollection() {
   }

   public MorphiaCollection(Collection collection) {
      super(collection);

      this.attributes = MorphiaAttribute.convert(collection.getAttributes());
      this.documentsCount = collection.getDocumentsCount();
      this.lastTimeUsed = collection.getLastTimeUsed();
   }

   @Override
   public Set<Attribute> getAttributes() {
      return Collections.unmodifiableSet(attributes);
   }

   @Override
   public void updateAttribute(final String attributeFullName, final Attribute attribute) {
      deleteAttribute(attributeFullName);
      attributes.add(MorphiaAttribute.convert(attribute));
   }

   @Override
   public void deleteAttribute(final String attributeFullName) {
      attributes.removeIf(a -> a.getFullName().equals(attributeFullName));
   }

   @Override
   public Integer getDocumentsCount() {
      return documentsCount;
   }

   @Override
   public LocalDateTime getLastTimeUsed() {
      return lastTimeUsed;
   }

   public void setAttributes(final Set<Attribute> attributes) {
      this.attributes = MorphiaAttribute.convert(attributes);
   }

   public void setDocumentsCount(final Integer documentsCount) {
      this.documentsCount = documentsCount;
   }

   public void setLastTimeUsed(final LocalDateTime lastTimeUsed) {
      this.lastTimeUsed = lastTimeUsed;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Collection)) {
         return false;
      }

      final Collection that = (Collection) o;

      return getCode() != null ? getCode().equals(that.getCode()) : that.getCode() == null;
   }

   @Override
   public int hashCode() {
      return getCode() != null ? getCode().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "MongoCollection{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", attributes=" + attributes +
            ", documentsCount=" + documentsCount +
            ", lastTimeUsed=" + lastTimeUsed +
            '}';
   }
}
