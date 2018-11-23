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

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.AttributeUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection extends Resource {

   public static Set<Role> ROLES = new HashSet<>(Arrays.asList(Role.MANAGE, Role.WRITE, Role.SHARE, Role.READ));

   public static String ATTRIBUTE_PREFIX = "a";

   private static final String ATTRIBUTES = "attributes";

   private Set<Attribute> attributes;
   private Integer documentsCount;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime lastTimeUsed;
   private String defaultAttributeId;
   private Integer lastAttributeNum;
   private boolean favorite;

   public Collection(final String code, final String name, final String icon, final String color, final Permissions permissions) {
      this(code, name, icon, color, "", permissions, new LinkedHashSet<>());
   }

   @JsonCreator
   public Collection(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(ATTRIBUTES) final Set<Attribute> attributes) {
      super(code, name, icon, color, description, permissions);

      this.attributes = attributes != null ? new LinkedHashSet<>(attributes) : new LinkedHashSet<>();
      this.documentsCount = 0;
      this.lastAttributeNum = 0;
   }

   @Override
   public Collection copy() {
      final Collection o = new Collection(this.code, this.name, this.icon, this.color, new Permissions(this.getPermissions()));

      o.id = this.id;
      o.description = this.description;
      o.nonRemovable = this.nonRemovable;
      o.attributes = new HashSet<>(this.attributes);
      o.documentsCount = this.documentsCount;
      o.lastTimeUsed = this.lastTimeUsed;
      o.defaultAttributeId = this.defaultAttributeId;
      o.lastAttributeNum = this.lastAttributeNum;
      o.favorite = this.favorite;

      return o;
   }


   @Override
   public ResourceType getType() {
      return ResourceType.COLLECTION;
   }

   public Set<Attribute> getAttributes() {
      return Collections.unmodifiableSet(attributes);
   }

   public void setAttributes(final Set<Attribute> attributes) {
      this.attributes = attributes != null ? new LinkedHashSet<>(attributes) : new LinkedHashSet<>();
   }

   public void createAttribute(final Attribute attribute) {
      attributes.add(attribute);
   }

   public void updateAttribute(final String attributeId, final Attribute attribute) {
      Optional<Attribute> oldAttribute = attributes.stream().filter(attr -> attribute.getId().equals(attribute.getId())).findFirst();
      attributes.removeIf(a -> a.getId().equals(attributeId));
      attributes.add(attribute);

      if (oldAttribute.isPresent() && !oldAttribute.get().getName().equals(attribute.getName())) {
         AttributeUtil.renameChildAttributes(attributes, oldAttribute.get().getName(), attribute.getName());
      }
   }

   public void deleteAttribute(final String attributeId) {
      Optional<Attribute> toDelete = attributes.stream().filter(attribute -> attribute.getId().equals(attributeId)).findFirst();
      toDelete.ifPresent(jsonAttribute -> attributes.removeIf(attribute -> AttributeUtil.isEqualOrChild(attribute, jsonAttribute.getName())));
   }

   public Integer getDocumentsCount() {
      return documentsCount;
   }

   public void setDocumentsCount(final Integer documentsCount) {
      this.documentsCount = documentsCount;
   }

   public ZonedDateTime getLastTimeUsed() {
      return lastTimeUsed;
   }

   public void setLastTimeUsed(final ZonedDateTime lastTimeUsed) {
      this.lastTimeUsed = lastTimeUsed;
   }

   public boolean isFavorite() {
      return favorite;
   }

   public void setFavorite(final boolean favorite) {
      this.favorite = favorite;
   }

   @JsonIgnore
   public Integer getLastAttributeNum() {
      return lastAttributeNum;
   }

   public void setLastAttributeNum(final Integer lastAttributeNum) {
      this.lastAttributeNum = lastAttributeNum;
   }

   public String getDefaultAttributeId() {
      return this.defaultAttributeId;
   }

   public void setDefaultAttributeId(final String attributeId) {
      this.defaultAttributeId = attributeId;
   }

   @Override
   public String toString() {
      return "JsonCollection{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", attributes=" + attributes +
            ", documentsCount=" + documentsCount +
            ", defaultAttributeId=" + defaultAttributeId +
            ", lastTimeUsed=" + lastTimeUsed +
            '}';
   }

}
