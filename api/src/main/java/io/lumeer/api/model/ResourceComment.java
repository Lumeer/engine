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
import io.lumeer.api.model.common.WithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceComment implements WithId {

   private String id;

   private ResourceType resourceType;
   private String resourceId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime creationDate;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime updateDate;

   private String author;
   private String authorEmail;
   private String authorName;

   private String comment;

   private DataDocument metaData;

   @JsonCreator
   public ResourceComment(@JsonProperty("comment") final String comment, @JsonProperty("metaData") final DataDocument metaData) {
      this.comment = comment;
      this.metaData = metaData;
   }

   @Override
   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public ResourceType getResourceType() {
      return resourceType;
   }

   public void setResourceType(final ResourceType resourceType) {
      this.resourceType = resourceType;
   }

   public String getResourceId() {
      return resourceId;
   }

   public void setResourceId(final String resourceId) {
      this.resourceId = resourceId;
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

   public String getAuthor() {
      return author;
   }

   public void setAuthor(final String author) {
      this.author = author;
   }

   public String getAuthorEmail() {
      return authorEmail;
   }

   public void setAuthorEmail(final String authorEmail) {
      this.authorEmail = authorEmail;
   }

   public String getAuthorName() {
      return authorName;
   }

   public void setAuthorName(final String authorName) {
      this.authorName = authorName;
   }

   public String getComment() {
      return comment;
   }

   public void setComment(final String comment) {
      this.comment = comment;
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

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final ResourceComment that = (ResourceComment) o;
      return Objects.equals(id, that.id) &&
            resourceType == that.resourceType &&
            Objects.equals(resourceId, that.resourceId);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, resourceType, resourceId);
   }

   @Override
   public String toString() {
      return "ResourceComment{" +
            "id='" + id + '\'' +
            ", resourceType=" + resourceType +
            ", resourceId='" + resourceId + '\'' +
            ", creationDate=" + creationDate +
            ", updateDate=" + updateDate +
            ", author='" + author + '\'' +
            ", authorEmail='" + authorEmail + '\'' +
            ", authorName='" + authorName + '\'' +
            ", comment='" + comment + '\'' +
            ", metaData=" + metaData +
            '}';
   }
}
