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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LinkInstance {

   public static final String ID = "_ID";
   public static final String LINK_TYPE_ID = "linkTypeId";
   public static final String DOCUMENTS_IDS = "documentIds";
   public static final String DATA = "data";

   private String id;
   private String linkTypeId;
   private List<String> documentIds;
   private Map<String, Object> data;

   @JsonCreator
   public LinkInstance(@JsonProperty(LINK_TYPE_ID) final String linkTypeId,
         @JsonProperty(DOCUMENTS_IDS) final List<String> documentIds,
         @JsonProperty(DATA) final Map<String, Object> data) {
      this.id = id;
      this.linkTypeId = linkTypeId;
      this.documentIds = documentIds;
      this.data = data;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getLinkTypeId() {
      return linkTypeId;
   }

   public void setLinkTypeId(final String linkTypeId) {
      this.linkTypeId = linkTypeId;
   }

   public List<String> getDocumentIds() {
      return Collections.unmodifiableList(documentIds);
   }

   public void setDocumentIds(final List<String> documentIds) {
      this.documentIds = documentIds;
   }

   public Map<String, Object> getData() {
      return Collections.unmodifiableMap(data);
   }

   public void setData(final Map<String, Object> data) {
      this.data = data;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof LinkInstance)) {
         return false;
      }

      final LinkInstance that = (LinkInstance) o;

      return id != null ? id.equals(that.id) : that.id == null;
   }

   @Override
   public int hashCode() {
      return id != null ? id.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "LinkInstance{" +
            "id='" + id + '\'' +
            ", linkTypeId='" + linkTypeId + '\'' +
            ", documentIds=" + documentIds +
            ", data=" + data +
            '}';
   }

}
