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
import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.rule.ZapierRule;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ZapierFacade extends AbstractFacade {

   public static class ZapierField {
      private final String key;
      private final String label;
      private final String type;
      private final boolean computed;

      @JsonCreator
      public ZapierField(@JsonProperty("key") final String key, @JsonProperty("label") final String label, @JsonProperty("type") final String type, @JsonProperty("computed") final boolean computed) {
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

      @Override
      public boolean equals(final Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         final ZapierField that = (ZapierField) o;
         return computed == that.computed &&
               Objects.equals(key, that.key) &&
               Objects.equals(label, that.label) &&
               Objects.equals(type, that.type);
      }

      @Override
      public int hashCode() {
         return Objects.hash(key, label, type, computed);
      }
   }

   public static class ZapierSelectField extends ZapierField {
      private final List<Map<String, String>> choices;

      @JsonCreator
      public ZapierSelectField(@JsonProperty("key") final String key, @JsonProperty("label") final String label, @JsonProperty("type") final String type, @JsonProperty("computed") final boolean computed, @JsonProperty("choices") final List<Map<String, String>> choices) {
         super(key, label, type, computed);
         this.choices = choices;
      }

      public List<Map<String, String>> getChoices() {
         return choices;
      }

      @Override
      public boolean equals(final Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         if (!super.equals(o)) {
            return false;
         }
         final ZapierSelectField that = (ZapierSelectField) o;
         return Objects.equals(choices, that.choices);
      }

      @Override
      public int hashCode() {
         return Objects.hash(super.hashCode(), choices);
      }
   }


   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   protected AuthenticatedUser authenticatedUser;

   public List<? extends ZapierField> getCollectionFields(final String collectionId) {
      final List<ZapierField> result = new ArrayList<>();
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

   public List<DataDocument> updateDocument(final String collectionId, final String key, final Map<String, Object> data) {
      final List<DataDocument> results = new ArrayList<>();
      final List<Document> documents = searchFacade.searchDocuments(
            new Query(
                  new QueryStem(
                        collectionId,
                        null,
                        null,
                        Set.of(new CollectionAttributeFilter(collectionId, key, "=", data.get(key))),
                        null)
            )
      );

      documents.forEach(document -> {
         results.add(documentFacade.patchDocumentData(collectionId, document.getId(), new DataDocument(data)).getData());
      });

      return results;
   }

   public Rule createCollectionRule(final String collectionId, final Rule.RuleTiming timing, final String hookUrl) {
      final Collection collection = collectionFacade.getCollection(collectionId);

      final Optional<Rule> existingRule = collection.getRules().values().stream().filter(rule ->
         rule.getType() == Rule.RuleType.ZAPIER && rule.getConfiguration().getString(ZapierRule.HOOK_URL).equals(hookUrl)
      ).findFirst();

      if (!existingRule.isPresent()) {
         final Rule rule = new Rule(Rule.RuleType.ZAPIER, timing, new DataDocument(ZapierRule.HOOK_URL, hookUrl).append(ZapierRule.SUBSCRIBE_ID, UUID.randomUUID().toString()));
         final String userEmail = authenticatedUser.getUserEmail();
         collection.getRules().put("Zapier" + (userEmail != null && userEmail.length() > 0 ? " (" + userEmail + ")" : ""), rule);
         collectionFacade.updateCollection(collection.getId(), collection);

         return rule;
      }

      return null;
   }

   public void removeCollectionRule(final String collectionId, final String subscribeId) {
      final Collection collection = collectionFacade.getCollection(collectionId);

      final List<String> ruleKeysForRemoval = collection.getRules().entrySet().stream().filter(entry ->
            entry.getValue().getType() == Rule.RuleType.ZAPIER && entry.getValue().getConfiguration().getString(ZapierRule.SUBSCRIBE_ID).equals(subscribeId)
      ).map(Map.Entry::getKey).collect(Collectors.toList());

      if (ruleKeysForRemoval.size() > 0) {
         ruleKeysForRemoval.forEach(key -> collection.getRules().remove(key));
         collectionFacade.updateCollection(collection.getId(), collection);
      }
   }

   public List<DataDocument> getSampleEntries(final String collectionId, final boolean byUpdate) {
      return documentFacade.getRecentDocuments(collectionId, byUpdate).stream().map(Document::getData).collect(Collectors.toList());
   }

}