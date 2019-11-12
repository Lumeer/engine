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
package io.lumeer.core.facade;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ZapierFacade extends AbstractFacade {

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

   public static class ZapierSelectField extends ZapierField {
      private final List<Map<String, String>> choices;

      public ZapierSelectField(final String key, final String label, final String type, final boolean computed, final List<Map<String, String>> choices) {
         super(key, label, type, computed);
         this.choices = choices;
      }

      public List<Map<String, String>> getChoices() {
         return choices;
      }
   }

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DocumentFacade documentFacade;

   public List<? super ZapierField> getCollectionFields(final String collectionId) {
      final List<? super ZapierField> result = new ArrayList<>();
      final Collection collection = collectionFacade.getCollection(collectionId);

      collection.getAttributes().forEach(attribute -> {
         final String attributeId = attribute.getId();
         final String label = attribute.getName();
         final List<Map<String, String>> choices = new ArrayList<>();
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
               final Map<String, Object> config = (Map<String, Object>) attribute.getConstraint().getConfig();
               final List<Map<String, Object>> options = (List<Map<String, Object>>) config.get("options");

               if (options != null) {
                  options.forEach(opt -> {
                     var displayValue = opt.get("displayValue");
                     if (displayValue != null && !"".equals(displayValue)) {
                        choices.add(Map.of("label", opt.get("displayValue").toString(), "value", opt.get("value").toString()));
                     } else {
                        choices.add(Map.of("label", opt.get("value").toString(), "value", opt.get("value").toString()));
                     }
                  });
               }

               type = "choices";
            }
         }

         if (type.equals("choices")) {
            result.add(new ZapierSelectField(attributeId, label, type, readOnly, choices));
         } else {
            result.add(new ZapierField(attributeId, label, type, readOnly));
         }
      });

      return result;
   }

   public DataDocument createDocument(final String collectionId, final Map<String, Object> data) {
      final DataDocument dataDocument = new DataDocument(data);
      final Document document = new Document(dataDocument);

      return documentFacade.createDocument(collectionId, document).getData();
   }
}
