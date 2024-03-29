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
import io.lumeer.api.model.DocumentsAndLinks;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Query;
import io.lumeer.core.facade.SearchFacade;
import io.lumeer.core.util.Tuple;
import io.lumeer.remote.rest.annotation.HealthCheck;
import io.lumeer.remote.rest.annotation.QueryProcessor;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @Path("documents")
   @QueryProcessor
   @HealthCheck
   public List<Document> searchDocuments(Query query, @QueryParam("subItems") boolean includeSubItems) {
      return searchFacade.searchDocuments(query, includeSubItems);
   }

   @POST
   @Path("linkInstances")
   @QueryProcessor
   @HealthCheck
   public List<LinkInstance> getLinkInstances(Query query, @QueryParam("subItems") boolean includeSubItems) {
      return searchFacade.searchLinkInstances(query, includeSubItems);
   }

   @POST
   @Path("documentsAndLinks")
   @QueryProcessor
   @HealthCheck
   public DocumentsAndLinks getDocumentsAndLinkInstances(Query query, @QueryParam("subItems") boolean includeSubItems) {
      final Tuple<List<Document>, List<LinkInstance>> documentsAndLinks = searchFacade.searchDocumentsAndLinks(query, includeSubItems);

      return new DocumentsAndLinks(documentsAndLinks.getFirst(), documentsAndLinks.getSecond());
   }

   @POST
   @Path("tasks")
   @QueryProcessor
   @HealthCheck
   public DocumentsAndLinks getTaskDocumentsAndLinkInstances(Query query, @QueryParam("subItems") boolean includeSubItems) {
      final Tuple<List<Document>, List<LinkInstance>> documentsAndLinks = searchFacade.searchTasksDocumentsAndLinks(query, includeSubItems);

      return new DocumentsAndLinks(documentsAndLinks.getFirst(), documentsAndLinks.getSecond());
   }

}
