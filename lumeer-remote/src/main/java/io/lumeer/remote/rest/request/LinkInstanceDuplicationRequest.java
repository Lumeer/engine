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
package io.lumeer.remote.rest.request;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LinkInstanceDuplicationRequest {

   private final String originalMasterDocument;
   private final String newMasterDocument;

   private final Set<String> linkInstanceIds;
   private final Map<String, String> documentMap;

   public LinkInstanceDuplicationRequest(final String originalMasterDocument, final String newMasterDocument, final Set<String> linkInstanceIds, final Map<String, String> documentMap) {
      this.originalMasterDocument = originalMasterDocument;
      this.newMasterDocument = newMasterDocument;
      this.linkInstanceIds = linkInstanceIds;
      this.documentMap = documentMap;
   }

   public String getOriginalMasterDocument() {
      return originalMasterDocument;
   }

   public String getNewMasterDocument() {
      return newMasterDocument;
   }

   public Set<String> getLinkInstanceIds() {
      return linkInstanceIds;
   }

   public Map<String, String> getDocumentMap() {
      return documentMap;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final LinkInstanceDuplicationRequest that = (LinkInstanceDuplicationRequest) o;
      return Objects.equals(originalMasterDocument, that.originalMasterDocument) &&
            Objects.equals(newMasterDocument, that.newMasterDocument) &&
            Objects.equals(linkInstanceIds, that.linkInstanceIds) &&
            Objects.equals(documentMap, that.documentMap);
   }

   @Override
   public int hashCode() {
      return Objects.hash(originalMasterDocument, newMasterDocument, linkInstanceIds, documentMap);
   }
}
