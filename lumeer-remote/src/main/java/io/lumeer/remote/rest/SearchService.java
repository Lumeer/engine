/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.remote.rest;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.dto.JsonSuggestions;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.SuggestionType;
import io.lumeer.core.facade.SearchFacade;
import io.lumeer.core.facade.SuggestionFacade;
import io.lumeer.engine.api.dto.SearchSuggestion;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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

   @GET
   @Path("collections")
   public List<JsonCollection> getCollections(Query query) {
      return searchFacade.searchCollections(query).stream()
                         .map(JsonCollection::convert)
                         .collect(Collectors.toList());

   }

   @GET
   @Path("documents")
   public List<JsonDocument> getDocuments(Query query) {
      return searchFacade.searchDocuments(query).stream()
                         .map(JsonDocument::convert)
                         .collect(Collectors.toList());

   }

   @GET
   @Path("views")
   public List<JsonView> getViews(Query query) {
      return searchFacade.searchViews(query).stream()
                         .map(JsonView::convert)
                         .collect(Collectors.toList());

   }

   private SuggestionType parseSuggestionType(String type) {
      if (type == null) {
         return SuggestionType.ALL;
      }

      try {
         return SuggestionType.valueOf(type);
      } catch (IllegalArgumentException ex) {
         throw new BadRequestException("Unknown suggestion type");
      }
   }

}
