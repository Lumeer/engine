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
package io.lumeer.remote.rest.interceptor;

import static io.lumeer.api.model.ConditionValueType.*;

import io.lumeer.api.model.AttributeFilter;
import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.LinkAttributeFilter;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.remote.rest.annotation.QueryProcessor;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@QueryProcessor
public class QueryProcessorInterceptor {

   @Inject
   private AuthenticatedUser authenticatedUser;

   private void processQuery(final Query query) {
      query.getStems().forEach(this::processStem);
   }

   private void processStem(final QueryStem stem) {
      if (!stem.getFilters().isEmpty()) {
         final List<CollectionAttributeFilter> newFilters = stem.getFilters().stream().map(this::processFilter).collect(Collectors.toList());
         stem.getFilters().clear();
         stem.getFilters().addAll(newFilters);
      }
      if (!stem.getLinkFilters().isEmpty()) {
         final List<LinkAttributeFilter> newFilters = stem.getLinkFilters().stream().map(this::processFilter).collect(Collectors.toList());
         stem.getLinkFilters().clear();
         stem.getLinkFilters().addAll(newFilters);
      }
   }

   private CollectionAttributeFilter processFilter(final CollectionAttributeFilter filter) {
      var filterCopy = new CollectionAttributeFilter(filter);
      transformValue(filter);
      return filterCopy;
   }

   private LinkAttributeFilter processFilter(final LinkAttributeFilter filter) {
      var filterCopy = new LinkAttributeFilter(filter);
      transformValue(filter);
      return filterCopy;
   }

   private void transformValue(AttributeFilter filter) {
      if (filter.getValue() == null) {
         return;
      }
      var conditionValue = valueOf(filter.getValue().toString());
      switch (conditionValue) {
         case CURRENT_USER:
            filter.setValue(authenticatedUser.getUserEmail());
      }
   }

   @AroundInvoke
   public Object processQueries(InvocationContext context) throws Exception {
      Object[] params = context.getParameters();

      for (final Object param : params) {
         if (param instanceof Query) {
            processQuery((Query) param);
         } else if (param instanceof QueryStem) {
            processStem((QueryStem) param);
         } else if (param instanceof CollectionAttributeFilter) {
            processFilter((CollectionAttributeFilter) param);
         } else if (param instanceof LinkAttributeFilter) {
            processFilter((LinkAttributeFilter) param);
         }
      }

      return context.proceed();
   }
}
