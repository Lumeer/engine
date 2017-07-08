/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.controller.search;

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.SearchSuggestion;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.ViewFacade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Suggests how to complete the part of the query the user is typing
 */
@ApplicationScoped
public class QuerySuggester {

   static final int SUGGESTIONS_LIMIT = 12;

   @Inject
   @UserDataStorage
   private DataStorage userDataStorage;

   @Inject
   private DataStorageDialect storageDialect;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private ViewFacade viewFacade;

   /**
    * Suggests how to complete the part of the query the user is typing
    *
    * @param text
    *       part of the query the user is typing
    * @param type
    *       suggestion type from the request
    * @return a list of suggestions (collections, links, views, etc.)
    */
   public List<SearchSuggestion> suggest(String text, String type) {
      if (type == null) {
         type = SearchSuggestion.TYPE_ALL;
      }

      switch (type) {
         case SearchSuggestion.TYPE_ATTRIBUTE:
            return suggestAttributes(text, SUGGESTIONS_LIMIT);
         case SearchSuggestion.TYPE_COLLECTION:
            return suggestCollections(text, SUGGESTIONS_LIMIT);
         case SearchSuggestion.TYPE_LINK:
            return suggestLinks(text, SUGGESTIONS_LIMIT);
         case SearchSuggestion.TYPE_VIEW:
            return suggestViews(text, SUGGESTIONS_LIMIT);
         default:
            return suggestAll(text, SUGGESTIONS_LIMIT);
      }
   }

   List<SearchSuggestion> suggestAll(String text, int limit) {
      List<SearchSuggestion> suggestions = new ArrayList<>();
      suggestions.addAll(suggestAttributes(text, limit / 4));
      suggestions.addAll(suggestCollections(text, limit / 4));
      suggestions.addAll(suggestLinks(text, limit / 4));
      suggestions.addAll(suggestViews(text, limit / 4));
      return suggestions;
   }

   List<SearchSuggestion> suggestAttributes(String text, int limit) {
      String[] parts = text.split("\\.", 2);
      if (parts.length < 2) {
         return suggestAttributesForAllCollections(text, limit);
      }

      String collectionName = parts[0];
      String attributePart = parts[1];

      String attributeKey = storageDialect.concatFields(LumeerConst.Collection.ATTRIBUTES, LumeerConst.Collection.ATTRIBUTE_NAME);

      DataFilter collectionFilter = storageDialect.fieldValueFilter(LumeerConst.Collection.REAL_NAME, collectionName);
      DataFilter attributePartFilter = storageDialect.fieldValueWildcardFilter(attributeKey, attributePart);

      DataDocument metadata = userDataStorage.readDocumentIncludeAttrs(collectionMetadataFacade.metadataCollection(),
            storageDialect.combineFilters(collectionFilter, attributePartFilter), Collections.singletonList(LumeerConst.Collection.ATTRIBUTES));

      if (metadata == null) {
         return Collections.emptyList();
      }

      return metadata.getArrayList(LumeerConst.Collection.ATTRIBUTES, DataDocument.class).stream()
                     .filter(dataDocument -> dataDocument.getString(LumeerConst.Collection.ATTRIBUTE_NAME).toLowerCase().contains(attributePart.toLowerCase()))
                     .map(Attribute::new)
                     .map(attribute -> convertAttribute(collectionName, attribute))
                     .limit(limit)
                     .collect(Collectors.toList());
   }

   private List<SearchSuggestion> suggestAttributesForAllCollections(String attributePart, int limit) {
      String metadataCollection = collectionMetadataFacade.metadataCollection();
      String attributeKey = storageDialect.concatFields(LumeerConst.Collection.ATTRIBUTES, LumeerConst.Collection.ATTRIBUTE_NAME);
      DataFilter attributePartFilter = storageDialect.fieldValueWildcardFilter(attributeKey, attributePart);

      List<DataDocument> collectionsRaw = userDataStorage.search(metadataCollection, attributePartFilter,
            Arrays.asList(LumeerConst.Collection.REAL_NAME, LumeerConst.Collection.ATTRIBUTES));

      List<SearchSuggestion> suggestions = new ArrayList<>();
      for (DataDocument metadata : collectionsRaw) {
         if (suggestions.size() >= limit) {
            break;
         }
         String collectionName = metadata.getString(LumeerConst.Collection.REAL_NAME);
         List<SearchSuggestion> s = metadata.getArrayList(LumeerConst.Collection.ATTRIBUTES, DataDocument.class).stream()
                                            .filter(dataDocument -> dataDocument.getString(LumeerConst.Collection.ATTRIBUTE_NAME).toLowerCase().contains(attributePart.toLowerCase()))
                                            .map(Attribute::new)
                                            .map(attribute -> convertAttribute(collectionName, attribute))
                                            .limit(limit - suggestions.size())
                                            .collect(Collectors.toList());
         suggestions.addAll(s);
      }
      return suggestions;
   }

   private SearchSuggestion convertAttribute(String collectionName, Attribute attribute) {
      String text = collectionName + "." + attribute.getFullName();
      List<String> constraints = attribute.getConstraints();
      String icon = "";
      return new SearchSuggestion(SearchSuggestion.TYPE_ATTRIBUTE, text, constraints, icon);
   }

   List<SearchSuggestion> suggestCollections(String text, int limit) {
      String metadataCollection = collectionMetadataFacade.metadataCollection();
      DataFilter filter = storageDialect.fieldValueWildcardFilter(LumeerConst.Collection.REAL_NAME, text);
      List<String> projection = Collections.singletonList(LumeerConst.Collection.REAL_NAME);

      return userDataStorage.search(metadataCollection, filter, null, projection, 0, limit)
                            .stream()
                            .map(QuerySuggester::convertCollection)
                            .collect(Collectors.toList());
   }

   private static SearchSuggestion convertCollection(DataDocument collection) {
      String name = collection.getString(LumeerConst.Collection.REAL_NAME);
      String icon = "";  // TODO store paths to icons in the database or somewhere else
      return new SearchSuggestion(SearchSuggestion.TYPE_COLLECTION, name, icon);
   }

   List<SearchSuggestion> suggestLinks(String text, int limit) {
      return Collections.emptyList(); // TODO needs to be discussed how to search links
   }

   List<SearchSuggestion> suggestViews(String text, int limit) {
      String metadataCollection = viewFacade.metadataCollection();
      DataFilter filter = storageDialect.fieldValueWildcardFilter(LumeerConst.View.NAME_KEY, text);

      return userDataStorage.search(metadataCollection, filter, null, Collections.singletonList(LumeerConst.View.NAME_KEY), 0, limit)
                            .stream()
                            .map(QuerySuggester::convertView)
                            .collect(Collectors.toList());
   }

   private static SearchSuggestion convertView(DataDocument view) {
      String name = view.getString(LumeerConst.View.NAME_KEY);
      String icon = "";  // TODO store paths to icons in the database or somewhere else
      return new SearchSuggestion(SearchSuggestion.TYPE_VIEW, name, icon);
   }

}
