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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MongoUser;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class OrganizationServiceIntegrationTest extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Set<Role> USER_ROLES = Organization.ROLES;

   private static final Permission USER_PERMISSION = new SimplePermission(USER, USER_ROLES);

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String ORGANIZATION_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations";
   private static final String ORGANIZATION_URL = SERVER_URL + ORGANIZATION_PATH;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private UserDao userDao;

   private Client client;

   @Before
   public void createClient() {
      client = ClientBuilder.newBuilder().build();
   }

   @After
   public void closeClient() {
      if (client != null) {
         client.close();
      }
   }

   @Before
   public void init() {
      MongoUser user = new MongoUser();
      user.setUsername(USER);
      userDao.createUser(user);
   }


   @Test
   public void testGetOrganizations() {
      createOrganization(CODE1);
      createOrganization(CODE2);

      Response response = client.target(ORGANIZATION_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonOrganization> organizations = response.readEntity(new GenericType<List<JsonOrganization>>() {});
      assertThat(organizations).extracting(Resource::getCode).containsOnly(CODE1, CODE2);

      Permissions permissions1 = organizations.get(0).getPermissions();
      assertThat(permissions1).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(USER_PERMISSION));
      assertThat(permissions1).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions1).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());

      Permissions permissions2 = organizations.get(1).getPermissions();
      assertThat(permissions2).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(USER_PERMISSION));
      assertThat(permissions2).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions2).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());
   }

   private void createOrganization(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR);

      organizationFacade.createOrganization(organization);
   }

   @Test
   public void testGetOrganization() {
      createOrganization(CODE1);

      Response response = client.target(ORGANIZATION_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(JsonOrganization.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(returnedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedOrganization.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testDeleteOrganization() {
      createOrganization(CODE1);

      Response response = client.target(ORGANIZATION_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(ORGANIZATION_URL).build());

      assertThatThrownBy(() -> organizationFacade.getOrganization(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = new JsonOrganization(CODE1, NAME, ICON, COLOR);
      Entity entity = Entity.json(organization);

      Response response = client.target(ORGANIZATION_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);
      assertThat(response.getLocation().getPath()).isEqualTo(ORGANIZATION_PATH + "/" + CODE1);

      Organization storedOrganization = organizationFacade.getOrganization(CODE1);
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testEditOrganization() {
      createOrganization(CODE1);

      Organization updatedOrganization = new JsonOrganization(CODE2, NAME, ICON, COLOR);
      Entity entity = Entity.json(updatedOrganization);

      Response response = client.target(ORGANIZATION_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Organization returnedOrganization = response.readEntity(JsonOrganization.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedOrganization.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedOrganization.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      Organization storedOrganization = organizationFacade.getOrganization(CODE2);
      assertThat(storedOrganization).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }
}
