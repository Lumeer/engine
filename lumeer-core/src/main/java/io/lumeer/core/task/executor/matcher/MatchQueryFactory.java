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
import io.lumeer.api.model.Query;
import io.lumeer.api.util.AttributeUtil;

import java.util.function.Function;

public class MatchQueryFactory {

   private MatchQueryFactory() {}

   public static Function<Object, Query> getMatchQuery(final Attribute thisAttribute, final Collection thatCollection, final Attribute thatAttribute) {
      final boolean thisMulti = AttributeUtil.isMultiselect(thisAttribute);
      final boolean thatMulti = AttributeUtil.isMultiselect(thatAttribute);
      MatchQueryProvider queryProvider;

      if (!thisMulti) {
         if (!thatMulti) {
            queryProvider = new SimpleToSimpleMatch();
         } else {
            queryProvider = new SimpleToMultiselectMatch();
         }
      } else {
         if (!thatMulti) {
            queryProvider = new MultiselectToSimpleMatch();
         } else {
            queryProvider = new MultiselectToMultiselectMatch();
         }
      }

      return (value) -> queryProvider.getMatchQuery(thatCollection, thatAttribute, value);
   }
}
