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
import io.lumeer.api.model.DocumentsChain;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.core.facade.LinkInstanceFacade;

import java.util.List;
import java.util.Set;
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
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/data")
public class DataService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @Path("documentsChain")
   public DocumentsChain createDocumentsChain(DocumentsChain documentsChain) {
      return documentFacade.createDocumentsChain(documentsChain.getDocuments(), documentsChain.getLinkInstances());
   }

   @POST
   @Path("documents")
   public List<Document> getDocuments(Set<String> documentIds) {
      return documentFacade.getDocuments(documentIds);
   }

   @POST
   @Path("linkInstances")
   public List<LinkInstance> getLinkInstances(final Set<String> linkInstanceIds) {
      return linkInstanceFacade.getLinkInstances(linkInstanceIds);
   }

}
