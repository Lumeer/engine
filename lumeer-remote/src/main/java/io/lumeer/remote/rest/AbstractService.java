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
package io.lumeer.remote.rest;

import io.lumeer.core.WorkspaceKeeper;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

abstract class AbstractService {

   @Inject
   private HttpServletRequest request;

   @Inject
   protected WorkspaceKeeper workspaceKeeper;

   protected URI getResourceUri(String additionalPath) {
      return UriBuilder.fromUri(request.getRequestURL() + "/" + additionalPath).build();
   }

   protected URI getParentUri(String... urlEnd) {
      String fullPath = request.getRequestURL().toString();
      String regex = "\\/" + Arrays.stream(urlEnd).collect(Collectors.joining("\\/")) + "\\/?$";
      String parentPath = fullPath.replaceFirst(regex, "");
      return UriBuilder.fromUri(parentPath).build();
   }
}
