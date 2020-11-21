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
package io.lumeer.remote.rest;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.SuggestionQuery;
import io.lumeer.api.model.Suggestions;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.core.facade.LinkInstanceFacade;
import io.lumeer.core.facade.SearchFacade;
import io.lumeer.core.facade.SuggestionFacade;
import io.lumeer.remote.rest.annotation.QueryProcessor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/search")
public class SearchService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private SuggestionFacade suggestionFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @Path("suggestions")
   public Suggestions getSuggestions(SuggestionQuery suggestionQuery) {
      if (suggestionQuery.getText() == null || suggestionQuery.getText().isEmpty()) {
         return Suggestions.emptySuggestions();
      }

      return suggestionFacade.suggest(suggestionQuery);
   }

   @POST
   @Path("documents")
   @QueryProcessor
   public List<Document> searchDocuments(Query query) {
      Set<String> favoriteDocumentIds = documentFacade.getFavoriteDocumentsIds();
      List<Document> documents = searchFacade.searchDocuments(query);
      Set<String> documentIds = documents.stream().map(Document::getId).collect(Collectors.toSet());
      Map<String, Integer> commentCounts = documentFacade.getCommentsCounts(documentIds);
      documents.forEach(document -> {
         document.setFavorite(favoriteDocumentIds.contains(document.getId()));
         document.setCommentsCount((long) commentCounts.getOrDefault(document.getId(), 0));
      });

      return documents;
   }

   @POST
   @Path("linkInstances")
   @QueryProcessor
   public List<LinkInstance> getLinkInstances(Query query) {
      final List<LinkInstance> links = searchFacade.getLinkInstances(query);

      Set<String> linkIds = links.stream().map(l -> l.getId()).collect(Collectors.toSet());
      Map<String, Integer> commentCounts = linkInstanceFacade.getCommentsCounts(linkIds);
      links.forEach(linkInstance -> {
         linkInstance.setCommentsCount((long) commentCounts.getOrDefault(linkInstance.getId(), 0));
      });

      return links;
   }

}
