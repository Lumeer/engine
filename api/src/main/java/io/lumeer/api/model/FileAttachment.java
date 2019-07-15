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

import io.lumeer.api.model.common.WithId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FileAttachment implements WithId {

   public static final String ID = "id";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String PROJECT_ID = "projectId";
   public static final String COLLECTION_ID = "collectionId";
   public static final String DOCUMENT_ID = "documentId";
   public static final String ATTRIBUTE_ID = "attributeId";
   public static final String FILE_NAME = "fileName";
   public static final String PRESIGNED_URL = "presignedUrl";
   public static final String SIZE = "size";

   private String id;
   private final String organizationId;
   private final String projectId;
   private final String collectionId;
   private final String documentId;
   private final String attributeId;
   private String fileName;

   @JsonProperty(PRESIGNED_URL)
   private String presignedUrl;

   @JsonProperty(SIZE)
   private long size;

   @JsonCreator
   public FileAttachment(@JsonProperty(ORGANIZATION_ID) final String organizationId,
         @JsonProperty(PROJECT_ID) final String projectId, @JsonProperty(COLLECTION_ID) final String collectionId,
         @JsonProperty(DOCUMENT_ID) final String documentId, @JsonProperty(ATTRIBUTE_ID) final String attributeId,
         @JsonProperty(FILE_NAME) final String fileName) {
      this.organizationId = organizationId;
      this.projectId = projectId;
      this.collectionId = collectionId;
      this.documentId = documentId;
      this.attributeId = attributeId;
      this.fileName = fileName;
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

   public String getAttributeId() {
      return attributeId;
   }

   public String getFileName() {
      return fileName;
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
            ", presignedUrl='" + presignedUrl + '\'' +
            ", size='" + size + '\'' +
            '}';
   }
}
