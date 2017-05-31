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
import io.lumeer.engine.api.dto.SearchSuggestion;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.ViewFacade;
import io.lumeer.engine.rest.dao.Attribute;
import io.lumeer.engine.rest.dao.CollectionMetadata;

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
         return suggestCollectionsForAttribute(text, limit); // TODO look for attributes of all collections instead
      }

      String collectionName = parts[0];
      String attributePart = parts[1];

      try {
         CollectionMetadata collectionMetadata = collectionMetadataFacade.getCollectionMetadata(collectionMetadataFacade.getInternalCollectionName(collectionName));
         return collectionMetadata.getAttributes().values().stream()
                                  .filter(attribute -> attribute.getName().toLowerCase().contains(attributePart.toLowerCase()))
                                  .map(attribute -> convertAttribute(collectionName, attribute))
                                  .collect(Collectors.toList()); // TODO do not ignore child attributes
      } catch (UserCollectionNotFoundException e) {
         return Collections.emptyList();
      }
   }

   private SearchSuggestion convertAttribute(String collectionName, Attribute attribute) {
      String text = collectionName + "." + attribute.getName();
      List<String> constraints = attribute.getConstraints();
      String icon = "";
      return new SearchSuggestion(SearchSuggestion.TYPE_ATTRIBUTE, text, constraints, icon);
   }

   private List<SearchSuggestion> suggestCollectionsForAttribute(String text, int limit) {
      return suggestCollections(text, limit)
            .stream()
            .map(s -> new SearchSuggestion(SearchSuggestion.TYPE_ATTRIBUTE, s.getText(), s.getIcon()))
            .collect(Collectors.toList());
   }

   List<SearchSuggestion> suggestCollections(String text, int limit) {
      String metadataCollection = collectionMetadataFacade.metadataCollection();
      DataFilter filter = storageDialect.fieldValueWildcardFilter(LumeerConst.Collection.REAL_NAME_KEY, text);
      List<String> projection = Arrays.asList(LumeerConst.Collection.REAL_NAME_KEY);

      return userDataStorage.search(metadataCollection, filter, null, projection, 0, limit)
                            .stream()
                            .map(QuerySuggester::convertCollection)
                            .collect(Collectors.toList());
   }

   private static SearchSuggestion convertCollection(DataDocument collection) {
      String name = collection.getString(LumeerConst.Collection.REAL_NAME_KEY);
      String icon = "";  // TODO store paths to icons in the database or somewhere else
      return new SearchSuggestion(SearchSuggestion.TYPE_COLLECTION, name, icon);
   }

   List<SearchSuggestion> suggestLinks(String text, int limit) {
      return Collections.emptyList(); // TODO needs to be discussed how to search links
   }

   List<SearchSuggestion> suggestViews(String text, int limit) {
      String metadataCollection = viewFacade.metadataCollection();
      DataFilter filter = storageDialect.fieldValueWildcardFilter(LumeerConst.View.NAME_KEY, text);

      return userDataStorage.search(metadataCollection, filter, null, Arrays.asList(LumeerConst.View.NAME_KEY), 0, limit)
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
