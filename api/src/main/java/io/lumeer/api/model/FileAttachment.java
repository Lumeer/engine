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
import io.lumeer.api.model.common.WithId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class FileAttachment implements WithId, HealthChecking {

   public enum AttachmentType {
      DOCUMENT, LINK;
   }

   public static final String ID = "id";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String PROJECT_ID = "projectId";
   public static final String COLLECTION_ID = "collectionId";
   public static final String DOCUMENT_ID = "documentId";
   public static final String ATTRIBUTE_ID = "attributeId";
   public static final String FILE_NAME = "fileName";
   public static final String UNIQUE_NAME = "uniqueName";
   public static final String ATTACHMENT_TYPE = "attachmentType";
   public static final String PRESIGNED_URL = "presignedUrl";
   public static final String SIZE = "size";

   private String id;
   private final String organizationId;
   private final String projectId;
   private final String collectionId;

   private String documentId;

   private final String attributeId;
   private final AttachmentType attachmentType;
   private String fileName;
   private String uniqueName;
   @JsonProperty(PRESIGNED_URL)
   private String presignedUrl;

   private String createdBy;
   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   @JsonSerialize(using = ZonedDateTimeSerializer.class)
   @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
   private ZonedDateTime creationDate;

   @JsonProperty(SIZE)
   private long size;

   @JsonCreator
   public FileAttachment(@JsonProperty(ORGANIZATION_ID) final String organizationId,
         @JsonProperty(PROJECT_ID) final String projectId, @JsonProperty(COLLECTION_ID) final String collectionId,
         @JsonProperty(DOCUMENT_ID) final String documentId, @JsonProperty(ATTRIBUTE_ID) final String attributeId,
         @JsonProperty(FILE_NAME) final String fileName, @JsonProperty(UNIQUE_NAME) final String uniqueName, @JsonProperty(ATTACHMENT_TYPE) final AttachmentType attachmentType) {
      this.organizationId = organizationId;
      this.projectId = projectId;
      this.collectionId = collectionId;
      this.documentId = documentId;
      this.attributeId = attributeId;
      this.fileName = fileName;
      this.uniqueName = uniqueName;
      this.attachmentType = attachmentType;
   }

   public FileAttachment(final FileAttachment source) {
      this(source.organizationId, source.projectId, source.collectionId, source.documentId, source.attributeId, source.fileName, source.uniqueName, source.attachmentType);
      this.createdBy = source.createdBy;
      this.creationDate = source.creationDate;
   }

   @Override
   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public String getProjectId() {
      return projectId;
   }

   public String getCollectionId() {
      return collectionId;
   }

   public String getDocumentId() {
      return documentId;
   }

   public void setDocumentId(final String documentId) {
      this.documentId = documentId;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public String getFileName() {
      return fileName;
   }

   public AttachmentType getAttachmentType() {
      return attachmentType;
   }

   public void setFileName(final String fileName) {
      this.fileName = fileName;
   }

   public String getPresignedUrl() {
      return presignedUrl;
   }

   public void setPresignedUrl(final String presignedUrl) {
      this.presignedUrl = presignedUrl;
   }

   public long getSize() {
      return size;
   }

   public void setSize(long size) {
      this.size = size;
   }

   public String getUniqueName() {
      return uniqueName;
   }

   public String getCreatedBy() {
      return createdBy;
   }

   public void setCreatedBy(final String createdBy) {
      this.createdBy = createdBy;
   }

   public ZonedDateTime getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(final ZonedDateTime creationDate) {
      this.creationDate = creationDate;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final FileAttachment that = (FileAttachment) o;
      return Objects.equals(id, that.id);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "FileAttachment{" +
            "id='" + id + '\'' +
            ", organizationId='" + organizationId + '\'' +
            ", projectId='" + projectId + '\'' +
            ", collectionId='" + collectionId + '\'' +
            ", documentId='" + documentId + '\'' +
            ", attributeId='" + attributeId + '\'' +
            ", fileName='" + fileName + '\'' +
            ", uniqueName='" + uniqueName + '\'' +
            ", attachmentType='" + attachmentType.toString() + '\'' +
            ", presignedUrl='" + presignedUrl + '\'' +
            ", size='" + size + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", creationDate=" + creationDate +
            '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("fileName", fileName, MAX_LONG_STRING_LENGTH);
   }
}
