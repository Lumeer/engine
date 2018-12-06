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
import io.lumeer.api.model.Suggest;
import io.lumeer.api.model.Suggestions;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.SuggestionQuery;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class SuggestionFacade extends AbstractFacade {

   private static final int SUGGESTIONS_LIMIT = 12;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public Suggestions suggest(Suggest suggest) {
      switch (suggest.getType()) {
         case ALL:
            return suggestAll(suggest);
         case ATTRIBUTE:
            List<Collection> attributes = suggestAttributes(suggest, SUGGESTIONS_LIMIT);
            return Suggestions.attributeSuggestions(attributes);
         case COLLECTION:
            List<Collection> collections = suggestCollections(suggest, SUGGESTIONS_LIMIT);
            return Suggestions.collectionSuggestions(collections);
         case LINK:
            List<LinkType> linkTypes = suggestLinkTypes(suggest, SUGGESTIONS_LIMIT);
            return Suggestions.linkSuggestions(linkTypes);
         case VIEW:
            List<View> views = suggestViews(suggest, SUGGESTIONS_LIMIT);
            return Suggestions.viewSuggestions(views);
         default:
            throw new IllegalArgumentException("Unknown suggestion type: " + suggest.getType());
      }
   }

   private Suggestions suggestAll(Suggest suggest) {
      int currentLimit = SUGGESTIONS_LIMIT;
      int typesCount = 4;

      List<View> views = shouldSuggestViews(suggest) ? suggestViews(suggest, currentLimit / typesCount) : Collections.emptyList();
      currentLimit -= views.size();
      typesCount--;

      List<LinkType> linkTypes = suggestLinkTypes(suggest, currentLimit / typesCount);
      currentLimit -= linkTypes.size();
      typesCount--;

      List<Collection> collections = suggestCollections(suggest, currentLimit / typesCount);
      currentLimit -= collections.size();
      typesCount--;

      List<Collection> attributes = suggestAttributes(suggest, currentLimit / typesCount);

      return new Suggestions(attributes, collections, views, linkTypes);
   }

   private boolean shouldSuggestViews(Suggest suggest) {
      return suggest.getPriorityCollectionIds().isEmpty();
   }

   private List<View> suggestViews(Suggest suggest, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(suggest, limit);
      return viewDao.getViews(suggestionQuery);
   }

   private List<LinkType> suggestLinkTypes(Suggest suggest, int limit) {
      List<Collection> allowedCollections = collectionDao.getCollections(createSimpleQuery());
      if (allowedCollections.isEmpty()) {
         return Collections.emptyList();
      }

      Set<String> allowedCollectionIds = allowedCollections.stream().map(Collection::getId).collect(Collectors.toSet());
      SuggestionQuery suggestionQuery = createSuggestionQueryWithIds(suggest, limit, allowedCollectionIds);
      return linkTypeDao.getLinkTypes(suggestionQuery);
   }

   private List<Collection> suggestCollections(Suggest suggest, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(suggest, limit);
      return collectionDao.getCollections(suggestionQuery).stream()
                          .peek(collection -> collection.setAttributes(Collections.emptySet())).collect(Collectors.toList());
   }

   private List<Collection> suggestAttributes(Suggest suggest, int limit) {
      SuggestionQuery suggestionQuery = createSuggestionQuery(suggest, limit);
      List<Collection> collections = collectionDao.getCollectionsByAttributes(suggestionQuery);
      if (collections.isEmpty()) {
         return Collections.emptyList();
      }

      int attributesLimit = (limit / collections.size()) + 1;
      return keepOnlyMatchingAttributes(collections, suggest.getText(), attributesLimit);
   }

   private static List<Collection> keepOnlyMatchingAttributes(List<Collection> collections, String text, int attributesLimit) {
      return collections.stream()
                        .peek(collection -> collection.setAttributes(filterAttributes(collection.getAttributes(), text, attributesLimit)))
                        .collect(Collectors.toList());
   }

   private static Set<Attribute> filterAttributes(Set<Attribute> attributes, String text, int limit) {
      List<Attribute> filteredAttributes = attributes.stream()
                                                     .filter(a -> a.getName().toLowerCase().contains(text))
                                                     .collect(Collectors.toList());
      return new HashSet<>(filteredAttributes.subList(0, Math.min(filteredAttributes.size(), limit)));
   }

   private SuggestionQuery createSuggestionQuery(Suggest suggest, int limit) {
      return createBuilderForSuggestionQuery(suggest, limit)
            .build();
   }

   private SuggestionQuery createSuggestionQueryWithIds(Suggest suggest, int limit, Set<String> collectionIds) {
      return createBuilderForSuggestionQuery(suggest, limit)
            .collectionIds(collectionIds)
            .build();
   }

   private SuggestionQuery.Builder createBuilderForSuggestionQuery(Suggest suggest, int limit) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return SuggestionQuery.createBuilder(user).groups(groups)
                            .text(suggest.getText())
                            .priorityCollectionIds(suggest.getPriorityCollectionIds())
                            .page(0).pageSize(limit);
   }

}
