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
package io.lumeer.core.facade;

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
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.core.model.SimpleUser;
import io.lumeer.engine.IntegrationTestBase;
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
import java.util.Set;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class CollectionFacadeIntegrationTest extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ee00";

   private static final Permission USER_PERMISSION = new SimplePermission(USER, View.ROLES);
   private static final Permission GROUP_PERMISSION = new SimplePermission(GROUP, Collections.singleton(Role.READ));

   private static final String ATTRIBUTE_NAME = "name";
   private static final String ATTRIBUTE_FULLNAME = "fullname";
   private static final Set<String> ATTRIBUTE_CONSTRAINTS = Collections.emptySet();
   private static final Integer ATTRIBUTE_COUNT = 0;

   private static final String ATTRIBUTE_FULLNAME2 = "fullname2";

   private static final String CODE2 = "TCOLL2";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

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
      projectPermissions.updateUserPermissions(new JsonPermission(USER, Role.toStringRoles(Project.ROLES)));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
   }

   private Collection prepareCollection(String code) {
      return new JsonCollection(code, NAME, ICON, COLOR, null);
   }

   private Collection createCollection(String code) {
      Collection collection = prepareCollection(code);
      collection.getPermissions().updateUserPermissions(USER_PERMISSION);
      collection.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return collectionDao.createCollection(collection);
   }

   private Collection createCollection(String code, Attribute attribute) {
      Collection collection = prepareCollection(code);
      collection.getPermissions().updateUserPermissions(USER_PERMISSION);
      collection.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      collection.updateAttribute(attribute.getFullName(), attribute);
      return collectionDao.createCollection(collection);
   }

   @Test
   public void testCreateCollection() {
      Collection collection = prepareCollection(CODE);

      Collection returnedCollection = collectionFacade.createCollection(collection);
      assertThat(returnedCollection).isNotNull();
      assertThat(returnedCollection.getId()).isNotNull();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      assertThat(storedCollection).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollection() {
      createCollection(CODE);

      Collection updatedCollection = prepareCollection(CODE2);
      updatedCollection.getPermissions().removeUserPermission(USER);

      collectionFacade.updateCollection(CODE, updatedCollection);

      Collection storedCollection = collectionDao.getCollectionByCode(CODE2);
      assertThat(storedCollection).isNotNull();
      assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
   }

   @Test
   public void testDeleteCollection() {
      createCollection(CODE);

      collectionFacade.deleteCollection(CODE);

      assertThatThrownBy(() -> collectionDao.getCollectionByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetCollection() {
      createCollection(CODE);

      Collection storedCollection = collectionFacade.getCollection(CODE);
      assertThat(storedCollection).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      assertPermissions(storedCollection.getPermissions().getUserPermissions(), USER_PERMISSION);
   }

   @Test
   public void testGetCollections() {
      createCollection(CODE);
      createCollection(CODE2);

      assertThat(collectionFacade.getCollections(new Pagination(null, null)))
            .extracting(Resource::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testUpdateCollectionAttributeAdd() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).isEmpty();

      JsonAttribute attribute = new JsonAttribute(ATTRIBUTE_NAME, ATTRIBUTE_FULLNAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      collectionFacade.updateCollectionAttribute(CODE, ATTRIBUTE_FULLNAME, attribute);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(storedAttribute.getFullName()).isEqualTo(ATTRIBUTE_FULLNAME);
      assertions.assertThat(storedAttribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollectionAttributeUpdate() {
      JsonAttribute attribute = new JsonAttribute(ATTRIBUTE_NAME, ATTRIBUTE_FULLNAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      JsonAttribute updatedAttribute = new JsonAttribute(ATTRIBUTE_NAME, ATTRIBUTE_FULLNAME2, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      collectionFacade.updateCollectionAttribute(CODE, ATTRIBUTE_FULLNAME, updatedAttribute);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(storedAttribute.getFullName()).isEqualTo(ATTRIBUTE_FULLNAME2);
      assertions.assertThat(storedAttribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollectionAttribute() {
      JsonAttribute attribute = new JsonAttribute(ATTRIBUTE_NAME, ATTRIBUTE_FULLNAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      collectionFacade.deleteCollectionAttribute(CODE, ATTRIBUTE_FULLNAME);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).isEmpty();
   }

   @Test
   public void testGetCollectionPermissions() {
      createCollection(CODE);

      Permissions permissions = collectionFacade.getCollectionPermissions(CODE);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createCollection(CODE);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      collectionFacade.updateUserPermissions(CODE, userPermission);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createCollection(CODE);

      collectionFacade.removeUserPermission(CODE, USER);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createCollection(CODE);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      collectionFacade.updateGroupPermissions(CODE, groupPermission);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createCollection(CODE);

      collectionFacade.removeGroupPermission(CODE, GROUP);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
