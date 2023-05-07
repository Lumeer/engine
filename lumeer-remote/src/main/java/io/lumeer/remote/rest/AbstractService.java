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
package io.lumeer.remote.rest;

import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.remote.rest.init.StartupFacade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private StartupFacade startupFacade;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   protected boolean isProduction() {
      return defaultConfigurationProducer.getEnvironment() == DefaultConfigurationProducer.DeployEnvironment.PRODUCTION;
   }

   protected URI getParentUri(String... urlEnd) {
      String fullPath = request.getRequestURL().toString();
      String regex = "\\/" + Arrays.stream(urlEnd).collect(Collectors.joining("\\/")) + "\\/?$";
      String parentPath = fullPath.replaceFirst(regex, "");
      return UriBuilder.fromUri(parentPath).build();
   }

   protected <T> T callProductionApi(String apiPath, TypeReference<T> responseType) throws IOException, InterruptedException {
      String apiUrl = "https://get.lumeer.io/lumeer-engine/rest/" + apiPath;
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
                                       .uri(URI.create(apiUrl))
                                       .GET()
                                       .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();

      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(responseBody, responseType);
   }
}
