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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.util.AttributeUtil;
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
import java.util.Optional;
import java.util.Set;

@Entity(noClassnameStored = true)
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
   public static final String LAST_ATTRIBUTE_NUM = "lastAttributeNum";
   public static final String DEFAULT_ATTRIBUTE_ID = "defaultAttributeId";

   @Embedded(ATTRIBUTES)
   private Set<MorphiaAttribute> attributes;

   @Property(DOCUMENTS_COUNT)
   private Integer documentsCount;

   @Property(LAST_TIME_USED)
   private LocalDateTime lastTimeUsed;

   @Property(LAST_ATTRIBUTE_NUM)
   private Integer lastAttributeNum;

   @Property(DEFAULT_ATTRIBUTE_ID)
   private String defaultAttributeId;

   public MorphiaCollection() {
   }

   public MorphiaCollection(Collection collection) {
      super(collection);

      this.attributes = MorphiaAttribute.convert(collection.getAttributes());
      this.documentsCount = collection.getDocumentsCount();
      this.lastTimeUsed = collection.getLastTimeUsed();
      this.lastAttributeNum = collection.getLastAttributeNum();
   }

   @Override
   public Set<Attribute> getAttributes() {
      return Collections.unmodifiableSet(attributes);
   }

   public void setAttributes(final Set<Attribute> attributes) {
      this.attributes = MorphiaAttribute.convert(attributes);
   }

   @Override
   public void createAttribute(final Attribute attribute) {
      attributes.add(MorphiaAttribute.convert(attribute));
   }

   @Override
   public void updateAttribute(final String attributeId, final Attribute attribute) {
      Optional<MorphiaAttribute> oldAttribute = attributes.stream().filter(attr -> attribute.getId().equals(attribute.getId())).findFirst();
      attributes.removeIf(a -> a.getId().equals(attributeId));
      attributes.add(MorphiaAttribute.convert(attribute));

      if (oldAttribute.isPresent() && !oldAttribute.get().getName().equals(attribute.getName())) {
         AttributeUtil.renameChildAttributes(attributes, oldAttribute.get().getName(), attribute.getName());
      }
   }

   @Override
   public void deleteAttribute(final String attributeName) {
      attributes.removeIf(attribute -> AttributeUtil.isEqualOrChild(attribute, attributeName));
   }

   @Override
   public Integer getDocumentsCount() {
      return documentsCount;
   }

   public void setDocumentsCount(final Integer documentsCount) {
      this.documentsCount = documentsCount;
   }

   @Override
   public LocalDateTime getLastTimeUsed() {
      return lastTimeUsed;
   }

   @Override
   public void setLastTimeUsed(final LocalDateTime lastTimeUsed) {
      this.lastTimeUsed = lastTimeUsed;
   }

   @Override
   public Integer getLastAttributeNum() {
      return lastAttributeNum;
   }

   @Override
   public void setLastAttributeNum(final Integer lastAttributeNum) {
      this.lastAttributeNum = lastAttributeNum;

   }

   public String getDefaultAttributeId() {
      return this.defaultAttributeId;
   }

   @Override
   public void setDefaultAttributeId(final String attributeId) {
      this.defaultAttributeId = attributeId;
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
