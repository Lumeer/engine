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
package io.lumeer.api.dto;

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonDocument implements Document {

   private String id;

   private String collectionId;

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
