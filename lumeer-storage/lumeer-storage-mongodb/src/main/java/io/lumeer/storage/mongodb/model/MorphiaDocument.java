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

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.model.common.MorphiaEntity;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Transient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Entity
@Indexes({
      @Index(fields = @Field(MorphiaDocument.COLLECTION_ID))
})
public class MorphiaDocument extends MorphiaEntity implements Document {

   public static final String COLLECTION_ID = "collectionId";
   public static final String CREATION_DATE = "creationDate";
   public static final String UPDATE_DATE = "updateDate";
   public static final String CREATED_BY = "createdBy";
   public static final String UPDATED_BY = "updatedBy";
   public static final String DATA_VERSION = "dataVersion";

   @Property(COLLECTION_ID)
   private String collectionId;

   @Transient
   private String collectionCode;

   @Property(CREATION_DATE)
   private LocalDateTime creationDate;

   @Property(UPDATE_DATE)
   private LocalDateTime updateDate;

   @Property(CREATED_BY)
   private String createdBy;

   @Property(UPDATED_BY)
   private String updatedBy;

   @Property(DATA_VERSION)
   private Integer dataVersion;

   @Transient
   private DataDocument data;

   public MorphiaDocument() {
   }

   public MorphiaDocument(Document document) {
      super(document.getId());

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

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Document)) {
         return false;
      }

      final Document document = (Document) o;

      return getId() != null ? getId().equals(document.getId()) : document.getId() == null;
   }

   @Override
   public int hashCode() {
      return getId() != null ? getId().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "MorphiaDocument{" +
            "id=" + id +
            ", collectionId='" + collectionId + '\'' +
            ", collectionCode='" + collectionCode + '\'' +
            ", creationDate=" + creationDate +
            ", updateDate=" + updateDate +
            ", createdBy='" + createdBy + '\'' +
            ", updatedBy='" + updatedBy + '\'' +
            ", dataVersion=" + dataVersion +
            ", data=" + data +
            '}';
   }

   public org.bson.Document toBsonDocument() {
      DataDocument dataDocument = new DataDocument(COLLECTION_ID, collectionId)
            .append(CREATED_BY, createdBy)
            .append(CREATION_DATE, convertLocalDateTimeToDate(creationDate))
            .append(UPDATED_BY, updatedBy)
            .append(UPDATE_DATE, convertLocalDateTimeToDate(updateDate))
            .append(DATA_VERSION, dataVersion);
      return new org.bson.Document(dataDocument);
   }

   private Date convertLocalDateTimeToDate(LocalDateTime dateTime) {
      if (dateTime == null) {
         return null;
      }
      return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
   }
}
