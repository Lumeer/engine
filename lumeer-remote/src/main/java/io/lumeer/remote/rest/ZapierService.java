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

import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.ConditionValue;
import io.lumeer.api.model.Rule;
import io.lumeer.core.facade.ZapierFacade;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.remote.rest.annotation.QueryProcessor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

   public static class Condition {
      private final String attributeId;
      private final String condition;
      private final Object value;

      @JsonCreator
      public Condition(@JsonProperty("attributeId") final String attributeId, @JsonProperty("condition") final String condition, @JsonProperty("value") final Object value) {
         this.attributeId = attributeId;
         this.condition = condition;
         this.value = value;
      }

      public String getAttributeId() {
         return attributeId;
      }

      public String getCondition() {
         return condition;
      }

      public Object getValue() {
         return value;
      }

      @Override
      public String toString() {
         return "Condition{" +
               "attributeId='" + attributeId + '\'' +
               ", condition='" + condition + '\'' +
               ", value='" + value + '\'' +
               '}';
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
      if (StringUtils.isNotEmpty(organizationId)) {
         workspaceKeeper.setWorkspaceIds(organizationId.trim(), null);

         return zapierFacade.getProjects();
      }

      return List.of();
   }

   @GET
   @Path("collections")
   public List<? extends ZapierFacade.ZapierField> getCollections(@QueryParam("organization_id") final String organizationId, @QueryParam("project_id") final String projectId) {
      if (StringUtils.isNotEmpty(organizationId) && StringUtils.isNotEmpty(projectId)) {
         workspaceKeeper.setWorkspaceIds(organizationId.trim(), projectId.trim());

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

      final String id = cleanInput(data);

      return zapierFacade.createDocument(collectionId, data).append("id", id);
   }

   @PUT
   @Path("collection/documents")
   public DataDocument updateDocument(@QueryParam("collection_hash") final String collectionHash, @QueryParam("key") final String key, @QueryParam("create_update") final Boolean createUpdate, final Map<String, Object> data) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return new DataDocument();
      }

      final String id = cleanInput(data);

      final List<DataDocument> result = zapierFacade.updateDocument(collectionId, key, data);

      if (result.size() <= 0 && createUpdate != null && createUpdate) {
         return zapierFacade.createDocument(collectionId, data).append("updated_count", 1).append("id", id);
      }

      return result.get(0).append("updated_count", result.size()).append("id", id);
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
   @Path("find/documents")
   @QueryProcessor
   public List<DataDocument> findDocuments(@QueryParam("collection_hash") final String collectionHash, final Set<Condition> conditions) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return List.of();
      }

      if (conditions != null) {
         Optional<Condition> idFilter = conditions.stream().filter(c -> "_id".equals(c.getAttributeId())).findFirst();
         if (idFilter.isPresent()) {
            return zapierFacade.getDocument(collectionId, idFilter.get().getValue().toString());
         }
      }

      return zapierFacade.findDocuments(collectionId, conditions != null ? conditions.stream().map(c -> translateCondition(c, collectionId)).collect(Collectors.toSet()) : null);
   }

   private CollectionAttributeFilter translateCondition(final Condition condition, final String collectionId) {
      return new CollectionAttributeFilter(collectionId, condition.getAttributeId(), condition.getCondition(), List.of(new ConditionValue(null, condition.getValue())));
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
      workspaceKeeper.setWorkspaceIds(organization, project);

      return collectionId;
   }

   private String cleanInput(final Map<String, Object> data) {
      data.remove("collection_hash");
      data.remove("organization_id");
      data.remove("project_id");
      data.remove("collection_id");
      data.remove("key_attribute");
      data.remove("create_update");

      final Object o = data.remove("id");

      return o != null ? o.toString() : null;
   }
}
