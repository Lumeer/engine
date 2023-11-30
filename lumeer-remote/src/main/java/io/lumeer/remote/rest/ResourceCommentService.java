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

import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.core.facade.ResourceCommentFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/comments/{resourceType:[a-zA-Z]{1,12}}/{resourceId:[0-9a-fA-F]{24}}/")
public class ResourceCommentService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @PathParam("resourceId")
   private String resourceId;

   @PathParam("resourceType")
   private String resourceType;

   private ResourceType type;

   @Inject
   private ResourceCommentFacade resourceCommentFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
      if (resourceType != null) {
         type = ResourceType.fromString(resourceType);
      }
   }

   @POST
   @HealthCheck
   public ResourceComment createComment(final ResourceComment comment) {
      comment.setResourceType(type);
      comment.setResourceId(resourceId);

      return resourceCommentFacade.createResourceComment(comment);
   }

   @PUT
   @HealthCheck
   public ResourceComment updateComment(final ResourceComment comment) {
      comment.setResourceType(type);
      comment.setResourceId(resourceId);

      return resourceCommentFacade.updateResourceComment(comment);
   }

   @GET
   public List<ResourceComment> getComments(@QueryParam("pageStart") Integer pageStart, @QueryParam("pageLength") Integer pageLength) {
      return resourceCommentFacade.getComments(type, resourceId, pageStart != null ? pageStart : 0, pageLength != null ? pageLength : 0);
   }

   @DELETE
   @Path("{commentId:[0-9a-fA-F]{24}}")
   public void deleteComment(@PathParam("commentId") final String commentId) {
      final ResourceComment comment = new ResourceComment(null, null);
      comment.setId(commentId);
      comment.setResourceType(type);
      comment.setResourceId(resourceId);

      resourceCommentFacade.deleteComment(comment);
   }
}
