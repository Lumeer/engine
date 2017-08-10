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

import io.lumeer.api.model.Resource;

import java.net.URI;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

public abstract class AbstractService {

   @Inject
   private HttpServletRequest request;

   protected URI getResourceURI(Resource resource) {
      return UriBuilder.fromUri(request.getRequestURL() + "/" + resource.getCode()).build();
   }

   protected URI getParentURI() {
      String fullPath = request.getRequestURL().toString();
      String parentPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
      return UriBuilder.fromUri(parentPath).build();
   }

   protected URI getGrandParentURI() {
      String fullPath = request.getRequestURL().toString();
      String parentPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
      String grandParentPath = parentPath.substring(0, parentPath.lastIndexOf("/"));
      return UriBuilder.fromUri(grandParentPath).build();
   }
}
