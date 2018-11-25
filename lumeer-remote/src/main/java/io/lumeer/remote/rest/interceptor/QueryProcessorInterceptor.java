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
package io.lumeer.remote.rest.interceptor;

import io.lumeer.api.model.AttributeFilter;
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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Interceptor
@QueryProcessor
public class QueryProcessorInterceptor {

   private static final String USER_EMAIL = "userEmail()";

   @Inject
   private AuthenticatedUser authenticatedUser;

   private void processQuery(final Query query) {
      for (QueryStem stem : query.getStems()) {
         final List<AttributeFilter> newFilters = stem.getFilters().stream().map(this::processFilter).collect(Collectors.toList());
         stem.getFilters().clear();
         stem.getFilters().addAll(newFilters);
      }
   }

   private AttributeFilter processFilter(final AttributeFilter filter) {
      if (filter.getValue().equals(USER_EMAIL)) {
         String userEmail = authenticatedUser.getUserEmail();
         return new AttributeFilter(filter.getCollectionId(), filter.getAttributeId(), filter.getOperator(), userEmail);
      }
      return filter;
   }

   @AroundInvoke
   public Object processQueries(InvocationContext context) throws Exception {
      Object[] params = context.getParameters();

      for (final Object param : params) {
         if (param instanceof Query) {
            processQuery((Query) param);
         }
      }

      return context.proceed();
   }
}
