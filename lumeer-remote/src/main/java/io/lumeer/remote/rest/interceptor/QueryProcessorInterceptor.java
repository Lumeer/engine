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

import io.lumeer.api.dto.JsonQuery;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.remote.rest.annotation.QueryProcessor;

import java.util.Set;
import java.util.regex.Pattern;
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

   private String processFilter(final String filter) {
      final String[] parts = filter.split(":", 3);
      if (parts.length == 3 && parts[2].contains(USER_EMAIL)) {
         parts[2] = parts[2].replaceFirst(Pattern.quote(USER_EMAIL), authenticatedUser.getUserEmail());
         return String.join(":", parts);
      } else {
         return filter;
      }
   }

   private void processQuery(final JsonQuery query) {
      if (query.getFilters() != null && query.getFilters().size() > 0) {
         final Set<String> newFilters = query.getFilters().stream().map(this::processFilter).collect(Collectors.toSet());
         query.getFilters().clear();
         query.getFilters().addAll(newFilters);
      }
   }

   @AroundInvoke
   public Object processQueries(InvocationContext context) throws Exception {
      Object[] params = context.getParameters();

      for (final Object param: params) {
         if (param instanceof JsonQuery) {
            processQuery((JsonQuery) param);
         }
      }

      return context.proceed();
   }
}
