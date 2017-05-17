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
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.ViewFacade;
import io.lumeer.engine.rest.dao.Attribute;
import io.lumeer.engine.rest.dao.CollectionMetadata;

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

   private static final int SUGGESTIONS_LIMIT = 10;

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
   public DataDocument suggest(String text, SuggestionsType type) {
      switch (type) {
         case ATTRIBUTE:
            return new DataDocument(SuggestionsDocument.ATTRIBUTES, suggestAttributes(text));
         case COLLECTION:
            return new DataDocument(SuggestionsDocument.COLLECTIONS, suggestCollections(text));
         case LINK:
            return new DataDocument(SuggestionsDocument.LINKS, suggestLinkTypes(text));
         case VIEW:
            return new DataDocument(SuggestionsDocument.VIEWS, suggestViews(text));
         default:
            return suggestAll(text);
      }
   }

   DataDocument suggestAll(String text) {
      return new DataDocument()
            .append(SuggestionsDocument.ATTRIBUTES, suggestAttributes(text))
            .append(SuggestionsDocument.COLLECTIONS, suggestCollections(text))
            .append(SuggestionsDocument.LINKS, suggestLinkTypes(text))
            .append(SuggestionsDocument.VIEWS, suggestViews(text));
   }

   List<DataDocument> suggestCollections(String text) {
      String metadataCollection = collectionMetadataFacade.metadataCollection();
      DataFilter filter = storageDialect.fieldValueWildcardFilter(LumeerConst.Collection.REAL_NAME_KEY, text);
      List<String> projection = Arrays.asList(LumeerConst.Collection.REAL_NAME_KEY);

      return userDataStorage.search(metadataCollection, filter, null, projection, 0, SUGGESTIONS_LIMIT)
                            .stream()
                            .map(QuerySuggester::convertCollection)
                            .collect(Collectors.toList());
   }

   private static DataDocument convertCollection(DataDocument collection) {
      return new DataDocument()
            .append(SuggestionsDocument.COLLECTIONS_NAME, collection.getString(LumeerConst.Collection.REAL_NAME_KEY))
            .append(SuggestionsDocument.COLLECTIONS_ICON, ""); // TODO store paths to icons in the database or somewhere else
   }

   List<DataDocument> suggestAttributes(String text) {
      String[] parts = text.split("\\.", 2);
      if (parts.length < 2) {
         return suggestCollectionsForAttribute(text); // TODO look for attributes of all collections instead
      }

      String collectionName = parts[0];
      String attributePart = parts[1];

      try {
         CollectionMetadata collectionMetadata = collectionMetadataFacade.getCollectionMetadata(collectionMetadataFacade.getInternalCollectionName(collectionName));
         return collectionMetadata.getAttributes().values().stream()
                                  .filter(a -> a.getName().toLowerCase().contains(attributePart.toLowerCase()))
                                  .map(a -> convertAttribute(collectionName, a))
                                  .collect(Collectors.toList()); // TODO do not ignore child attributes
      } catch (UserCollectionNotFoundException e) {
         return Collections.emptyList();
      }
   }

   private DataDocument convertAttribute(String collectionName, Attribute attribute) {
      return new DataDocument()
            .append(SuggestionsDocument.ATTRIBUTES_COLLECTION, collectionName)
            .append(SuggestionsDocument.ATTRIBUTES_NAME, attribute.getName())
            .append(SuggestionsDocument.ATTRIBUTES_CONSTRAINTS, attribute.getConstraints());
   }

   private List<DataDocument> suggestCollectionsForAttribute(String text) {
      return suggestCollections(text)
            .stream()
            .map(c -> new DataDocument(SuggestionsDocument.ATTRIBUTES_COLLECTION, c.get(SuggestionsDocument.COLLECTIONS_NAME)))
            .collect(Collectors.toList());
   }

   List<DataDocument> suggestLinkTypes(String text) {
      String linkTypesCollection = LumeerConst.Linking.Type.NAME;
      DataFilter filter = storageDialect.combineFilters(
            storageDialect.fieldValueFilter(LumeerConst.Linking.Type.ATTR_PROJECT, projectFacade.getCurrentProjectId()),
            storageDialect.fieldValueWildcardFilter(LumeerConst.Linking.Type.ATTR_ROLE, text)
      );
      List<String> attributes = Arrays.asList(LumeerConst.Linking.Type.ATTR_ROLE, LumeerConst.Linking.Type.ATTR_FROM_COLLECTION, LumeerConst.Linking.Type.ATTR_TO_COLLECTION);

      return userDataStorage.search(linkTypesCollection, filter, null, attributes, 0, SUGGESTIONS_LIMIT)
                            .stream()
                            .map(QuerySuggester::convertLinkType)
                            .collect(Collectors.toList());
   }

   private static DataDocument convertLinkType(DataDocument linkType) {
      return new DataDocument()
            .append(SuggestionsDocument.LINKS_FROM, linkType.getString(LumeerConst.Linking.Type.ATTR_FROM_COLLECTION))
            .append(SuggestionsDocument.LINKS_TO, linkType.getString(LumeerConst.Linking.Type.ATTR_TO_COLLECTION))
            .append(SuggestionsDocument.LINKS_ROLE, linkType.getString(LumeerConst.Linking.Type.ATTR_ROLE));
   }

   List<DataDocument> suggestViews(String text) {
      String metadataCollection = viewFacade.metadataCollection();
      DataFilter filter = storageDialect.fieldValueWildcardFilter(LumeerConst.View.NAME_KEY, text);

      return userDataStorage.search(metadataCollection, filter, null, Arrays.asList(LumeerConst.View.NAME_KEY), 0, SUGGESTIONS_LIMIT)
                            .stream()
                            .map(QuerySuggester::convertView)
                            .collect(Collectors.toList());
   }

   private static DataDocument convertView(DataDocument view) {
      return new DataDocument()
            .append(SuggestionsDocument.VIEWS_NAME, view.getString(LumeerConst.View.NAME_KEY))
            .append(SuggestionsDocument.VIEWS_ICON, ""); // TODO store paths to icons in the database or somewhere else
   }

}
