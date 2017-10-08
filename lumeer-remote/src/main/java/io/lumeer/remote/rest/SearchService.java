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
package io.lumeer.remote.rest;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.dto.JsonSuggestions;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.SuggestionType;
import io.lumeer.core.facade.SearchFacade;
import io.lumeer.core.facade.SuggestionFacade;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationCode}/projects/{projectCode}/search")
public class SearchService extends AbstractService {

   @PathParam("organizationCode")
   private String organizationCode;

   @PathParam("projectCode")
   private String projectCode;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private SuggestionFacade suggestionFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationCode, projectCode);
   }

   @GET
   @Path("suggestions")
   public JsonSuggestions getSuggestions(@QueryParam("text") String text, @QueryParam("type") String type) {
      if (text == null || text.isEmpty()) {
         return JsonSuggestions.emptySuggestions();
      }

      SuggestionType suggestionType = parseSuggestionType(type);
      return suggestionFacade.suggest(text, suggestionType);
   }

   @POST
   @Path("collections")
   public List<JsonCollection> searchCollections(JsonQuery query) {
      return searchFacade.searchCollections(query).stream()
                         .map(JsonCollection::convert)
                         .collect(Collectors.toList());

   }

   @POST
   @Path("documents")
   public List<JsonDocument> searchDocuments(JsonQuery query) {
      return searchFacade.searchDocuments(query).stream()
                         .map(JsonDocument::convert)
                         .collect(Collectors.toList());

   }

   @POST
   @Path("views")
   public List<JsonView> searchViews(JsonQuery query) {
      return searchFacade.searchViews(query).stream()
                         .map(JsonView::convert)
                         .collect(Collectors.toList());

   }

   private SuggestionType parseSuggestionType(String type) {
      if (type == null) {
         return SuggestionType.ALL;
      }

      try {
         return SuggestionType.fromString(type);
      } catch (IllegalArgumentException ex) {
         throw new BadRequestException("Unknown suggestion type");
      }
   }

}
