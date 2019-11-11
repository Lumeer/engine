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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.core.facade.CollectionFacade;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("zapier")
public class ZapierService extends AbstractService {

   public static class ZapierField {
      private final String key;
      private final String label;
      private final String type;
      private final boolean computed;

      public ZapierField(final String key, final String label, final String type, final boolean computed) {
         this.key = key;
         this.label = label;
         this.type = type;
         this.computed = computed;
      }

      public String getKey() {
         return key;
      }

      public String getLabel() {
         return label;
      }

      public String getType() {
         return type;
      }

      public boolean isComputed() {
         return computed;
      }
   }

   @Inject
   private CollectionFacade collectionFacade;

   public List<ZapierField> getCollectionFields(@QueryParam("collection_id") final String collectionHash) {
      final List<ZapierField> result = new ArrayList<>();
      final String[] parts = collectionHash.split("/");

      if (parts.length < 3) {
         return result;
      }

      final String organization = parts[0], project = parts[1], collectionId = parts[3];
      workspaceKeeper.setWorkspace(organization, project);

      final Collection collection = collectionFacade.getCollection(collectionId);
      collection.getAttributes().forEach(attribute -> {
         final String attributeId = attribute.getId();
         final String label = attribute.getName();
         final boolean readOnly = attribute.getFunction() != null && !attribute.getFunction().isEditable();
         String type = "string";

         if (attribute.getConstraint() != null) {
            if (attribute.getConstraint().getType() == ConstraintType.DateTime) {
               type = "datetime";
            } else if (attribute.getConstraint().getType() == ConstraintType.Number) {
               type = "number";
            } else if (attribute.getConstraint().getType() == ConstraintType.Boolean) {
               type = "boolean";
            } else if (attribute.getConstraint().getType() == ConstraintType.Select) {
               // init choices
               // https://github.com/zapier/zapier-platform/blob/master/packages/schema/docs/build/schema.md#fieldschema
            }
         }

         result.add(new ZapierField(attributeId, label, type, readOnly));
      });

      return result;
   }
}
