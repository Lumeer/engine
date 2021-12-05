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

import io.lumeer.api.model.templateParse.CollectionWithId;
import io.lumeer.api.model.templateParse.DocumentWithId;
import io.lumeer.api.model.templateParse.LinkInstanceWithId;
import io.lumeer.api.model.templateParse.LinkTypeWithId;
import io.lumeer.api.model.templateParse.ResourceCommentWrapper;
import io.lumeer.api.model.templateParse.ViewWithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

   @JsonProperty("favoriteCollections")
   private Set<String> favoriteCollectionIds;

   @JsonProperty("favoriteViews")
   private Set<String> favoriteViewIds;

   @JsonProperty("sequences")
   private List<Sequence> sequences;

   @JsonProperty("selectionLists")
   private List<SelectionList> selectionLists;

   @JsonProperty("variables")
   private List<ResourceVariable> variables;

   @JsonProperty("comments")
   private List<ResourceCommentWrapper> comments;

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

   public Set<String> getFavoriteCollectionIds() {
      return favoriteCollectionIds;
   }

   public void setFavoriteCollectionIds(final Set<String> favoriteCollectionIds) {
      this.favoriteCollectionIds = favoriteCollectionIds;
   }

   public Set<String> getFavoriteViewIds() {
      return favoriteViewIds;
   }

   public void setFavoriteViewIds(final Set<String> favoriteViewIds) {
      this.favoriteViewIds = favoriteViewIds;
   }

   public List<Sequence> getSequences() {
      return sequences;
   }

   public void setSequences(final List<Sequence> sequences) {
      this.sequences = sequences;
   }

   public List<ResourceCommentWrapper> getComments() {
      return comments;
   }

   public void setComments(final List<ResourceCommentWrapper> comments) {
      this.comments = comments;
   }

   public List<SelectionList> getSelectionLists() {
      return selectionLists;
   }

   public void setSelectionLists(final List<SelectionList> selectionLists) {
      this.selectionLists = selectionLists;
   }

   public List<ResourceVariable> getVariables() {
      return variables;
   }

   public void setVariables(final List<ResourceVariable> variables) {
      this.variables = variables;
   }
}
