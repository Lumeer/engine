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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.SuggestionQuery;
import io.lumeer.api.model.Suggestions;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.SearchSuggestionQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class SuggestionFacade extends AbstractFacade {

   private static final int SUGGESTIONS_LIMIT = 15;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public Suggestions suggest(SuggestionQuery suggestionQuery) {
      switch (suggestionQuery.getType()) {
         case ALL:
            return suggestAll(suggestionQuery);
         case ATTRIBUTE:
            List<Collection> attributes = suggestAttributes(suggestionQuery, SUGGESTIONS_LIMIT);
            return Suggestions.attributeSuggestions(attributes);
         case COLLECTION:
            List<Collection> collections = suggestCollections(suggestionQuery, SUGGESTIONS_LIMIT);
            return Suggestions.collectionSuggestions(collections);
         case LINK:
            List<LinkType> linkTypes = suggestLinkTypes(suggestionQuery, SUGGESTIONS_LIMIT);
            return Suggestions.linkSuggestions(linkTypes);
         case LINK_ATTRIBUTE:
            List<LinkType> linkAttributes = suggestLinkAttributes(suggestionQuery, SUGGESTIONS_LIMIT);
            return Suggestions.linkAttributesSuggestions(linkAttributes);
         case VIEW:
            List<View> views = suggestViews(suggestionQuery, SUGGESTIONS_LIMIT);
            return Suggestions.viewSuggestions(views);
         default:
            throw new IllegalArgumentException("Unknown suggestion type: " + suggestionQuery.getType());
      }
   }

   private Suggestions suggestAll(SuggestionQuery suggestionQuery) {
      int currentLimit = SUGGESTIONS_LIMIT;
      int typesCount = 5;

      List<View> views = shouldSuggestViews(suggestionQuery) ? suggestViews(suggestionQuery, currentLimit / typesCount) : Collections.emptyList();
      currentLimit -= views.size();
      typesCount--;

      Set<String> allowedCollectionsIds = getAllowedCollectionIds();

      List<LinkType> linkTypes = suggestLinkTypes(suggestionQuery, currentLimit / typesCount, allowedCollectionsIds);
      currentLimit -= linkTypes.size();
      typesCount--;

      List<Collection> collections = suggestCollections(suggestionQuery, currentLimit / typesCount);
      currentLimit -= collections.size();
      typesCount--;

      List<Collection> attributes = suggestAttributes(suggestionQuery, currentLimit / typesCount);
      currentLimit -= collections.size();
      typesCount--;

      List<LinkType> linkAttributes = suggestLinkAttributes(suggestionQuery, currentLimit / typesCount, allowedCollectionsIds);

      return new Suggestions(attributes, collections, views, linkTypes, linkAttributes);
   }

   private boolean shouldSuggestViews(SuggestionQuery suggestionQuery) {
      return suggestionQuery.getPriorityCollectionIds().isEmpty();
   }

   private List<View> suggestViews(SuggestionQuery suggestionQuery, int limit) {
      SearchSuggestionQuery searchSuggestionQuery = createSuggestionQuery(suggestionQuery, limit);
      return viewDao.getViews(searchSuggestionQuery, isManager());
   }

   private List<LinkType> suggestLinkTypes(SuggestionQuery suggestionQuery, int limit) {
      Set<String> allowedCollectionIds = getAllowedCollectionIds();
      return suggestLinkTypes(suggestionQuery, limit, allowedCollectionIds);
   }

   private List<LinkType> suggestLinkTypes(SuggestionQuery suggestionQuery, int limit, Set<String> allowedCollectionIds) {
      if (allowedCollectionIds.isEmpty()) {
         return Collections.emptyList();
      }

      SearchSuggestionQuery searchSuggestionQuery = createSuggestionQueryWithIds(suggestionQuery, limit, allowedCollectionIds);
      return linkTypeDao.getLinkTypes(searchSuggestionQuery).stream()
                        .peek(linkTypes -> linkTypes.setAttributes(Collections.emptyList())).collect(Collectors.toList());
   }

   private List<LinkType> suggestLinkAttributes(SuggestionQuery suggestionQuery, int limit) {
      Set<String> allowedCollectionIds = getAllowedCollectionIds();
      return suggestLinkAttributes(suggestionQuery, limit, allowedCollectionIds);
   }

   private List<LinkType> suggestLinkAttributes(SuggestionQuery suggestionQuery, int limit, Set<String> allowedCollectionIds) {
      if (allowedCollectionIds.isEmpty()) {
         return Collections.emptyList();
      }

      SearchSuggestionQuery searchSuggestionQuery = createSuggestionQueryWithIds(suggestionQuery, limit, allowedCollectionIds);
      return keepOnlyMatchingLinkAttributes(linkTypeDao.getLinkTypesByAttributes(searchSuggestionQuery), suggestionQuery.getText(), limit);
   }

   private static List<LinkType> keepOnlyMatchingLinkAttributes(List<LinkType> linkTypes, String text, int attributesLimit) {
      return linkTypes.stream()
                      .peek(linkType -> linkType.setAttributes(new ArrayList<>(filterAttributes(linkType.getAttributes(), text, attributesLimit))))
                      .collect(Collectors.toList());
   }

   private Set<String> getAllowedCollectionIds() {
      if (isManager()) {
         return collectionDao.getAllCollections().stream().map(Collection::getId).collect(Collectors.toSet());
      }
      return collectionDao.getCollections(createSimpleQuery()).stream().map(Collection::getId).collect(Collectors.toSet());
   }

   private List<Collection> suggestCollections(SuggestionQuery suggestionQuery, int limit) {
      SearchSuggestionQuery searchSuggestionQuery = createSuggestionQuery(suggestionQuery, limit);
      return collectionDao.getCollections(searchSuggestionQuery, isManager()).stream()
                          .peek(collection -> collection.setAttributes(Collections.emptySet())).collect(Collectors.toList());
   }

   private List<Collection> suggestAttributes(SuggestionQuery suggestionQuery, int limit) {
      SearchSuggestionQuery searchSuggestionQuery = createSuggestionQuery(suggestionQuery, limit);
      List<Collection> collections = collectionDao.getCollectionsByAttributes(searchSuggestionQuery, isManager());
      if (collections.isEmpty()) {
         return Collections.emptyList();
      }

      int attributesLimit = (limit / collections.size()) + 1;
      return keepOnlyMatchingAttributes(collections, suggestionQuery.getText(), attributesLimit);
   }

   private static List<Collection> keepOnlyMatchingAttributes(List<Collection> collections, String text, int attributesLimit) {
      return collections.stream()
                        .peek(collection -> collection.setAttributes(new HashSet<>(filterAttributes(collection.getAttributes(), text, attributesLimit))))
                        .collect(Collectors.toList());
   }

   private static java.util.Collection<Attribute> filterAttributes(java.util.Collection<Attribute> attributes, String text, int limit) {
      List<Attribute> filteredAttributes = attributes.stream()
                                                     .filter(a -> a.getName().toLowerCase().contains(text))
                                                     .collect(Collectors.toList());
      return filteredAttributes.subList(0, Math.min(filteredAttributes.size(), limit));
   }

   private SearchSuggestionQuery createSuggestionQuery(SuggestionQuery suggestionQuery, int limit) {
      return createBuilderForSuggestionQuery(suggestionQuery, limit)
            .build();
   }

   private SearchSuggestionQuery createSuggestionQueryWithIds(SuggestionQuery suggestionQuery, int limit, Set<String> collectionIds) {
      return createBuilderForSuggestionQuery(suggestionQuery, limit)
            .collectionIds(collectionIds)
            .build();
   }

   private SearchSuggestionQuery.Builder createBuilderForSuggestionQuery(SuggestionQuery suggestionQuery, int limit) {
      return SearchSuggestionQuery.createBuilder(authenticatedUser.getCurrentUserId())
                                  .groups(authenticatedUserGroups.getCurrentUserGroups())
                                  .text(suggestionQuery.getText())
                                  .priorityCollectionIds(suggestionQuery.getPriorityCollectionIds())
                                  .page(0).pageSize(limit);
   }

}
