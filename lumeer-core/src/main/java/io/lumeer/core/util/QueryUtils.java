package io.lumeer.core.util;

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryUtils {

   private QueryUtils() {
   }

   public static Set<String> getQueryCollectionIds(Query query, List<LinkType> linkTypes) {
      Set<String> collectionIds = new HashSet<>(query.getCollectionIds());
      Set<String> linkTypeIds = query.getLinkTypeIds();
      Set<String> collectionIdsInLinks = linkTypes.stream().filter(linkType -> linkTypeIds.contains(linkType.getId()))
                                                  .map(LinkType::getCollectionIds)
                                                  .flatMap(Collection::stream)
                                                  .collect(Collectors.toSet());
      collectionIds.addAll(collectionIdsInLinks);
      return collectionIds;
   }
}
