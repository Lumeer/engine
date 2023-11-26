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
import io.lumeer.api.model.common.WithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document implements WithId {

   public static final String DATA = "data";
   public static final String META_PARENT_ID = "parentId";
   public static final String META_TEMPLATE_ID = "templateId"; // for importing and template creation
   public static final String META_ORIGINAL_DOCUMENT_ID = "originalDocumentId"; // for duplicating documents
   public static final String META_ORIGINAL_PARENT_ID = "originalParentId"; // for duplicating values
   public static final String META_CORRELATION_ID = "correlationId"; // for duplicating documents

   private String id;

   private String collectionId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   @JsonSerialize(using = ZonedDateTimeSerializer.class)
   @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
   private ZonedDateTime creationDate;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   @JsonSerialize(using = ZonedDateTimeSerializer.class)
   @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
   private ZonedDateTime updateDate;

   private String createdBy;
   private String updatedBy;
   private Integer dataVersion;
   private DataDocument data;
   private DataDocument metaData;
   private Long commentsCount;

   private boolean favorite;

   @JsonCreator
   public Document(@JsonProperty(DATA) final DataDocument data) {
      this.data = data;
   }

   public Document(final Document document) {
      this.id = document.getId();
      this.collectionId = document.getCollectionId();
      this.creationDate = document.getCreationDate();
      this.updateDate = document.getUpdateDate();
      this.createdBy = document.getCreatedBy();
      this.updatedBy = document.getUpdatedBy();
      this.dataVersion = document.getDataVersion();
      this.data = document.getData() != null ? new DataDocument(document.getData()) : new DataDocument();
      this.metaData = document.getMetaData() != null ? new DataDocument(document.getMetaData()) : new DataDocument();
   }

   public Document(final String collectionId, final ZonedDateTime creationDate, final ZonedDateTime updateDate, final String createdBy, final String updatedBy, final Integer dataVersion, final DataDocument metaData) {
      this.collectionId = collectionId;
      this.creationDate = creationDate;
      this.updateDate = updateDate;
      this.createdBy = createdBy;
      this.updatedBy = updatedBy;
      this.dataVersion = dataVersion;
      this.metaData = metaData;
   }

   public Document(final String collectionId, final ZonedDateTime creationDate, final String createdBy) {
      this.collectionId = collectionId;
      this.creationDate = creationDate;
      this.updateDate = null;
      this.createdBy = createdBy;
      this.updatedBy = null;
      this.dataVersion = 0;
      this.metaData = new DataDocument();
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getCollectionId() {
      return collectionId;
   }

   public void setCollectionId(final String collectionId) {
      this.collectionId = collectionId;
   }

   public ZonedDateTime getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(final ZonedDateTime creationDate) {
      this.creationDate = creationDate;
   }

   public ZonedDateTime getUpdateDate() {
      return updateDate;
   }

   public void setUpdateDate(final ZonedDateTime updateDate) {
      this.updateDate = updateDate;
   }

   public String getCreatedBy() {
      return createdBy;
   }

   public void setCreatedBy(final String createdBy) {
      this.createdBy = createdBy;
   }

   public String getUpdatedBy() {
      return updatedBy;
   }

   public void setUpdatedBy(final String updatedBy) {
      this.updatedBy = updatedBy;
   }

   public Integer getDataVersion() {
      return dataVersion;
   }

   public void setDataVersion(final Integer dataVersion) {
      this.dataVersion = dataVersion;
   }

   public DataDocument getData() {
      return data;
   }

   public void setData(final DataDocument data) {
      this.data = data;
   }

   public DataDocument getMetaData() {
      return metaData;
   }

   public DataDocument createIfAbsentMetaData() {
      if (metaData == null) {
         this.metaData = new DataDocument();
      }

      return metaData;
   }

   public void setMetaData(final DataDocument metaData) {
      this.metaData = metaData;
   }

   public boolean isFavorite() {
      return favorite;
   }

   public void setFavorite(final boolean favorite) {
      this.favorite = favorite;
   }

   public Long getCommentsCount() {
      return commentsCount;
   }

   public void setCommentsCount(final Long commentsCount) {
      this.commentsCount = commentsCount;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Document document = (Document) o;
      return Objects.equals(id, document.id) &&
            Objects.equals(collectionId, document.collectionId);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, collectionId);
   }

   @Override
   public String toString() {
      return "Document{" +
            "id='" + id + '\'' +
            ", collectionId='" + collectionId + '\'' +
            ", creationDate=" + creationDate +
            ", updateDate=" + updateDate +
            ", createdBy='" + createdBy + '\'' +
            ", updatedBy='" + updatedBy + '\'' +
            ", dataVersion=" + dataVersion +
            ", data=" + data +
            ", metaData=" + metaData +
            ", favorite=" + favorite +
            ", commentsCount=" + commentsCount +
            '}';
   }
}
