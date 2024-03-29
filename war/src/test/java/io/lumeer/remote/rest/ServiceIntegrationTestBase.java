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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.IntegrationTestBase;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

public abstract class ServiceIntegrationTestBase extends IntegrationTestBase {

   private static final String SERVER_URL = "http://localhost:8080";

   protected Client client;

   @BeforeEach
   public void createClient() {
      client = ClientBuilder.newBuilder().register(new JacksonJsonProvider(Utils.createJacksonObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS)).build();
   }

   @AfterEach
   public void closeClient() {
      if (client != null) {
         client.close();
      }
   }

   protected String basePath(){
      return SERVER_URL + "/" + PATH_CONTEXT + "/rest/";
   }

   protected String organizationPath(Organization organization) {
      return basePath() + "organizations/" + organization.getId() + "/";
   }

   protected String projectPath(Organization organization, Project project) {
      return organizationPath(organization) + "projects/" + project.getId() + "/";
   }

   protected String zapierPath() {
      return basePath() + "zapier/";
   }

   protected String getZapierCollectionHash(Organization organization, Project project, Collection collection) {
      return organization.getId() + "/" + project.getId() + "/" + collection.getId();
   }
}
