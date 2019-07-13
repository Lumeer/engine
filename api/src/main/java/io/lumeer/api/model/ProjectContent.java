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

import io.lumeer.api.model.template.CollectionWithId;
import io.lumeer.api.model.template.DocumentWithId;
import io.lumeer.api.model.template.LinkInstanceWithId;
import io.lumeer.api.model.template.LinkTypeWithId;
import io.lumeer.api.model.template.ViewWithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ProjectContent {

   @JsonProperty("templateMeta")
   private ProjectMeta templateMeta;

   @JsonProperty("collections")
   private List<CollectionWithId> collections;

   @JsonProperty("views")
   private List<ViewWithId> views;

   @JsonProperty("linkTypes")
   private List<LinkTypeWithId> linkTypes;

   @JsonProperty("linkInstances")
   private List<LinkInstanceWithId> linkInstances;

   @JsonProperty("linkData")
   private Map<String, List<DataDocument>> linkData;

   @JsonProperty("documents")
   private List<DocumentWithId> documents;

   @JsonProperty("data")
   private Map<String, List<DataDocument>> data;

   public ProjectMeta getTemplateMeta() {
      return templateMeta;
   }

   public void setTemplateMeta(final ProjectMeta templateMeta) {
      this.templateMeta = templateMeta;
   }

   public List<CollectionWithId> getCollections() {
      return collections;
   }

   public void setCollections(final List<CollectionWithId> collections) {
      this.collections = collections;
   }

   public List<ViewWithId> getViews() {
      return views;
   }

   public void setViews(final List<ViewWithId> views) {
      this.views = views;
   }

   public List<LinkTypeWithId> getLinkTypes() {
      return linkTypes;
   }

   public void setLinkTypes(final List<LinkTypeWithId> linkTypes) {
      this.linkTypes = linkTypes;
   }

   public List<LinkInstanceWithId> getLinkInstances() {
      return linkInstances;
   }

   public void setLinkInstances(final List<LinkInstanceWithId> linkInstances) {
      this.linkInstances = linkInstances;
   }

   public Map<String, List<DataDocument>> getLinkData() {
      return linkData;
   }

   public void setLinkData(final Map<String, List<DataDocument>> linkData) {
      this.linkData = linkData;
   }

   public List<DocumentWithId> getDocuments() {
      return documents;
   }

   public void setDocuments(final List<DocumentWithId> documents) {
      this.documents = documents;
   }

   public Map<String, List<DataDocument>> getData() {
      return data;
   }

   public void setData(final Map<String, List<DataDocument>> data) {
      this.data = data;
   }
}
