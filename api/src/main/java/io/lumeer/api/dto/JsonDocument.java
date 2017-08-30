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
package io.lumeer.api.dto;

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonDocument implements Document {

   private String id;

   @JsonIgnore
   private String collectionId;

   private String collectionCode;
   private LocalDateTime creationDate;
   private LocalDateTime updateDate;
   private String createdBy;
   private String updatedBy;
   private Integer dataVersion;
   private DataDocument data;

   @JsonCreator
   public JsonDocument(@JsonProperty("data") final DataDocument data) {
      this.data = data;
   }

   public JsonDocument(final Document document) {
      this.id = document.getId();
      this.collectionId = document.getCollectionId();
      this.collectionCode = document.getCollectionCode();
      this.creationDate = document.getCreationDate();
      this.updateDate = document.getUpdateDate();
      this.createdBy = document.getCreatedBy();
      this.updatedBy = document.getUpdatedBy();
      this.dataVersion = document.getDataVersion();
      this.data = document.getData();
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void setId(final String id) {
      this.id = id;
   }

   @Override
   public String getCollectionId() {
      return collectionId;
   }

   @Override
   public void setCollectionId(final String collectionId) {
      this.collectionId = collectionId;
   }

   @Override
   public String getCollectionCode() {
      return collectionCode;
   }

   @Override
   public void setCollectionCode(final String collectionCode) {
      this.collectionCode = collectionCode;
   }

   @Override
   public LocalDateTime getCreationDate() {
      return creationDate;
   }

   @Override
   public void setCreationDate(final LocalDateTime creationDate) {
      this.creationDate = creationDate;
   }

   @Override
   public LocalDateTime getUpdateDate() {
      return updateDate;
   }

   @Override
   public void setUpdateDate(final LocalDateTime updateDate) {
      this.updateDate = updateDate;
   }

   @Override
   public String getCreatedBy() {
      return createdBy;
   }

   @Override
   public void setCreatedBy(final String createdBy) {
      this.createdBy = createdBy;
   }

   @Override
   public String getUpdatedBy() {
      return updatedBy;
   }

   @Override
   public void setUpdatedBy(final String updatedBy) {
      this.updatedBy = updatedBy;
   }

   @Override
   public Integer getDataVersion() {
      return dataVersion;
   }

   @Override
   public void setDataVersion(final Integer dataVersion) {
      this.dataVersion = dataVersion;
   }

   @Override
   public DataDocument getData() {
      return data;
   }

   @Override
   public void setData(final DataDocument data) {
      this.data = data;
   }

   public static JsonDocument convert(Document document) {
      return document instanceof JsonDocument ? (JsonDocument) document : new JsonDocument(document);
   }

   public static List<JsonDocument> convert(List<Document> documents) {
      return documents.stream()
                      .map(JsonDocument::convert)
                      .collect(Collectors.toList());
   }
}
