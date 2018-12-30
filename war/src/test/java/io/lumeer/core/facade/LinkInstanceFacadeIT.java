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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;

import org.bson.types.ObjectId;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class LinkInstanceFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String KEY1 = "A";
   private static final String KEY2 = "B";
   private static final String VALUE1 = "firstValue";
   private static final String VALUE2 = "secondValue";

   private static final String NAME = "Connection";
   private static final String NAME2 = "Whuaaaa";
   private static final String ATTRIBUTE1_NAME = "Maxi";
   private static final String ATTRIBUTE2_NAME = "Light";
   private static final List<Attribute> ATTRIBUTES;
   private static final Map<String, Object> DATA;

   static {
      Attribute attribute1 = new Attribute(ATTRIBUTE1_NAME);
      Attribute attribute2 = new Attribute(ATTRIBUTE2_NAME);
      ATTRIBUTES = Arrays.asList(attribute1, attribute2);

      DATA = Collections.singletonMap("entry", "value");
   }

   private List<String> documentIdsColl1 = new ArrayList<>();
   private List<String> documentIdsColl2 = new ArrayList<>();
   private String linkTypeId1;
   private String linkTypeId2;
   private String collection1Id;
   private String collection2Id;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Before
   public void configureLinkInstances() {
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(createdUser.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(createdUser.getId(), Project.ROLES);
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);

      Permissions collectionPermissions = new Permissions();
      collectionPermissions.updateUserPermissions(new Permission(createdUser.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      Collection collection = new Collection("col1", "col1", "icon", "color", collectionPermissions);
      collection.setDocumentsCount(0);
      collection1Id = collectionDao.createCollection(collection).getId();

      Collection collection2 = new Collection("col2", "col2", "icon", "color", collectionPermissions);
      collection2.setDocumentsCount(0);
      collection2Id = collectionDao.createCollection(collection2).getId();

      LinkType linkType = new LinkType(null, NAME, Arrays.asList(collection1Id, collection2Id), ATTRIBUTES);
      linkTypeId1 = linkTypeDao.createLinkType(linkType).getId();
      LinkType linkType2 = new LinkType(null, NAME2, Arrays.asList(collection1Id, collection2Id), ATTRIBUTES);
      linkTypeId2 = linkTypeDao.createLinkType(linkType2).getId();

      documentIdsColl1.clear();
      for (int i = 0; i < 3; i++) {
         documentIdsColl1.add(createDocument(collection1Id).getId());
      }

      documentIdsColl2.clear();
      for (int i = 0; i < 3; i++) {
         documentIdsColl2.add(createDocument(collection2Id).getId());
      }
   }

   @Test
   public void testCreateLinkInstance() {
      LinkInstance linkInstance = prepareLinkInstance();

      String id = linkInstanceFacade.createLinkInstance(linkInstance).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      LinkInstance storedLinkInstance = linkInstanceDao.getLinkInstance(id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getLinkTypeId()).isEqualTo(linkTypeId1);
      assertThat(storedLinkInstance.getDocumentIds()).containsOnlyElementsOf(Arrays.asList(documentIdsColl1.get(0), documentIdsColl2.get(0)));
      assertThat(storedLinkInstance.getData().keySet()).containsOnlyElementsOf(DATA.keySet());
   }

   @Test
   public void testUpdateLinkInstance() {
      LinkInstance linkInstance = prepareLinkInstance();
      String id = linkInstanceFacade.createLinkInstance(linkInstance).getId();

      LinkInstance updateLinkedInstance = prepareLinkInstance();
      updateLinkedInstance.setLinkTypeId(linkTypeId2);
      updateLinkedInstance.setDocumentIds(Arrays.asList(documentIdsColl1.get(1), documentIdsColl2.get(1)));

      linkInstanceFacade.updateLinkInstance(id, updateLinkedInstance);

      LinkInstance storedLinkInstance = linkInstanceDao.getLinkInstance(id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getLinkTypeId()).isEqualTo(linkTypeId2);
      assertThat(storedLinkInstance.getDocumentIds()).containsOnlyElementsOf(Arrays.asList(documentIdsColl1.get(1), documentIdsColl2.get(1)));
      assertThat(storedLinkInstance.getData().keySet()).containsOnlyElementsOf(DATA.keySet());
   }

   @Test
   public void testDeleteLinkInstance() {
      LinkInstance created = linkInstanceFacade.createLinkInstance(prepareLinkInstance());
      assertThat(created.getId()).isNotNull();

      linkInstanceFacade.deleteLinkInstance(created.getId());

      assertThatThrownBy(() -> linkInstanceDao.getLinkInstance(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   private LinkInstance prepareLinkInstance() {
      return new LinkInstance(null, linkTypeId1, Arrays.asList(documentIdsColl1.get(0), documentIdsColl2.get(0)), DATA);
   }

   private Document prepareDocument() {
      DataDocument data = new DataDocument()
            .append(KEY1, VALUE1)
            .append(KEY2, VALUE2);

      return new Document(data);
   }

   private Document createDocument(String collectionId) {
      Document document = prepareDocument();
      document.setCollectionId(collectionId);
      document.setCreatedBy(USER);
      document.setCreationDate(ZonedDateTime.now());
      Document storedDocument = documentDao.createDocument(document);

      DataDocument storedData = dataDao.createData(collectionId, storedDocument.getId(), document.getData());

      storedDocument.setData(storedData);
      return storedDocument;
   }
}
