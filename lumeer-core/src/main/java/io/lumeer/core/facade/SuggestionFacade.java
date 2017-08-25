/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.core.facade;

import io.lumeer.api.dto.JsonSuggestions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.SuggestionType;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.SuggestionQuery;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class SuggestionFacade extends AbstractFacade {

   private static final int SUGGESTIONS_LIMIT = 12;
   private static final int TYPES_COUNT = 4;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private ViewDao viewDao;

   public JsonSuggestions suggest(String text, SuggestionType type) {
      switch (type) {
         case ALL:
            return suggestAll(text, SUGGESTIONS_LIMIT);
         case ATTRIBUTE:
            List<Collection> attributes = suggestAttributes(text, SUGGESTIONS_LIMIT);
            return JsonSuggestions.attributeSuggestions(attributes);
         case COLLECTION:
            List<Collection> collections = suggestCollections(text, SUGGESTIONS_LIMIT);
            return JsonSuggestions.collectionSuggestions(collections);
         case LINK:
            return JsonSuggestions.emptySuggestions(); // TODO implement links search
         case VIEW:
            List<View> views = suggestViews(text, SUGGESTIONS_LIMIT);
            return JsonSuggestions.viewSuggestions(views);
         default:
            throw new IllegalArgumentException("Unknown suggestion type: " + type);
      }
   }

   private JsonSuggestions suggestAll(String text, int limit) {
      List<Collection> attributes = suggestAttributes(text, limit / TYPES_COUNT);
      List<Collection> collections = suggestCollections(text, limit / TYPES_COUNT);
      List<View> views = suggestViews(text, limit / TYPES_COUNT);

      return new JsonSuggestions(attributes, collections, views);
   }

   private List<Collection> suggestAttributes(String text, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(text, limit);
      List<Collection> collections = collectionDao.getCollectionsByAttributes(suggestionQuery);
      return keepOnlyMatchingAttributes(collections, text);
   }

   private static List<Collection> keepOnlyMatchingAttributes(List<Collection> collections, String text) {
      for (Collection collection : collections) {
         List<Attribute> attributes = collection.getAttributes().stream()
                                                .filter(a -> a.getName().startsWith(text))
                                                .collect(Collectors.toList());
         collection.setAttributes(attributes);
      }
      return collections;
   }

   private List<Collection> suggestCollections(String text, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(text, limit);
      List<Collection> collections = collectionDao.getCollections(suggestionQuery);

      collections.forEach(c -> c.setAttributes(Collections.emptyList()));
      return collections;
   }

   private List<View> suggestViews(String text, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(text, limit);
      return viewDao.getViews(suggestionQuery);
   }

   private SuggestionQuery createSuggestionQuery(String text, int limit) {
      String user = authenticatedUser.getCurrentUsername();
      Set<String> groups = authenticatedUser.getCurrentUser().getGroups();

      return SuggestionQuery.createBuilder(user).groups(groups)
                            .text(text)
                            .page(0).pageSize(limit)
                            .build();
   }
}
