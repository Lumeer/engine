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
package io.lumeer.core.task.executor.request;

import io.lumeer.api.model.ResourceType;

public class PrintRequest extends GenericPrintRequest {

   private final String resourceId;
   private final String documentId;
   private final String attributeId;
   private final ResourceType type;

   public PrintRequest(final String organizationCode, final String projectCode, final String resourceId, final String documentId, final String attributeId, final ResourceType type, final boolean skipPrintDialog) {
      super(organizationCode, projectCode, skipPrintDialog);
      this.resourceId = resourceId;
      this.documentId = documentId;
      this.attributeId = attributeId;
      this.type = type;
   }

   public String getResourceId() {
      return resourceId;
   }

   public String getDocumentId() {
      return documentId;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public ResourceType getType() {
      return type;
   }

   @Override
   public String toString() {
      return "PrintRequest{" +
              "organizationCode='" + organizationCode + '\'' +
              ", projectCode='" + projectCode + '\'' +
              ", skipPrintDialog=" + skipPrintDialog +
              ", resourceId='" + resourceId + '\'' +
              ", documentId='" + documentId + '\'' +
              ", attributeId='" + attributeId + '\'' +
              ", type=" + type +
              '}';
   }
}
