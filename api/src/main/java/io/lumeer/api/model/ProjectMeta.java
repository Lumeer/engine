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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ProjectMeta {

   @JsonProperty("prefix")
   private String prefix;

   @JsonProperty("collectionCount")
   private int collectionCount;

   @JsonProperty("linkTypeCount")
   private int linkTypeCount;

   @JsonProperty("viewCount")
   private int viewCount;

   @JsonProperty("documentCount")
   private int documentCount;

   public ProjectMeta(final String prefix, final int collectionCount, final int linkTypeCount, final int viewCount, final int documentCount) {
      this.prefix = prefix;
      this.collectionCount = collectionCount;
      this.linkTypeCount = linkTypeCount;
      this.viewCount = viewCount;
      this.documentCount = documentCount;
   }

   public String getPrefix() {
      return prefix;
   }

   public void setPrefix(final String prefix) {
      this.prefix = prefix;
   }

   public int getCollectionCount() {
      return collectionCount;
   }

   public void setCollectionCount(final int collectionCount) {
      this.collectionCount = collectionCount;
   }

   public int getLinkTypeCount() {
      return linkTypeCount;
   }

   public void setLinkTypeCount(final int linkTypeCount) {
      this.linkTypeCount = linkTypeCount;
   }

   public int getViewCount() {
      return viewCount;
   }

   public void setViewCount(final int viewCount) {
      this.viewCount = viewCount;
   }

   public int getDocumentCount() {
      return documentCount;
   }

   public void setDocumentCount(final int documentCount) {
      this.documentCount = documentCount;
   }
}
