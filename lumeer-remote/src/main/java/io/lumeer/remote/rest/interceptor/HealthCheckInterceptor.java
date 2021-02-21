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

import io.lumeer.api.model.HealthChecking;
import io.lumeer.remote.rest.annotation.HealthCheck;

import java.util.Collection;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@HealthCheck
public class HealthCheckInterceptor {

   @AroundInvoke
   public Object processRequests(InvocationContext context) throws Exception {
      Object[] params = context.getParameters();

      for (final Object param : params) {
         if (param instanceof HealthChecking) {
            ((HealthChecking) param).checkHealth();
         } else if (param instanceof Collection) {
            ((Collection<?>) param).forEach(it -> {
               if (it instanceof HealthChecking) ((HealthChecking) it).checkHealth();
            });
         }
      }

      return context.proceed();
   }

}
