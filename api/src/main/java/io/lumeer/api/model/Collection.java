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

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.api.adapter.ZonedDateTimeDeserializer;
import io.lumeer.api.adapter.ZonedDateTimeSerializer;
import io.lumeer.api.exception.InsaneObjectException;
import io.lumeer.api.model.common.AttributesResource;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.RoleUtils;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection extends Resource implements AttributesResource, HealthChecking, Updatable<Collection> {

   public static Set<Role> ROLES = RoleUtils.collectionResourceRoles();

   public static final String ATTRIBUTES = "attributes";
   public static final String DATA_DESCRIPTION = "dataDescription";
   public static final String PURPOSE = "purpose";

   public static final String RULES = "rules";

   private Set<Attribute> attributes;
   private Long documentsCount;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   @JsonSerialize(using = ZonedDateTimeSerializer.class)
   @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
   private ZonedDateTime lastTimeUsed;
   private String defaultAttributeId;
   private Integer lastAttributeNum;
   private boolean favorite;
   private Map<String, Rule> rules;
   private String dataDescription;
   private CollectionPurpose purpose;

   public Collection(final String code, final String name, final String icon, final String color, final Permissions permissions) {
      this(code, name, icon, color, "", null, permissions, new LinkedHashSet<>(), new HashMap<>(), "", new CollectionPurpose(CollectionPurposeType.None, null));
   }

   @JsonCreator
   public Collection(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PRIORITY) final Long order,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(ATTRIBUTES) final Set<Attribute> attributes,
         @JsonProperty(RULES) final Map<String, Rule> rules,
         @JsonProperty(DATA_DESCRIPTION) final String dataDescription,
         @JsonProperty(PURPOSE) final CollectionPurpose purpose) {
      super(code, name, icon, color, description, order, permissions);

      this.attributes = attributes != null ? new LinkedHashSet<>(attributes) : new LinkedHashSet<>();
      this.documentsCount = 0L;
      this.lastAttributeNum = 0;
      this.rules = rules;
      this.dataDescription = dataDescription;
      this.purpose = purpose;
   }

   public Collection(final Resource resource,
         final Set<Attribute> attributes,
         final Map<String, Rule> rules,
         final String dataDescription,
         final CollectionPurpose purpose) {
      super(resource);

      this.attributes = attributes != null ? new LinkedHashSet<>(attributes) : new LinkedHashSet<>();
      this.documentsCount = 0L;
      this.lastAttributeNum = 0;
      this.rules = rules;
      this.dataDescription = dataDescription;
      this.purpose = purpose;
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
      o.version = this.version;
      o.rules = this.rules != null ? new HashMap<>(this.rules) : Collections.emptyMap();
      o.dataDescription = this.dataDescription;
      o.priority = this.priority;
      o.purpose = this.purpose != null ? new CollectionPurpose(this.purpose.getType(), new DataDocument(this.purpose.createIfAbsentMetaData())) : null;

      return o;
   }

   @Override
   public ResourceType getType() {
      return ResourceType.COLLECTION;
   }

   public Set<Attribute> getAttributes() {
      return attributes != null ? Collections.unmodifiableSet(attributes) : Collections.emptySet();
   }

   public void setAttributes(final java.util.Collection<Attribute> attributes) {
      this.attributes = attributes != null ? new LinkedHashSet<>(attributes) : new LinkedHashSet<>();
   }

   @Override
   public java.util.Collection<Attribute> getMutableAttributes() {
      return attributes;
   }

   public Long getDocumentsCount() {
      return documentsCount;
   }

   public void setDocumentsCount(final Long documentsCount) {
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

   public Map<String, Rule> getRules() {
      return rules != null ? rules : new HashMap<>();
   }

   public void setRules(final Map<String, Rule> rules) {
      this.rules = rules;
   }

   public String getDataDescription() {
      return dataDescription;
   }

   public void setDataDescription(final String dataDescription) {
      this.dataDescription = dataDescription;
   }

   @JsonIgnore
   public DataDocument getPurposeMetaData() {
      return getPurpose().getMetaData();
   }

   public void setPurposeMetaData(final DataDocument purposeMetaData) {
      this.purpose.setMetaData(purposeMetaData);
   }

   public void setPurpose(final CollectionPurpose purpose) {
      this.purpose = purpose;
   }

   public CollectionPurpose getPurpose() {
      if (this.purpose == null) {
         this.purpose = new CollectionPurpose(CollectionPurposeType.None, new DataDocument());
      }
      return this.purpose;
   }

   @JsonIgnore
   public CollectionPurposeType getPurposeType() {
      return purpose != null ? purpose.getType() : CollectionPurposeType.None;
   }

   @JsonIgnore
   public boolean someFunctionChangedOrAdded(Collection originalCollection) {
      Map<String, Attribute> attributesMap = originalCollection.getAttributes().stream().collect(Collectors.toMap(Attribute::getId, attribute -> attribute));
      return getAttributes().stream().anyMatch(attribute -> attribute.functionChangedOrAdded(attributesMap.get(attribute.getId())));
   }

   @JsonIgnore
   public boolean someRuleChangedOrAdded(Collection originalCollection) {
      return getRules().entrySet().stream().anyMatch(entry -> !entry.getValue().equals(originalCollection.getRules().get(entry.getKey())));
   }

   @Override
   public String toString() {
      return "Collection{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", order='" + priority + '\'' +
            ", permissions=" + permissions +
            ", attributes=" + attributes +
            ", documentsCount=" + documentsCount +
            ", defaultAttributeId=" + defaultAttributeId +
            ", lastTimeUsed=" + lastTimeUsed +
            ", rules=" + rules +
            ", dataDescription=" + dataDescription +
            ", purpose=" + purpose +
            '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      super.checkHealth();

      if (attributes != null) {
         attributes.forEach(Attribute::checkHealth);
      }
      if (rules != null) {
         rules.forEach((k, v) -> v.checkHealth());
      }
   }

   @Override
   public void patch(final Collection resource, final Set<RoleType> roles) {
      patchResource(this, resource, roles);

      if (roles.contains(RoleType.Manage)) {
         setDataDescription(resource.getDataDescription());
      }
      if (roles.contains(RoleType.AttributeEdit)) {
         setDefaultAttributeId(resource.getDefaultAttributeId());
      }
      if (roles.contains(RoleType.TechConfig)) {
         setPurpose(resource.getPurpose());
         patchRules(resource.getRules());
      }

      patchAttributes(resource.getAttributes(), roles);
   }
}
