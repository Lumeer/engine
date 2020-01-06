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
package io.lumeer.engine.api.event;

import io.lumeer.api.model.Project;

import java.util.List;

public class TemplateCreated {

   private Project project;
   private List<String> collectionIds;
   private List<String> linkTypeIds;
   private List<String> viewIds;

   public TemplateCreated(final Project project, final List<String> collectionIds, final List<String> linkTypeIds, final List<String> viewIds) {
      this.project = project;
      this.collectionIds = collectionIds;
      this.linkTypeIds = linkTypeIds;
      this.viewIds = viewIds;
   }

   public Project getProject() {
      return project;
   }

   public List<String> getCollectionIds() {
      return collectionIds;
   }

   public List<String> getLinkTypeIds() {
      return linkTypeIds;
   }

   public List<String> getViewIds() {
      return viewIds;
   }
}
