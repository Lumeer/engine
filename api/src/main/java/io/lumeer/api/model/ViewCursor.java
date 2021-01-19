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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ViewCursor {
   private final String collectionId;
   private final String linkTypeId;
   private final String documentId;
   private final String linkInstanceId;
   private final String attributeId;
   private final Boolean sidebar;

   public ViewCursor(final String collectionId, final String linkTypeId, final String documentId, final String linkInstanceId, final String attributeId, final Boolean sidebar) {
      this.collectionId = collectionId;
      this.linkTypeId = linkTypeId;
      this.documentId = documentId;
      this.linkInstanceId = linkInstanceId;
      this.attributeId = attributeId;
      this.sidebar = sidebar;
   }

   public String getCollectionId() {
      return collectionId;
   }

   public String getLinkTypeId() {
      return linkTypeId;
   }

   public String getDocumentId() {
      return documentId;
   }

   public String getLinkInstanceId() {
      return linkInstanceId;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public Boolean getSidebar() {
      return sidebar;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final ViewCursor that = (ViewCursor) o;
      return Objects.equals(collectionId, that.collectionId) && Objects.equals(linkTypeId, that.linkTypeId) && Objects.equals(documentId, that.documentId) && Objects.equals(linkInstanceId, that.linkInstanceId) && Objects.equals(attributeId, that.attributeId) && Objects.equals(sidebar, that.sidebar);
   }

   @Override
   public int hashCode() {
      return Objects.hash(collectionId, linkTypeId, documentId, linkInstanceId, attributeId, sidebar);
   }

   @Override
   public String toString() {
      return "ViewCursor{" +
            "collectionId='" + collectionId + '\'' +
            ", linkTypeId='" + linkTypeId + '\'' +
            ", documentId='" + documentId + '\'' +
            ", linkInstanceId='" + linkInstanceId + '\'' +
            ", attributeId='" + attributeId + '\'' +
            ", sidebar=" + sidebar +
            '}';
   }

   /**
    * Incomplete implementation that needs to be extended for more use cases.
    *
    * @return partial URL query string representing this stem
    */
   public String toQueryString() {
      Map<String, Object> elements = new HashMap<>();
      if (StringUtils.isNotEmpty(collectionId)) {
         elements.put("c", collectionId);
      }
      if (StringUtils.isNotEmpty(linkTypeId)) {
         elements.put("t", linkTypeId);
      }
      if (StringUtils.isNotEmpty(documentId)) {
         elements.put("d", documentId);
      }
      if (StringUtils.isNotEmpty(linkInstanceId)) {
         elements.put("l", linkInstanceId);
      }
      if (StringUtils.isNotEmpty(attributeId)) {
         elements.put("a", attributeId);
      }
      if (BooleanUtils.isTrue(sidebar)) {
         elements.put("s", sidebar);
      }

      return new JSONObject(elements).toString();
   }

}
