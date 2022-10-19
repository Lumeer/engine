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

public class NavigationRequest {

   private final String organizationCode;
   private final String projectCode;
   private final String viewId;
   private final String collectionId;
   private final String documentId;
   private final String attributeId;
   private final String correlationId;
   private final boolean sidebar;
   private final boolean newWindow;
   private final String search;

   public NavigationRequest(final String organizationCode, final String projectCode, final String viewId, final String collectionId, final String documentId, final String correlationId, final String attributeId, final boolean sidebar, final boolean newWindow, final String search) {
      this.organizationCode = organizationCode;
      this.projectCode = projectCode;
      this.viewId = viewId;
      this.collectionId = collectionId;
      this.documentId = documentId;
      this.correlationId = correlationId;
      this.attributeId = attributeId;
      this.sidebar = sidebar;
      this.newWindow = newWindow;
      this.search = search;
   }

   public String getOrganizationCode() {
      return organizationCode;
   }

   public String getProjectCode() {
      return projectCode;
   }

   public String getViewId() {
      return viewId;
   }

   public String getCollectionId() {
      return collectionId;
   }

   public String getCorrelationId() {
      return correlationId;
   }

   public String getDocumentId() {
      return documentId;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public boolean isSidebar() {
      return sidebar;
   }

   public boolean isNewWindow() {
      return newWindow;
   }

   public String getSearch() {
      return search;
   }

   @Override
   public String toString() {
      return "NavigationRequest{" +
              "organizationCode='" + organizationCode + '\'' +
              ", projectCode='" + projectCode + '\'' +
              ", viewId='" + viewId + '\'' +
              ", collectionId='" + collectionId + '\'' +
              ", documentId='" + documentId + '\'' +
              ", attributeId='" + attributeId + '\'' +
              ", correlationId='" + correlationId + '\'' +
              ", sidebar=" + sidebar +
              ", newWindow=" + newWindow +
              ", search='" + search + '\'' +
              '}';
   }
}
