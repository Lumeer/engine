/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class CollectionMetadata {

   private final String name;
   private final String code;
   private final List<Attribute> attributes;
   private final Date lastTimeUsed;
   private final List<String> recentlyUsedDocumentIds;
   private final DataDocument customMetadata;
   private final String createdBy;
   private final Date createDate;
   private final String updatedBy;
   private final Date updateDate;
   private final String icon;
   private final String color;
   private final int documentCount;
   private final Map<String, List<Permission>> permissions;

   public CollectionMetadata() {
      this(null, null, Collections.emptyList(), null, Collections.emptyList(), null, null, null, null, null, null, null, 0, Collections.emptyMap());
   }

   public CollectionMetadata(final DataDocument metadata) {
      name = metadata.getString(Collection.REAL_NAME);
      code = metadata.getString(Collection.CODE);

      attributes = new ArrayList<>();
      List<DataDocument> attributeDocuments = metadata.getArrayList(Collection.ATTRIBUTES, DataDocument.class);
      attributes.addAll(attributeDocuments.stream().map(Attribute::new).collect(Collectors.toList()));

      lastTimeUsed = metadata.getDate(Collection.LAST_TIME_USED);
      recentlyUsedDocumentIds = metadata.getArrayList(Collection.RECENTLY_USED_DOCUMENTS, String.class);
      customMetadata = metadata.getDataDocument(Collection.CUSTOM_META);
      createdBy = metadata.getString(Collection.CREATE_USER);
      createDate = metadata.getDate(Collection.CREATE_DATE);
      updatedBy = metadata.getString(Collection.UPDATE_USER);
      updateDate = metadata.getDate(Collection.UPDATE_DATE);
      icon = metadata.getString(Collection.ICON);
      color = metadata.getString(Collection.COLOR);
      documentCount = metadata.getInteger(Collection.DOCUMENT_COUNT, 0);

      permissions = new HashMap<>();

      List<String> keys = Arrays.asList(LumeerConst.Security.USERS_KEY, LumeerConst.Security.GROUP_KEY);
      DataDocument permissionsDocument = metadata.getDataDocument(LumeerConst.Security.PERMISSIONS_KEY);
      for (String key : keys) {
         permissions.put(key, permissionsDocument.getArrayList(key, DataDocument.class).stream()
                                                 .map(Permission::new).collect(Collectors.toList()));
      }
   }

   @JsonCreator
   public CollectionMetadata(final @JsonProperty("name") String name,
         final @JsonProperty("code") String code,
         final @JsonProperty("attributes") List<Attribute> attributes,
         final @JsonProperty("lastTimeUsed") Date lastTimeUsed,
         final @JsonProperty("recentlyUsedDocumentIds") List<String> recentlyUsedDocumentIds,
         final @JsonProperty("customMetadata") DataDocument customMetadata,
         final @JsonProperty("createdBy") String createdBy,
         final @JsonProperty("createDate") Date createDate,
         final @JsonProperty("updatedBy") String updatedBy,
         final @JsonProperty("updateDate") Date updateDate,
         final @JsonProperty("icon") String icon,
         final @JsonProperty("color") String color,
         final @JsonProperty("documentCount") int documentCount,
         final @JsonProperty("permissions") Map<String, List<Permission>> permissions) {
      this.name = name;
      this.code = code;
      this.attributes = attributes;
      this.lastTimeUsed = lastTimeUsed;
      this.recentlyUsedDocumentIds = recentlyUsedDocumentIds;
      this.customMetadata = customMetadata;
      this.createdBy = createdBy;
      this.createDate = createDate;
      this.updatedBy = updatedBy;
      this.updateDate = updateDate;
      this.icon = icon;
      this.color = color;
      this.documentCount = documentCount;
      this.permissions = permissions;
   }

   public String getName() {
      return name;
   }

   public String getCode() {
      return code;
   }

   public List<Attribute> getAttributes() {
      return Collections.unmodifiableList(attributes);
   }

   public Date getLastTimeUsed() {
      return lastTimeUsed != null ? new Date(lastTimeUsed.getTime()) : null;
   }

   public List<String> getRecentlyUsedDocumentIds() {
      return Collections.unmodifiableList(recentlyUsedDocumentIds);
   }

   public DataDocument getCustomMetadata() {
      return customMetadata != null ? new DataDocument(customMetadata) : null;
   }

   public String getCreatedBy() {
      return createdBy;
   }

   public Date getCreateDate() {
      return createDate != null ? new Date(createDate.getTime()) : null;
   }

   public String getColor() {
      return color;
   }

   public String getIcon() {
      return icon;
   }

   public int getDocumentCount() {
      return documentCount;
   }

   public Map<String, List<Permission>> getPermissions() {
      return Collections.unmodifiableMap(permissions);
   }

   public DataDocument toDataDocument() {
      List<DataDocument> attributesList = attributes.stream().map(Attribute::toDataDocument).collect(Collectors.toList());

      return new DataDocument(Collection.REAL_NAME, name)
            .append(Collection.CODE, code)
            .append(Collection.ATTRIBUTES, attributesList)
            .append(Collection.LAST_TIME_USED, lastTimeUsed)
            .append(Collection.RECENTLY_USED_DOCUMENTS, recentlyUsedDocumentIds)
            .append(Collection.CUSTOM_META, customMetadata)
            .append(Collection.CREATE_USER, createdBy)
            .append(Collection.CREATE_DATE, createDate)
            .append(Collection.UPDATE_USER, updatedBy)
            .append(Collection.UPDATE_DATE, updateDate)
            .append(Collection.ICON, icon)
            .append(Collection.COLOR, color)
            .append(Collection.DOCUMENT_COUNT, documentCount);
   }
}
