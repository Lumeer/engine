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
package io.lumeer.core.auth;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */

@Provider
public class CorsFilter implements ContainerResponseFilter {

   @Override
   public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws IOException {
      responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", requestContext.getHeaders().getFirst("Origin"));
      responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH");
      responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
      String reqHeader = requestContext.getHeaderString("Access-Control-Request-Headers");
      if (reqHeader != null && reqHeader != "") {
         responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", reqHeader);
      }
   }
}