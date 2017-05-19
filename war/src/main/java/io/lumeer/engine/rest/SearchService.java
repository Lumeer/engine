/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.exception.InvalidQueryException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SearchFacade;
import io.lumeer.engine.controller.search.QuerySuggester;
import io.lumeer.engine.controller.search.SuggestionsType;

import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Provides suggestions when creating search queries as well as query execution.
 */
@Path("/{organisation}/{project}/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class SearchService implements Serializable {

   private static final long serialVersionUID = -2420982599853772842L;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private QuerySuggester querySuggester;

   @PathParam("organisation")
   private String organisationCode;

   @PathParam("project")
   private String projectCode;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationCode(organisationCode);
      projectFacade.setCurrentProjectCode(projectCode);
   }

   @GET
   @Path("suggestion")
   public DataDocument suggest(@QueryParam("text") String text, @QueryParam("type") String type) {
      if (text == null || text.isEmpty()) {
         return new DataDocument();
      }

      return querySuggester.suggest(text, SuggestionsType.getType(type));
   }

   /**
    * Queries the data storage in a flexible way. Allows for none or multiple collection names to be specified,
    * automatically sets limit to default values.
    *
    * @param query
    *       Query to execute
    * @return The query result.
    * @throws InvalidQueryException
    *       When it was not possible to execute the query.
    */
   @POST
   @Path("query")
   public List<DataDocument> runQuery(final Query query) throws InvalidQueryException {
      if (query == null) {
         throw new IllegalArgumentException();
      }
      return searchFacade.query(query);
   }
}
