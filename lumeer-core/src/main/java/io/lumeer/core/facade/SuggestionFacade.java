/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Suggestions;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.SuggestionType;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
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

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public Suggestions suggest(String text, SuggestionType type) {
      switch (type) {
         case ALL:
            return suggestAll(text, SUGGESTIONS_LIMIT);
         case ATTRIBUTE:
            List<Collection> attributes = suggestAttributes(text, SUGGESTIONS_LIMIT);
            return Suggestions.attributeSuggestions(attributes);
         case COLLECTION:
            List<Collection> collections = suggestCollections(text, SUGGESTIONS_LIMIT);
            return Suggestions.collectionSuggestions(collections);
         case LINK:
            List<LinkType> linkTypes = suggestLinkTypes(text, SUGGESTIONS_LIMIT);
            return Suggestions.linkSuggestions(linkTypes);
         case VIEW:
            List<View> views = suggestViews(text, SUGGESTIONS_LIMIT);
            return Suggestions.viewSuggestions(views);
         default:
            throw new IllegalArgumentException("Unknown suggestion type: " + type);
      }
   }

   private Suggestions suggestAll(String text, int limit) {
      List<Collection> attributes = suggestAttributes(text, limit / TYPES_COUNT);
      List<Collection> collections = suggestCollections(text, limit / TYPES_COUNT);
      List<View> views = suggestViews(text, limit / TYPES_COUNT);
      List<LinkType> linkTypes = suggestLinkTypes(text, limit / TYPES_COUNT);

      return new Suggestions(attributes, collections, views, linkTypes);
   }

   private List<Collection> suggestAttributes(String text, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(text, limit);
      List<Collection> collections = collectionDao.getCollectionsByAttributes(suggestionQuery);
      return keepOnlyMatchingAttributes(collections, text);
   }

   private static List<Collection> keepOnlyMatchingAttributes(List<Collection> collections, String text) {
      for (Collection collection : collections) {
         Set<Attribute> attributes = collection.getAttributes().stream()
                                               .filter(a -> a.getName().toLowerCase().contains(text))
                                               .collect(Collectors.toSet());
         collection.setAttributes(attributes);
      }
      return collections;
   }

   private List<Collection> suggestCollections(String text, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(text, limit);
      List<Collection> collections = collectionDao.getCollections(suggestionQuery);

      collections.forEach(c -> c.setAttributes(Collections.emptySet()));
      return collections;
   }

   private List<LinkType> suggestLinkTypes(String text, int limit) {
      List<Collection> allowedCollections = collectionDao.getCollections(createSimpleQuery());
      if (allowedCollections.isEmpty()) {
         return Collections.emptyList();
      }

      List<String> allowedCollectionIds = allowedCollections.stream().map(Collection::getId).collect(Collectors.toList());
      SuggestionQuery suggestionQuery = createSuggestionQueryWithIds(text, limit, allowedCollectionIds);
      List<LinkType> linkTypes = linkTypeDao.getLinkTypes(suggestionQuery);
      return linkTypes.stream()
                      .filter(linkType -> allowedCollectionIds.containsAll(linkType.getCollectionIds()))
                      .collect(Collectors.toList());
   }

   private List<View> suggestViews(String text, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(text, limit);
      return viewDao.getViews(suggestionQuery);
   }

   private SuggestionQuery createSuggestionQuery(String text, int limit) {
      return createBuilderForSuggestionQuery(text, limit)
            .build();
   }

   private SuggestionQuery createSuggestionQueryWithIds(String text, int limit, List<String> collectionIds) {
      return createBuilderForSuggestionQuery(text, limit)
            .collectionIds(collectionIds)
            .build();
   }

   private SuggestionQuery.Builder createBuilderForSuggestionQuery(String text, int limit) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return SuggestionQuery.createBuilder(user).groups(groups)
                            .text(text)
                            .page(0).pageSize(limit);
   }

}
