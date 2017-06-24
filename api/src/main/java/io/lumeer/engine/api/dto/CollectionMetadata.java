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

import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@Immutable
public class CollectionMetadata {

   private String name;
   private String internalName;
   private List<Attribute> attributes = new ArrayList<>();
   private Date lastTimeUsed;
   private List<String> recentlyUsedDocumentIds = new ArrayList<>();
   private DataDocument customMetadata = new DataDocument();
   private String creator;
   private Date createDate;

   public CollectionMetadata() {
   }

   public CollectionMetadata(final DataDocument metadata) {
      name = metadata.getString(Collection.REAL_NAME_KEY);
      internalName = metadata.getString(Collection.INTERNAL_NAME_KEY);

      List<DataDocument> attributeDocuments = metadata.getArrayList(Collection.ATTRIBUTES_KEY, DataDocument.class);
      attributes.addAll(attributeDocuments.stream().map(Attribute::new).collect(Collectors.toList()));

      lastTimeUsed = metadata.getDate(Collection.LAST_TIME_USED_KEY);
      recentlyUsedDocumentIds = metadata.getArrayList(Collection.RECENTLY_USED_DOCUMENTS_KEY, String.class);
      customMetadata = metadata.getDataDocument(Collection.CUSTOM_META_KEY);
      creator = metadata.getString(Collection.CREATE_USER_KEY);
      createDate = metadata.getDate(Collection.CREATE_DATE_KEY);
   }

   @JsonCreator
   public CollectionMetadata(final @JsonProperty(Collection.REAL_NAME_KEY) String name,
         final @JsonProperty(Collection.INTERNAL_NAME_KEY) String internalName,
         final @JsonProperty(Collection.ATTRIBUTES_KEY) List<Attribute> attributes,
         final @JsonProperty(Collection.LAST_TIME_USED_KEY) Date lastTimeUsed,
         final @JsonProperty(Collection.RECENTLY_USED_DOCUMENTS_KEY) List<String> recentlyUsedDocumentIds,
         final @JsonProperty(Collection.CUSTOM_META_KEY) DataDocument customMetadata,
         final @JsonProperty(Collection.CREATE_USER_KEY) String creator,
         final @JsonProperty(Collection.CREATE_DATE_KEY) Date createDate) {
      this.name = name;
      this.internalName = internalName;
      this.attributes = attributes;
      this.lastTimeUsed = lastTimeUsed;
      this.recentlyUsedDocumentIds = recentlyUsedDocumentIds;
      this.customMetadata = customMetadata;
      this.creator = creator;
      this.createDate = createDate;
   }

   public String getName() {
      return name;
   }

   public String getInternalName() {
      return internalName;
   }

   public List<Attribute> getAttributes() {
      return attributes;
   }

   public Date getLastTimeUsed() {
      return lastTimeUsed;
   }

   public List<String> getRecentlyUsedDocumentIds() {
      return recentlyUsedDocumentIds;
   }

   public DataDocument getCustomMetadata() {
      return customMetadata;
   }

   public String getCreator() {
      return creator;
   }

   public Date getCreateDate() {
      return createDate;
   }

   public DataDocument toDataDocument() {
      List<DataDocument> attributesList = attributes.stream().map(Attribute::toDataDocument).collect(Collectors.toList());

      return new DataDocument(Collection.REAL_NAME_KEY, name)
            .append(Collection.INTERNAL_NAME_KEY, internalName)
            .append(Collection.ATTRIBUTES_KEY, attributesList)
            .append(Collection.LAST_TIME_USED_KEY, lastTimeUsed)
            .append(Collection.RECENTLY_USED_DOCUMENTS_KEY, recentlyUsedDocumentIds)
            .append(Collection.CUSTOM_META_KEY, customMetadata)
            .append(Collection.CREATE_USER_KEY, creator)
            .append(Collection.CREATE_DATE_KEY, createDate);
   }
}
