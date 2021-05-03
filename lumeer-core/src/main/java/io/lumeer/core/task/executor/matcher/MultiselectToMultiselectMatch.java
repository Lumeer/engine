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
package io.lumeer.core.task.executor.matcher;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.ConditionValue;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;

import java.util.Collections;
import java.util.List;

public class MultiselectToMultiselectMatch implements MatchQueryProvider {

   private final DocumentMatcher matcher;

   MultiselectToMultiselectMatch() {
      matcher = null;
   }

   public MultiselectToMultiselectMatch(final DocumentMatcher matcher) {
      this.matcher = matcher;
   }

   public Query getMatchQueryForRemoval(final Object oldValue, final Object newValue) {
      final List<Object> addedValues = getValues(newValue);
      addedValues.removeAll(getValues(oldValue));

      return new Query(
            Collections.singletonList(
                  new QueryStem(
                        null,
                        matcher.getThisCollection().getId(),
                        Collections.singletonList(matcher.getLinkType().getId()),
                        Collections.emptySet(),
                        List.of(
                              new CollectionAttributeFilter(
                                    matcher.getThatCollection().getId(),
                                    matcher.getThatAttribute().getId(),
                                    ConditionType.HAS_NONE_OF,
                                    List.of(new ConditionValue(addedValues))
                              )
                        ),
                        Collections.emptyList()
                  )
            )
      );
   }

   public Query getMatchQueryForCreation(final Object oldValue, final Object newValue) {
      return new Query(
            Collections.singletonList(
                  new QueryStem(
                        null,
                        matcher.getThatCollection().getId(),
                        Collections.emptyList(),
                        Collections.emptySet(),
                        List.of(
                              new CollectionAttributeFilter(
                                    matcher.getThatCollection().getId(),
                                    matcher.getThatAttribute().getId(),
                                    ConditionType.HAS_ALL,
                                    List.of(new ConditionValue(getValues(newValue)))
                              )
                        ),
                        Collections.emptyList()
                  )
            )
      );
   }

   public Query getMatchQuery(final Collection collection, final Attribute attribute, final Object newValue) {
      return new Query(
            Collections.singletonList(
                  new QueryStem(
                        null,
                        collection.getId(),
                        Collections.emptyList(),
                        Collections.emptySet(),
                        List.of(
                              new CollectionAttributeFilter(
                                    collection.getId(),
                                    attribute.getId(),
                                    ConditionType.HAS_ALL,
                                    List.of(new ConditionValue(getValues(newValue)))
                              )
                        ),
                        Collections.emptyList()
                  )
            )
      );
   }
}
