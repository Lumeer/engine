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

import io.lumeer.api.model.LinkInstance;
import io.lumeer.core.facade.LinkInstanceFacade;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationCode}/projects/{projectCode}/link-instances")
public class LinkInstanceService extends AbstractService {

   @PathParam("organizationCode")
   private String organizationCode;

   @PathParam("projectCode")
   private String projectCode;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationCode, projectCode);
   }

   @POST
   public LinkInstance createLinkInstance(LinkInstance linkInstance) {
      return linkInstanceFacade.createLinkInstance(linkInstance);
   }

   @PUT
   @Path("{linkInstanceId}")
   public LinkInstance updateLinkInstance(@PathParam("linkInstanceId") String id, LinkInstance linkInstance) {
      return linkInstanceFacade.updateLinkInstance(id, linkInstance);
   }

   @DELETE
   @Path("{linkInstanceId}")
   public Response deleteLinkInstance(@PathParam("linkInstanceId") String id) {
      linkInstanceFacade.deleteLinkInstance(id);

      return Response.ok().link(getParentUri(id), "parent").build();
   }

}
