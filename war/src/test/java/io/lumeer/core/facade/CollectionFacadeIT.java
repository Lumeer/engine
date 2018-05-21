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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class CollectionFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ee00";

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;

   private static final String ATTRIBUTE_ID = "a1";
   private static final String ATTRIBUTE_NAME = "fullname";
   private static final Set<String> ATTRIBUTE_CONSTRAINTS = Collections.emptySet();
   private static final Integer ATTRIBUTE_COUNT = 0;

   private static final String ATTRIBUTE_NAME2 = "fullname2";

   private static final String CODE2 = "TCOLL2";
   private static final String NAME2 = "Test collection 2";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureProject() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      this.user = userDao.createUser(user);

      JsonPermissions organizationPermissions = new JsonPermissions();
      userPermission = new SimplePermission(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      groupDao.setOrganization(storedOrganization);
      Group group = new Group(GROUP);
      this.group = groupDao.createGroup(group);

      userPermission = new SimplePermission(this.user.getId(), Collection.ROLES);
      groupPermission = new SimplePermission(this.group.getId(), Collections.singleton(Role.READ));

      JsonProject project = new JsonProject();
      project.setCode(PROJECT_CODE);

      JsonPermissions projectPermissions = new JsonPermissions();
      projectPermissions.updateUserPermissions(new JsonPermission(this.user.getId(), Role.toStringRoles(Project.ROLES)));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
   }

   private Collection prepareCollection(String code) {
      return prepareCollection(code, NAME);
   }

   private Collection prepareCollection(String code, String name) {
      return new JsonCollection(code, name, ICON, COLOR, null);
   }

   private Collection createCollection(String code) {
      return createCollection(code, NAME);
   }

   private Collection createCollection(String code, String name) {
      Collection collection = prepareCollection(code, name);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      return collectionDao.createCollection(collection);
   }

   private Collection createCollection(String code, Attribute attribute) {
      Collection collection = prepareCollection(code);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      collection.updateAttribute(attribute.getId(), attribute);
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
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollection() {
      String collectionId = createCollection(CODE).getId();

      Collection updatedCollection = prepareCollection(CODE2);
      updatedCollection.getPermissions().removeUserPermission(USER);

      collectionFacade.updateCollection(collectionId, updatedCollection);

      Collection storedCollection = collectionDao.getCollectionByCode(CODE2);
      assertThat(storedCollection).isNotNull();
      assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testDeleteCollection() {
      String collectionId = createCollection(CODE).getId();

      collectionFacade.deleteCollection(collectionId);

      assertThatThrownBy(() -> collectionDao.getCollectionByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetCollection() {
      String collectionId = createCollection(CODE).getId();

      Collection storedCollection = collectionFacade.getCollection(collectionId);
      assertThat(storedCollection).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertAll();

      assertPermissions(storedCollection.getPermissions().getUserPermissions(), userPermission);
      assertPermissions(storedCollection.getPermissions().getGroupPermissions(), groupPermission);
   }

   @Test
   public void testGetCollections() {
      createCollection(CODE);
      createCollection(CODE2);

      assertThat(collectionFacade.getCollections(new Pagination(null, null)))
            .extracting(Resource::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetCollectionNames() {
      createCollection(CODE, NAME);
      createCollection(CODE2, NAME2);

      assertThat(collectionFacade.getCollectionNames()).containsOnly(NAME, NAME2);
   }

   @Test
   public void testUpdateCollectionAttributeAdd() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).isEmpty();

      JsonAttribute attribute = new JsonAttribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      final Attribute createdAttribute = collectionFacade.updateCollectionAttribute(collection.getId(), ATTRIBUTE_ID, attribute);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getId()).isEqualTo(createdAttribute.getId());
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(storedAttribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollectionAttributeUpdate() {
      JsonAttribute attribute = new JsonAttribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      JsonAttribute updatedAttribute = new JsonAttribute(ATTRIBUTE_ID, ATTRIBUTE_NAME2, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      collectionFacade.updateCollectionAttribute(collection.getId(), ATTRIBUTE_ID, updatedAttribute);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getId()).isEqualTo(ATTRIBUTE_ID);
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME2);
      assertions.assertThat(storedAttribute.getConstraints()).isEqualTo(ATTRIBUTE_CONSTRAINTS);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollectionAttribute() {
      JsonAttribute attribute = new JsonAttribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINTS, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      collectionFacade.deleteCollectionAttribute(collection.getId(), ATTRIBUTE_ID);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).isEmpty();
   }

   @Test
   public void testGetCollectionPermissions() {
      String collectionId = createCollection(CODE).getId();

      Permissions permissions = collectionFacade.getCollectionPermissions(collectionId);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      String collectionId = createCollection(CODE).getId();

      SimplePermission userPermission = new SimplePermission(user.getId(), new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      collectionFacade.updateUserPermissions(collectionId, userPermission);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      String collectionId = createCollection(CODE).getId();

      collectionFacade.removeUserPermission(collectionId, user.getId());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      String collectionId = createCollection(CODE).getId();

      SimplePermission groupPermission = new SimplePermission(group.getId(), new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      collectionFacade.updateGroupPermissions(collectionId, groupPermission);

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      String collectionId = createCollection(CODE).getId();

      collectionFacade.removeGroupPermission(collectionId, group.getId());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testTooManyCollections() {
      for (int i = 1; i <= 10; i++) {
         Collection collection = prepareCollection(CODE + i);
         collectionFacade.createCollection(collection);
      }

      Collection collection = prepareCollection(CODE + "11");
      assertThatExceptionOfType(ServiceLimitsExceededException.class).isThrownBy(() -> {
         collectionFacade.createCollection(collection);
      }).as("On Trial plan, it should be possible to create only 10 collections but it was possible to create another one.");
   }

   public void testAddFavoriteCollection() {
      List<String> ids = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         Collection collection = prepareCollection(CODE + i);
         ids.add(collectionFacade.createCollection(collection).getId());
      }

      assertThat(collectionFacade.getFavoriteCollectionsIds()).isEmpty();

      collectionFacade.addFavoriteCollection(ids.get(0));
      collectionFacade.addFavoriteCollection(ids.get(3));
      collectionFacade.addFavoriteCollection(ids.get(5));

      assertThat(collectionFacade.getFavoriteCollectionsIds()).containsOnly(ids.get(0), ids.get(3), ids.get(5));

      for (int i = 0; i < 10; i++) {
         assertThat(collectionFacade.isFavorite(ids.get(i))).isEqualTo(i == 0 || i == 3 || i == 5);
      }
   }

   @Test
   public void testRemoveFavoriteCollection() {
      List<String> ids = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         Collection collection = prepareCollection(CODE + i);
         ids.add(collectionFacade.createCollection(collection).getId());
      }

      collectionFacade.addFavoriteCollection(ids.get(1));
      collectionFacade.addFavoriteCollection(ids.get(2));
      collectionFacade.addFavoriteCollection(ids.get(9));

      assertThat(collectionFacade.getFavoriteCollectionsIds()).containsOnly(ids.get(1), ids.get(2), ids.get(9));

      collectionFacade.removeFavoriteCollection(ids.get(1));
      collectionFacade.removeFavoriteCollection(ids.get(9));

      assertThat(collectionFacade.getFavoriteCollectionsIds()).containsOnly(ids.get(2));
   }

   @Test
   public void testSetDefaultAttribute() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE));
      assertThat(collection.getDefaultAttributeId()).isNull();

      collectionFacade.setDefaultAttribute(collection.getId(), "some not existing id");
      collection = collectionFacade.getCollection(collection.getId());
      assertThat(collection.getDefaultAttributeId()).isNull();

      JsonAttribute attribute = new JsonAttribute("a1");
      Attribute created =  new ArrayList<Attribute>(collectionFacade.createCollectionAttributes(collection.getId(), Collections.singletonList(attribute))).get(0);

      collectionFacade.setDefaultAttribute(collection.getId(), created.getId());
      collection = collectionFacade.getCollection(collection.getId());
      assertThat(collection.getDefaultAttributeId()).isEqualTo(created.getId());
   }

   @Test
   public void testDocumentCount() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE));

      assertThat(collectionFacade.getDocumentsCountInAllCollections()).isEqualTo(0);

      documentFacade.createDocument(collection.getId(), new JsonDocument(new DataDocument("pepa", "zdepa")));
      documentFacade.createDocument(collection.getId(), new JsonDocument(new DataDocument("tonda", "fonda")));
      documentFacade.createDocument(collection.getId(), new JsonDocument(new DataDocument("franta", "pajta")));

      assertThat(collectionFacade.getDocumentsCountInAllCollections()).isEqualTo(3);
   }
}
