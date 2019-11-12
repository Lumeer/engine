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

import io.lumeer.core.facade.ZapierFacade;

import org.assertj.core.util.Lists;

import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("zapier")
public class ZapierService extends AbstractService {

   @Inject
   private ZapierFacade zapierFacade;

   @GET
   @Path("collection/attributes")
   public List<? super ZapierFacade.ZapierField> getCollectionFields(@QueryParam("collection_id") final String collectionHash) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return Lists.emptyList();
      }

      return zapierFacade.getCollectionFields(collectionId);
   }

   @POST
   @Path("collection/documents")
   public Map<String, Object> createDocument(@QueryParam("collection_id") final String collectionHash, final Map<String, Object> data) {
      final String collectionId = initWorkspace(collectionHash);

      if (collectionId == null) {
         return Map.of();
      }

      return zapierFacade.createDocument(collectionId, data);
   }

   private String initWorkspace(final String collectionHash) {
      final String[] parts = collectionHash.split("/");

      if (parts.length < 3) {
         return null;
      }

      final String organization = parts[0], project = parts[1], collectionId = parts[2];
      workspaceKeeper.setWorkspace(organization, project);

      return collectionId;
   }
}
