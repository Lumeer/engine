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

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.core.model.SimpleUser;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class CollectionServiceIntegrationTest extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";

   private static final Set<Role> USER_ROLES = View.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(Role.READ);
   private static final Permission USER_PERMISSION = new SimplePermission(USER, USER_ROLES);
   private static final Permission GROUP_PERMISSION = new SimplePermission(GROUP, GROUP_ROLES);

   private static final String CODE2 = "TCOLL2";

   private static final String ATTRIBUTE_NAME = "name";
   private static final String ATTRIBUTE_FULLNAME = "fullname";
   private static final Set<String> ATTRIBUTE_CONSTRAINTS = Collections.emptySet();
   private static final Integer ATTRIBUTE_COUNT = 0;

   private static final String ATTRIBUTE_FULLNAME2 = "fullname";

   private static final JsonAttribute ATTRIBUTE = new JsonAttribute(ATTRIBUTE_NAME, ATTRIBUTE_FULLNAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String COLLECTIONS_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations/" + ORGANIZATION_CODE + "/projects/" + PROJECT_CODE + "/collections";
   private static final String COLLECTIONS_URL = SERVER_URL + COLLECTIONS_PATH;
   private static final String PERMISSIONS_URL = COLLECTIONS_URL + "/" + CODE + "/permissions";

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private CollectionDao collectionDao;

   @Before
   public void configureProject() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);
      userDao.setOrganization(storedOrganization);

      SimpleUser user = new SimpleUser(USER);
      userDao.createUser(user);

      JsonProject project = new JsonProject();
      project.setCode(PROJECT_CODE);

      JsonPermissions projectPermissions = new JsonPermissions();
      projectPermissions.updateUserPermissions(new JsonPermission(USER, Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(project);
   }

   private Collection prepareCollection(String code) {
      return new JsonCollection(code, NAME, ICON, COLOR, null);
   }

   private Collection createCollection(String code) {
      Collection collection = prepareCollection(code);
      collection.getPermissions().updateUserPermissions(USER_PERMISSION);
      collection.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      collection.updateAttribute(ATTRIBUTE_FULLNAME, ATTRIBUTE);
      return collectionDao.createCollection(collection);
   }

   @Test
   public void testCreateCollection() {
      Collection collection = prepareCollection(CODE);
      Entity entity = Entity.json(collection);

      Response response = client.target(COLLECTIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);
      assertThat(response.getLocation().getPath()).isEqualTo(COLLECTIONS_PATH + "/" + CODE);

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      assertThat(storedCollection).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollection() {
      createCollection(CODE);

      Collection updatedCollection = prepareCollection(CODE2);
      Entity entity = Entity.json(updatedCollection);

      Response response = client.target(COLLECTIONS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Collection returnedCollection = response.readEntity(JsonCollection.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedCollection.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedCollection.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE2);
      assertThat(storedCollection).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).containsOnly(GROUP_PERMISSION);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollection() {
      createCollection(CODE);

      Response response = client.target(COLLECTIONS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(COLLECTIONS_URL).build());

      assertThatThrownBy(() -> collectionDao.getCollectionByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetCollectionByCode() {
      createCollection(CODE);

      Response response = client.target(COLLECTIONS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Collection returnedCollection = response.readEntity(JsonCollection.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedCollection.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testGetAllCollections() {
      createCollection(CODE);
      createCollection(CODE2);

      Response response = client.target(COLLECTIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonCollection> collections = response.readEntity(new GenericType<List<JsonCollection>>() {
      });
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE, CODE2);

      Permissions permissions1 = collections.get(0).getPermissions();
      assertThat(permissions1).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(USER_PERMISSION));
      assertThat(permissions1).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions1).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());

      Permissions permissions2 = collections.get(1).getPermissions();
      assertThat(permissions2).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(USER_PERMISSION));
      assertThat(permissions2).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions2).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());
   }

   @Test
   public void testGetCollectionAttributes() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      Response response = client.target(COLLECTIONS_URL).path(CODE).path("attributes")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonAttribute> attributes = response.readEntity(new GenericType<List<JsonAttribute>>() {
      });
      assertThat(attributes).hasSize(1);

      JsonAttribute attribute = attributes.get(0);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(attribute.getFullName()).isEqualTo(ATTRIBUTE_FULLNAME);
      assertions.assertThat(attribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(attribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollectionAttribute() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      JsonAttribute updatedAttribute = new JsonAttribute(ATTRIBUTE_NAME, ATTRIBUTE_FULLNAME2, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      Entity entity = Entity.json(updatedAttribute);

      Response response = client.target(COLLECTIONS_URL).path(CODE).path("attributes").path(ATTRIBUTE_FULLNAME)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      JsonAttribute attribute = response.readEntity(new GenericType<JsonAttribute>() {
      });

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(attribute.getFullName()).isEqualTo(ATTRIBUTE_FULLNAME2);
      assertions.assertThat(attribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(attribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      Set<Attribute> storedAttributes = storedCollection.getAttributes();
      assertThat(storedAttributes).hasSize(1);

      Attribute storedAttribute = storedAttributes.iterator().next();
      assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(storedAttribute.getFullName()).isEqualTo(ATTRIBUTE_FULLNAME2);
      assertions.assertThat(storedAttribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollectionAttribute() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      Response response = client.target(COLLECTIONS_URL).path(CODE).path("attributes").path(ATTRIBUTE_FULLNAME)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      Set<Attribute> storedAttributes = storedCollection.getAttributes();
      assertThat(storedAttributes).isEmpty();
   }

   @Test
   public void testGetCollectionPermissions() {
      createCollection(CODE);

      Response response = client.target(PERMISSIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(JsonPermissions.class);
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createCollection(CODE);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      Entity entity = Entity.json(userPermission);

      Response response = client.target(PERMISSIONS_URL).path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission);

      Permissions storedPermissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createCollection(CODE);

      Response response = client.target(PERMISSIONS_URL).path("users").path(USER)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createCollection(CODE);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(PERMISSIONS_URL).path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission);

      Permissions storedPermissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createCollection(CODE);

      Response response = client.target(PERMISSIONS_URL).path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
