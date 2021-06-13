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
import io.lumeer.api.model.common.WithId;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.api.util.RoleUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkType implements WithId, HealthChecking {

   public static Set<Role> ROLES = RoleUtils.linkTypeResourceRoles();

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String COLLECTION_IDS = "collectionIds";
   public static final String ATTRIBUTES = "attributes";
   public static final String RULES = "rules";
   public static final String PERMISSIONS_TYPE = "permissionsType";
   public static final String PERMISSIONS = "permissions";

   public static final String ATTRIBUTE_PREFIX = "a";

   private String id;
   private String name;
   private long version;
   private List<String> collectionIds;
   private List<Attribute> attributes;
   private Integer lastAttributeNum;
   private Long linksCount;
   private Map<String, Rule> rules;
   private LinkPermissionsType permissionsType;
   private Permissions permissions;

   @JsonCreator
   public LinkType(@JsonProperty(NAME) final String name,
         @JsonProperty(COLLECTION_IDS) final List<String> collectionIds,
         @JsonProperty(ATTRIBUTES) final List<Attribute> attributes,
         @JsonProperty(RULES) final Map<String, Rule> rules,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(PERMISSIONS_TYPE) final LinkPermissionsType permissionsType) {
      this.name = name;
      this.collectionIds = collectionIds;
      this.attributes = attributes;
      this.rules = rules;
      this.permissions = permissions;
      this.permissionsType = permissionsType;
   }

   public LinkType(LinkType linkType) {
      this.id = linkType.getId();
      this.name = linkType.getName();
      this.collectionIds = linkType.getCollectionIds() != null ? new ArrayList<>(linkType.getCollectionIds()) : Collections.emptyList();
      this.version = linkType.getVersion();
      this.attributes = linkType.getAttributes() != null ? new ArrayList<>(linkType.getAttributes()) : Collections.emptyList();
      this.lastAttributeNum = linkType.getLastAttributeNum();
      this.linksCount = linkType.getLinksCount();
      this.rules = linkType.rules != null ? linkType.rules.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new Rule(e.getValue()))) : null;
      this.permissions = linkType.getPermissions() != null ? new Permissions(linkType.getPermissions()) : new Permissions();
      this.permissionsType = linkType.getPermissionsType();
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getCollectionIds() {
      return Collections.unmodifiableList(collectionIds);
   }

   @JsonIgnore
   public String getFirstCollectionId() {
      return collectionIds != null ? collectionIds.get(0) : null;
   }

   @JsonIgnore
   public String getSecondCollectionId() {
      return collectionIds != null ? collectionIds.get(1) : null;
   }

   public void setCollectionIds(List<String> collectionIds) {
      this.collectionIds = collectionIds;
   }

   public Permissions getPermissions() {
      return permissions;
   }

   public void setPermissions(final Permissions permissions) {
      this.permissions = permissions;
   }

   public List<Attribute> getAttributes() {
      return attributes != null ? Collections.unmodifiableList(attributes) : Collections.emptyList();
   }

   public void setAttributes(final java.util.Collection<Attribute> attributes) {
      this.attributes = attributes != null ? new LinkedList<>(attributes) : new LinkedList<>();
   }

   public void createAttribute(final Attribute attribute) {
      if (attributes != null) {
         attributes.add(attribute);
      } else {
         attributes = new ArrayList<>(Collections.singletonList(attribute));
      }
   }

   public void updateAttribute(final String attributeId, final Attribute attribute) {
      Optional<Attribute> oldAttribute = attributes.stream().filter(attr -> attr.getId().equals(attributeId)).findFirst();
      attributes.removeIf(a -> a.getId().equals(attributeId));

      oldAttribute.ifPresent((a) -> attribute.setUsageCount(a.getUsageCount()));
      attributes.add(attribute);

      if (oldAttribute.isPresent() && !oldAttribute.get().getName().equals(attribute.getName())) {
         AttributeUtil.renameChildAttributes(attributes, oldAttribute.get().getName(), attribute.getName());
      }
   }

   public void deleteAttribute(final String attributeId) {
      Optional<Attribute> toDelete = attributes.stream().filter(attribute -> attribute.getId().equals(attributeId)).findFirst();
      toDelete.ifPresent(jsonAttribute -> attributes.removeIf(attribute -> AttributeUtil.isEqualOrChild(attribute, jsonAttribute.getName())));
   }

   public long getVersion() {
      return version;
   }

   public void setVersion(final long version) {
      this.version = version;
   }

   public Integer getLastAttributeNum() {
      return lastAttributeNum;
   }

   public void setLastAttributeNum(final Integer lastAttributeNum) {
      this.lastAttributeNum = lastAttributeNum;
   }

   public Long getLinksCount() {
      return linksCount;
   }

   public void setLinksCount(final Long linksCount) {
      this.linksCount = linksCount;
   }

   public Map<String, Rule> getRules() {
      return rules != null ? rules : new HashMap<>();
   }

   public void setRules(final Map<String, Rule> rules) {
      this.rules = rules;
   }

   public void setPermissionsType(final LinkPermissionsType permissionsType) {
      this.permissionsType = permissionsType;
   }

   public LinkPermissionsType getPermissionsType() {
      return permissionsType;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof LinkType)) {
         return false;
      }

      final LinkType linkType = (LinkType) o;

      return Objects.equals(id, linkType.id);
   }

   @Override
   public int hashCode() {
      return id != null ? id.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "LinkType{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", version=" + version +
            ", collectionIds=" + collectionIds +
            ", attributes=" + attributes +
            ", lastAttributeNum=" + lastAttributeNum +
            ", linksCount=" + linksCount +
            ", rules=" + rules +
            '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("name", name, MAX_STRING_LENGTH);

      if (attributes != null) attributes.forEach(Attribute::checkHealth);
      if (rules != null) rules.forEach((k, v) -> v.checkHealth());
   }
}
