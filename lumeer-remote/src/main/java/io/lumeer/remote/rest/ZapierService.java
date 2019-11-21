package io.lumeer.remote.rest;/*
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

import io.lumeer.api.model.Rule;
import io.lumeer.core.facade.ZapierFacade;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("zapier")
public class ZapierService extends AbstractService {

   public static class HookUrl {
      private final String hookUrl;

      @JsonCreator
      public HookUrl(@JsonProperty("hookUrl") final String hookUrl) {
         this.hookUrl = hookUrl;
      }

      public String getHookUrl() {
         return hookUrl;
      }
   }

   @Inject
   private ZapierFacade zapierFacade;

   @GET
   @Path("organizations")
   public List<? extends ZapierFacade.ZapierField> getOrganizations() {
      return zapierFacade.getOrganizations();
   }

   @GET
   @Path("projects")
   public List<? extends ZapierFacade.ZapierField> getProjects(@QueryParam("organization_id") final String organizationId) {
      if (organizationId != null && !"".equals(organizationId)) {
         workspaceKeeper.setWorkspace(organizationId.trim(), null);

         return zapierFacade.getProjects();
      }

      return List.of();
   }

   @GET
   @Path("collections")
   public List<? extends ZapierFacade.ZapierField> getCollections(@QueryParam("organization_id") final String organizationId, @QueryParam("project_id") final String projectId) {
      if (organizationId != null && !"".equals(organizationId) && projectId != null && !"".equals(projectId)) {
         workspaceKeeper.setWorkspace(organizationId.trim(), projectId.trim());

         return zapierFacade.getCollections();
      }

      return List.of();
   }

   @GET
   @Path("collection/attributes")
   public List<? extends ZapierFacade.ZapierField> getCollectionFields(@QueryParam("collection_hash") final String collectionHash) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return List.of();
      }

      return zapierFacade.getCollectionFields(collectionId);
   }

   @POST
   @Path("collection/documents")
   public Map<String, Object> createDocument(@QueryParam("collection_hash") final String collectionHash, final Map<String, Object> data) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return Map.of();
      }

      cleanInput(data);

      return zapierFacade.createDocument(collectionId, data);
   }

   @PUT
   @Path("collection/documents")
   public DataDocument updateDocument(@QueryParam("collection_hash") final String collectionHash, @QueryParam("key") final String key, @QueryParam("create_update") final Boolean createUpdate, final Map<String, Object> data) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return new DataDocument();
      }

      cleanInput(data);

      final List<DataDocument> result = zapierFacade.updateDocument(collectionId, key, data);

      if (result.size() <= 0 && createUpdate != null && createUpdate) {
         return zapierFacade.createDocument(collectionId, data).append("updated_count", 1);
      }

      return result.get(0).append("updated_count", result.size());
   }

   @GET
   @Path("collection/documents")
   public List<DataDocument> getSampleDocuments(@QueryParam("collection_hash") final String collectionHash, @QueryParam("by_update") final Boolean byUpdate) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return List.of();
      }

      return zapierFacade.getSampleEntries(collectionId, byUpdate != null && byUpdate);
   }

   @POST
   @Path("collection/document/created")
   public DataDocument subscribeToDocumentCreated(@QueryParam("collection_hash") final String collectionHash, final HookUrl hookUrl) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId != null) {
         final Rule rule = zapierFacade.createCollectionRule(collectionId, Rule.RuleTiming.CREATE, hookUrl.getHookUrl());

         if (rule != null) {
            return rule.getConfiguration();
         }
      }

      return null;
   }

   @DELETE
   @Path("collection/document/created")
   public DataDocument unsubscribeFromDocumentCreated(@QueryParam("collection_hash") final String collectionHash, @QueryParam("subscribe_id") final String subscribeId) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId != null) {
         zapierFacade.removeCollectionRule(collectionId, subscribeId);

         return new DataDocument("id", subscribeId);
      }

      return null;
   }

   @POST
   @Path("collection/document/updated")
   public DataDocument subscribeToDocumentUpdated(@QueryParam("collection_hash") final String collectionHash, final HookUrl hookUrl) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId != null) {
         final Rule rule = zapierFacade.createCollectionRule(collectionId, Rule.RuleTiming.UPDATE, hookUrl.getHookUrl());

         if (rule != null) {
            return rule.getConfiguration();
         }
      }

      return null;
   }

   @DELETE
   @Path("collection/document/updated")
   public DataDocument unsubscribeFromDocumentUpdated(@QueryParam("collection_hash") final String collectionHash, @QueryParam("subscribe_id") final String subscribeId) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId != null) {
         zapierFacade.removeCollectionRule(collectionId, subscribeId);

         return new DataDocument("id", subscribeId);
      }

      return null;
   }

   private String initWorkspace(final String collectionHash) {
      final String[] parts = collectionHash.trim().split("/");

      if (parts.length < 3) {
         return null;
      }

      final String organization = parts[0], project = parts[1], collectionId = parts[2];
      workspaceKeeper.setWorkspace(organization, project);

      return collectionId;
   }

   private void cleanInput(final Map<String, Object> data) {
      System.out.println(data);
      data.remove("collection_hash");
      data.remove("organization_id");
      data.remove("project_id");
      data.remove("collection_id");
      data.remove("key_attribute");
      data.remove("create_update");
   }
}
