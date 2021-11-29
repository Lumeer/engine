package io.lumeer.core.util;

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryUtils {

   private QueryUtils() {
   }

   public static Set<String> getViewsCollectionIds(java.util.Collection<View> views, java.util.Collection<LinkType> linkTypes) {
      return views.stream().map(view -> QueryUtils.getViewCollectionIds(view, linkTypes))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
   }

   public static Set<String> getViewCollectionIds(View view, java.util.Collection<LinkType> linkTypes) {
      Set<String> collectionIds = getQueryCollectionIds(view.getQuery(), linkTypes);
      collectionIds.addAll(getViewAdditionalCollectionIds(view, linkTypes));
      return collectionIds;
   }

   public static Set<String> getViewAdditionalCollectionIds(View view, java.util.Collection<LinkType> linkTypes) {
      Set<String> collectionIds = new HashSet<>();
      view.getAdditionalQueries().forEach(query -> collectionIds.addAll(getQueryCollectionIds(query, linkTypes)));
      Set<String> queryCollectionIds = getQueryCollectionIds(view.getQuery(), linkTypes);
      collectionIds.removeAll(queryCollectionIds);
      return collectionIds;
   }

   public static Set<String> getQueryCollectionIds(Query query, java.util.Collection<LinkType> linkTypes) {
      if (query == null) {
         return new HashSet<>();
      }
      Set<String> collectionIds = new HashSet<>(query.getCollectionIds());
      Set<String> linkTypeIds = query.getLinkTypeIds();
      Set<String> collectionIdsInLinks = linkTypes.stream().filter(linkType -> linkTypeIds.contains(linkType.getId()))
                                                  .map(LinkType::getCollectionIds)
                                                  .flatMap(Collection::stream)
                                                  .collect(Collectors.toSet());
      collectionIds.addAll(collectionIdsInLinks);
      return collectionIds;
   }

   public static String getOtherCollectionId(LinkType linkType, String collectionId) {
      if (linkType != null && collectionId != null) {
         var collectionIds = linkType.getCollectionIds().stream().filter(id -> !collectionId.equals(id)).findFirst();
         if (collectionIds.isPresent()) {
            return collectionIds.get();
         }
      }
      return null;
   }
}
