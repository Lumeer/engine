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

import io.lumeer.api.model.geocoding.GeoCodingResult;
import io.lumeer.core.cache.GeoCodingCache;
import io.lumeer.core.client.mapquest.MapQuestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GeoCodingFacade {

   @Inject
   private GeoCodingCache geoCodingCache;

   @Inject
   private MapQuestClient mapQuestClient;

   public List<GeoCodingResult> getResults(Set<String> queries) {
      var cachedResults = getCachedResults(queries);
      var unresolvedQueries = filterUnresolvedQueries(queries, cachedResults);

      List<GeoCodingResult> freshResults = unresolvedQueries.size() > 0 ? loadFreshResults(unresolvedQueries) : Collections.emptyList();

      return Stream.concat(cachedResults.values().stream(), freshResults.stream())
                   .collect(Collectors.toList());
   }

   private Map<String, GeoCodingResult> getCachedResults(Set<String> queries) {
      return queries.stream()
                    .filter(query -> query != null && !query.isEmpty())
                    .map(query -> geoCodingCache.getResult(query))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(GeoCodingResult::getQuery, result -> result));
   }

   private Set<String> filterUnresolvedQueries(Set<String> queries, Map<String, GeoCodingResult> cachedResults) {
      return queries.stream()
                    .filter(query -> cachedResults.get(query) == null)
                    .collect(Collectors.toSet());
   }

   private List<GeoCodingResult> loadFreshResults(Set<String> queries) {
      var results = mapQuestClient.batchGeoCode(queries);
      results.forEach(result -> geoCodingCache.updateResult(result.getQuery(), result));
      return results;
   }

}
