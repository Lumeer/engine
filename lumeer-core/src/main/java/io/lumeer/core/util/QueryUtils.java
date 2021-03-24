package io.lumeer.core.util;

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryUtils {

   private QueryUtils() {
   }

   public static Set<String> getViewsCollectionIds(java.util.Collection<View> views, java.util.Collection<LinkType> linkTypes) {
      List<Query> queries = views.stream().map(View::getQuery).collect(Collectors.toList());
      return queries.stream().map(query -> QueryUtils.getQueriesCollectionIds(queries, linkTypes))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
   }

   public static Set<String> getQueriesCollectionIds(java.util.Collection<Query> queries, java.util.Collection<LinkType> linkTypes) {
      return queries.stream().map(query -> QueryUtils.getQueryCollectionIds(query, linkTypes))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
   }

   public static Set<String> getQueryCollectionIds(Query query, java.util.Collection<LinkType> linkTypes) {
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
