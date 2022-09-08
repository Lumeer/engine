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

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.AttributeLock;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.facade.ZapierFacade;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
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
public class CollectionServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = new ObjectId().toHexString();

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";

   private static final Set<Role> USER_ROLES = View.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(new Role(RoleType.Read));
   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;
   private Organization organization;
   private Project project;

   private static final String CODE2 = "TCOLL2";
   private static final String NAME2 = "Test collection 2";

   private static final String ATTRIBUTE_ID = "a1";
   private static final String ATTRIBUTE_NAME = "fullname";
   private static final Constraint ATTRIBUTE_CONSTRAINT = new Constraint(ConstraintType.Boolean, null);
   private static final Function ATTRIBUTE_FUNCTION = new Function("js", "xml", "error", 123456L, false);
   private static final Integer ATTRIBUTE_COUNT = 0;
   private static final AttributeLock ATTRIBUTE_LOCK = new AttributeLock(new ArrayList<>(), false);

   private static final String ATTRIBUTE_NAME2 = "fullname2";

   private static final Attribute ATTRIBUTE = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, null, ATTRIBUTE_CONSTRAINT, null, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);

   private String collectionsUrl;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private CollectionDao collectionDao;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);
      groupDao.setOrganization(storedOrganization);
      group = groupDao.createGroup(new Group(GROUP, Collections.singletonList(user.getId())));

      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      userPermission = Permission.buildWithRoles(this.user.getId(), USER_ROLES);
      groupPermission = Permission.buildWithRoles(group.getId(), GROUP_ROLES);

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(new Permission(this.user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      collectionDao.setProject(storedProject);
      collectionDao.createRepository(project);

      this.collectionsUrl = projectPath(storedOrganization, storedProject) + "collections";
      this.organization = storedOrganization;
      this.project = storedProject;

      PermissionCheckerUtil.allowGroups();
   }

   private Collection prepareCollection(String code) {
      return prepareCollection(code, NAME);
   }

   private Collection prepareCollection(String code, String name) {
      return new Collection(code, name, ICON, COLOR, null);
   }

   private Collection createCollection(String code) {
      return createCollection(code, NAME);
   }

   private Collection createCollection(String code, String name) {
      Collection collection = prepareCollection(code, name);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      collection.createAttribute(ATTRIBUTE);
      return collectionDao.createCollection(collection);
   }

   @Test
   public void testCreateCollection() {
      Collection collection = prepareCollection(CODE);
      Entity entity = Entity.json(collection);

      Response response = client.target(collectionsUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Collection returnedCollection = response.readEntity(Collection.class);
      assertThat(returnedCollection).isNotNull();
      assertThat(returnedCollection.getId()).isNotNull();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      assertThat(storedCollection).isNotNull();
      assertThat(returnedCollection.getId()).isEqualTo(storedCollection.getId());

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollection() {
      String collectionId = createCollection(CODE).getId();

      Collection updatedCollection = prepareCollection(CODE2);
      updatedCollection.setPermissions(new Permissions(Set.of(userPermission), Set.of(groupPermission)));
      Entity entity = Entity.json(updatedCollection);

      Response response = client.target(collectionsUrl).path(collectionId)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Collection returnedCollection = response.readEntity(Collection.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedCollection.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedCollection.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE2);
      assertThat(storedCollection).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollection() {
      String collectionId = createCollection(CODE).getId();

      Response response = client.target(collectionsUrl).path(collectionId)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(collectionsUrl).build());

      assertThatThrownBy(() -> collectionDao.getCollectionByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetCollection() {
      String collectionId = createCollection(CODE).getId();

      Response response = client.target(collectionsUrl).path(collectionId)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Collection returnedCollection = response.readEntity(Collection.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedCollection.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testGetAllCollections() {
      createCollection(CODE);
      createCollection(CODE2, NAME2);

      Response response = client.target(collectionsUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Collection> collections = response.readEntity(new GenericType<List<Collection>>() {
      });
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE, CODE2);

      Permissions permissions1 = collections.get(0).getPermissions();
      assertThat(permissions1.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions1.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions1.getGroupPermissions().iterator()).toIterable().containsOnly(groupPermission);

      Permissions permissions2 = collections.get(1).getPermissions();
      assertThat(permissions2.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions2.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions2.getGroupPermissions().iterator()).toIterable().containsOnly(groupPermission);
   }

   @Test
   public void testGetCollectionAttributes() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      Response response = client.target(collectionsUrl).path(collection.getId()).path("attributes")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Attribute> attributes = response.readEntity(new GenericType<List<Attribute>>() {
      });
      assertThat(attributes).hasSize(1);

      Attribute attribute = attributes.get(0);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attribute.getId()).isEqualTo(ATTRIBUTE_ID);
      assertions.assertThat(attribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(attribute.getConstraint()).isEqualTo(ATTRIBUTE_CONSTRAINT);
      assertions.assertThat(attribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testGetZapierAttributes() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      Response response = client.target(zapierPath()).path("collection").path("attributes")
                                .queryParam("collection_hash", getZapierCollectionHash(organization, project, collection))
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();

      List<? extends ZapierFacade.ZapierField> fields = response.readEntity(new GenericType<List<? extends ZapierFacade.ZapierField>>() {
      });

      assertThat(fields).hasSize(2);
      assertThat(fields.get(0)).isEqualTo(new ZapierFacade.ZapierField("_id", "_id", "string", true, false, false));
      assertThat(fields.get(1)).isEqualTo(new ZapierFacade.ZapierField("a1", "fullname", "boolean", true, false, false));
   }

   @Test
   public void testUpdateCollectionAttribute() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute updatedAttribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME2, null, ATTRIBUTE_CONSTRAINT, ATTRIBUTE_LOCK, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);
      Entity entity = Entity.json(updatedAttribute);

      Response response = client.target(collectionsUrl).path(collection.getId()).path("attributes").path(ATTRIBUTE_ID)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Attribute attribute = response.readEntity(new GenericType<Attribute>() {
      });

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attribute.getId()).isEqualTo(ATTRIBUTE_ID);
      assertions.assertThat(attribute.getName()).isEqualTo(ATTRIBUTE_NAME2);
      assertions.assertThat(attribute.getConstraint()).isEqualTo(ATTRIBUTE_CONSTRAINT);
      assertions.assertThat(attribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertThat(attribute.getLock()).isEqualTo(ATTRIBUTE_LOCK);
      assertions.assertAll();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      Set<Attribute> storedAttributes = storedCollection.getAttributes();
      assertThat(storedAttributes).hasSize(1);

      Attribute storedAttribute = storedAttributes.iterator().next();
      assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getId()).isEqualTo(ATTRIBUTE_ID);
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME2);
      assertions.assertThat(storedAttribute.getConstraint()).isEqualTo(ATTRIBUTE_CONSTRAINT);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertThat(storedAttribute.getLock()).isEqualTo(ATTRIBUTE_LOCK);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollectionAttribute() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).hasSize(1);

      Response response = client.target(collectionsUrl).path(collection.getId()).path("attributes").path(ATTRIBUTE_ID)
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
      String collectionId = createCollection(CODE).getId();

      Response response = client.target(collectionsUrl).path(collectionId).path("permissions")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(Permissions.class);
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      String collectionId = createCollection(CODE).getId();

      Permission[] userPermission = { Permission.buildWithRoles(user.getId(), Set.of(new Role(RoleType.DataWrite), new Role(RoleType.Manage))) };
      Entity entity = Entity.json(userPermission);

      Response response = client.target(collectionsUrl).path(collectionId).path("permissions").path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission[0]);

      Permissions storedPermissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission[0]);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      String collectionId = createCollection(CODE).getId();

      Response response = client.target(collectionsUrl).path(collectionId).path("permissions").path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      String collectionId = createCollection(CODE).getId();

      Permission[] groupPermission = { Permission.buildWithRoles(this.group.getId(), Set.of(new Role(RoleType.DataWrite), new Role(RoleType.Read))) };
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(collectionsUrl).path(collectionId).path("permissions").path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission[0]);

      Permissions storedPermissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission[0]);
   }

   @Test
   public void testRemoveGroupPermission() {
      String collectionId = createCollection(CODE).getId();

      Response response = client.target(collectionsUrl).path(collectionId).path("permissions").path("groups").path(this.group.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
