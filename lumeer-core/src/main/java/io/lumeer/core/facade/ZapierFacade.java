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
import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.rule.ZapierRule;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
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

   public static final String CHANGED_PREFIX = "_changed_";
   public static final String PREVIOUS_VALUE_PREFIX = "_previous_";

   public static class ZapierField {
      private final String key;
      private final String label;
      private final String type;
      private final boolean computed;
      private final boolean required;
      private final boolean altersDynamicFields;

      @JsonCreator
      public ZapierField(@JsonProperty("key") final String key, @JsonProperty("label") final String label, @JsonProperty("type") final String type, @JsonProperty("computed") final boolean computed, @JsonProperty("required") final boolean required, @JsonProperty("altersDynamicFields") final boolean altersDynamicFields) {
         this.key = key;
         this.label = label;
         this.type = type;
         this.computed = computed;
         this.required = required;
         this.altersDynamicFields = altersDynamicFields;
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

      public boolean isRequired() {
         return required;
      }

      public boolean isAltersDynamicFields() {
         return altersDynamicFields;
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
               required == that.required &&
               altersDynamicFields == that.altersDynamicFields &&
               Objects.equals(key, that.key) &&
               Objects.equals(label, that.label) &&
               Objects.equals(type, that.type);
      }

      @Override
      public int hashCode() {
         return Objects.hash(key, label, type, computed, required, altersDynamicFields);
      }
   }

   public static class ZapierSelectField extends ZapierField {
      private final List<Map<String, String>> choices;

      @JsonCreator
      public ZapierSelectField(@JsonProperty("key") final String key, @JsonProperty("label") final String label, @JsonProperty("type") final String type, @JsonProperty("computed") final boolean computed, @JsonProperty("required") final boolean required, @JsonProperty("altersDynamicFields") final boolean altersDynamicFields, @JsonProperty("choices") final List<Map<String, String>> choices) {
         super(key, label, type, computed, required, altersDynamicFields);
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
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   protected AuthenticatedUser authenticatedUser;

   public List<? extends ZapierField> getOrganizations() {
      final List<Map<String, String>> choices = new ArrayList<>();

      organizationFacade.getOrganizations().forEach(organization -> {
         choices.add(Map.of("value", organization.getId(), "label", organization.getCode() + ": " + organization.getName()));
      });

      return List.of(new ZapierSelectField("organization_id", "Organization", "string", false, true, true, choices));
   }

   public List<? extends ZapierField> getProjects() {
      final List<Map<String, String>> choices = new ArrayList<>();

      projectFacade.getProjects().forEach(project -> {
         choices.add(Map.of("value", project.getId(), "label", project.getCode() + ": " + project.getName()));
      });

      return List.of(new ZapierSelectField("project_id", "Project", "string", false, true, true, choices));
   }

   public List<? extends ZapierField> getCollections() {
      final List<Map<String, String>> choices = new ArrayList<>();

      collectionFacade.getCollections().forEach(collection -> {
         choices.add(Map.of("value", collection.getId(), "label", collection.getName()));
      });

      return List.of(new ZapierSelectField("collection_id", "Table", "string", false, true, true, choices));
   }

   public List<? extends ZapierField> getCollectionFields(final String collectionId) {
      final List<ZapierField> result = new ArrayList<>();
      final Collection collection = collectionFacade.getCollection(collectionId);

      result.add(new ZapierField("_id", "_id", "string", true, false, false));

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
            result.add(new ZapierSelectField(attributeId, label, type, readOnly, false, false, choices));
         } else {
            result.add(new ZapierField(attributeId, label, type, readOnly, false, false));
         }
      });

      return result;
   }

   public DataDocument createDocument(final String collectionId, final Map<String, Object> data) {
      final Collection collection = collectionFacade.getCollection(collectionId);
      final DataDocument dataDocument = new DataDocument(data);
      final Document document = new Document(dataDocument);
      final Document createdDocument = documentFacade.createDocument(collectionId, document);

      return translateAttributes(collection, addMissingAttributes(createdDocument.getData().append("_id", createdDocument.getId()), collection));
   }

   public List<DataDocument> updateDocument(final String collectionId, final String key, final Map<String, Object> data) {
      final List<DataDocument> results = new ArrayList<>();
      final Collection collection = collectionFacade.getCollection(collectionId);
      List<Document> documents;

      if (key.equals("_id")) {
         if (data != null && data.containsKey("_id")) {
            documents = List.of(documentFacade.getDocument(collectionId, data.get("_id").toString()));
         } else {
            return List.of();
         }
      } else {
         documents = searchFacade.searchDocuments(
               new Query(
                     new QueryStem(
                           collectionId,
                           null,
                           null,
                           Collections.singletonList(CollectionAttributeFilter.createFromValues(collectionId, key, ConditionType.EQUALS, data.get(key))),
                           null)
               ),
               Language.EN
         );
      }

      documents.forEach(document -> {
         results.add(translateAttributes(collection, addMissingAttributes(documentFacade.patchDocumentData(collectionId, document.getId(), new DataDocument(data)).getData(), collection)));
      });

      return results;
   }

   public Rule createCollectionRule(final String collectionId, final Rule.RuleTiming timing, final String hookUrl) {
      final Collection collection = collectionFacade.getCollection(collectionId);

      final Optional<Rule> existingRule = collection.getRules().values().stream().filter(rule ->
            rule.getType() == Rule.RuleType.ZAPIER && rule.getConfiguration().getString(ZapierRule.HOOK_URL).equals(hookUrl)
      ).findFirst();

      if (existingRule.isEmpty()) {
         final String subscribeId = UUID.randomUUID().toString();
         final String userEmail = authenticatedUser.getUserEmail();
         final String ruleName = "Zapier" + (userEmail != null && userEmail.length() > 0 ? " (" + userEmail + ")" : "") + " " + subscribeId;
         final Rule rule = new Rule(ruleName, Rule.RuleType.ZAPIER, timing, new DataDocument(ZapierRule.HOOK_URL, hookUrl).append(ZapierRule.SUBSCRIBE_ID, subscribeId));
         collection.getRules().put(ruleName, rule);
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
      final Collection collection = collectionFacade.getCollection(collectionId);

      return documentFacade
            .getRecentDocuments(collectionId, byUpdate)
            .stream()
            .map(Document::getData)
            .map(data -> addMissingAttributes(data, collection))
            .map(ZapierFacade::addModifiers)
            .map(data -> translateAttributes(collection, data))
            .collect(Collectors.toList());
   }

   public List<DataDocument> findDocuments(final String collectionId, final List<CollectionAttributeFilter> collectionAttributeFilters) {
      final Collection collection = collectionFacade.getCollection(collectionId);

      return searchFacade.searchDocuments(new Query(List.of(new QueryStem(collectionId, null, null, collectionAttributeFilters, null)), null, 0, 20), Language.EN)
             .stream()
             .map(Document::getData)
             .map(data -> addMissingAttributes(data, collection))
             .map(data -> translateAttributes(collection, data))
             .collect(Collectors.toList());
   }

   public List<DataDocument> getDocument(final String collectionId, final String documentId) {
      final Document document = documentFacade.getDocument(collectionId, documentId);
      final Collection collection = collectionFacade.getCollection(collectionId);

      return List.of(translateAttributes(collection, addMissingAttributes(document.getData(), collection)));
   }

   public static DataDocument addMissingAttributes(final DataDocument data, final Collection collection) {
      collection.getAttributes().forEach(attribute -> {
         if (!data.containsKey(attribute.getId())) {
            data.append(attribute.getId(), "");
         }
      });

      return data;
   }

   public static DataDocument addModifiers(final DataDocument data, final DataDocument oldDocument) {
      final DataDocument result = new DataDocument(data);

      data.entrySet().stream().forEach(entry -> {
         if (!entry.getKey().equals("_id")) {
            if (oldDocument == null) {
               result.append(CHANGED_PREFIX + entry.getKey(), false);
               result.append(PREVIOUS_VALUE_PREFIX + entry.getKey(), entry.getValue());
            } else {
               final Object oldValue = oldDocument.get(entry.getKey());
               final Object newValue = entry.getValue();

               if ((oldValue == null && newValue != null) || (newValue == null && oldValue != null) || (newValue != null && !newValue.equals(oldValue))) {
                  result.put(ZapierFacade.CHANGED_PREFIX + entry.getKey(), true);
               } else {
                  result.put(ZapierFacade.CHANGED_PREFIX + entry.getKey(), false);
               }

               result.put(ZapierFacade.PREVIOUS_VALUE_PREFIX + entry.getKey(), oldValue);
            }
         }
      });

      return result;
   }

   public static DataDocument addModifiers(final DataDocument data) {
      return addModifiers(data, null);
   }

   public static DataDocument translateAttributes(final Collection collection, final DataDocument data) {
      final Map<String, String> attributeNames = collection.getAttributes().stream().map(attribute -> Map.entry(attribute.getId(), attribute.getName())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      final DataDocument result = new DataDocument();

      data.forEach((key, value) -> result.append(translateAttributeName(attributeNames, key), value));

      return result;
   }

   private static String translateAttributeName(final Map<String, String> dictionary, final String key) {
      if (StringUtils.isNotEmpty(key)) {
         if (key.startsWith(CHANGED_PREFIX)) {
            final String keySuffix = key.substring(CHANGED_PREFIX.length());
            return CHANGED_PREFIX + dictionary.getOrDefault(keySuffix, keySuffix);
         }
         if (key.startsWith(PREVIOUS_VALUE_PREFIX)) {
            final String keySuffix = key.substring(PREVIOUS_VALUE_PREFIX.length());
            return PREVIOUS_VALUE_PREFIX + dictionary.getOrDefault(keySuffix, keySuffix);
         }

         return dictionary.getOrDefault(key, key);
      }

      return key;
   }
}
