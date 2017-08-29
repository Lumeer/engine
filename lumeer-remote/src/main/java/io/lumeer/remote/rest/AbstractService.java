/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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
