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
package io.lumeer.api.model.templateParse;

import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.common.WithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceCommentWrapper implements WithId {

   private String id;

   private ResourceType resourceType;
   private String resourceId;
   private String parentId;

   private Long creationDate;
   private Long updateDate;

   private String author;
   private String authorEmail;
   private String authorName;

   private String comment;

   private DataDocument metaData;

   @JsonCreator
   public ResourceCommentWrapper(@JsonProperty("comment") final String comment, @JsonProperty("metaData") final DataDocument metaData) {
      this.comment = comment;
      this.metaData = metaData;
   }

   public ResourceCommentWrapper(final ResourceComment resourceComment) {
      this.id = resourceComment.getId();
      this.resourceType = resourceComment.getResourceType();
      this.resourceId = resourceComment.getResourceId();
      this.parentId = resourceComment.getParentId();
      this.creationDate = resourceComment.getCreationDate() != null ? Date.from(resourceComment.getCreationDate().toInstant()).getTime() : null;
      this.updateDate = resourceComment.getUpdateDate() != null ? Date.from(resourceComment.getUpdateDate().toInstant()).getTime() : null;
      this.author = resourceComment.getAuthor();
      this.authorEmail = resourceComment.getAuthorEmail();
      this.authorName = resourceComment.getAuthorName();
      this.comment = resourceComment.getComment();
      this.metaData = new DataDocument(resourceComment.getMetaData());
   }

   public ResourceComment getResourceComment() {
      final ResourceComment rc = new ResourceComment(comment, metaData);
      rc.setId(id);
      rc.setResourceType(resourceType);
      rc.setResourceId(resourceId);
      rc.setParentId(parentId);
      if (creationDate != null) {
         rc.setCreationDate(ZonedDateTime.ofInstant(new Date(creationDate).toInstant(), ZoneOffset.UTC));
      }
      if (updateDate != null) {
         rc.setUpdateDate(ZonedDateTime.ofInstant(new Date(updateDate).toInstant(), ZoneOffset.UTC));
      }
      rc.setAuthor(author);
      rc.setAuthorEmail(authorEmail);
      rc.setAuthorName(authorName);

      return rc;
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

   public Long getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(final Long creationDate) {
      this.creationDate = creationDate;
   }

   public Long getUpdateDate() {
      return updateDate;
   }

   public void setUpdateDate(final Long updateDate) {
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

   public String getParentId() {
      return parentId;
   }

   public void setParentId(final String parentId) {
      this.parentId = parentId;
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

}
