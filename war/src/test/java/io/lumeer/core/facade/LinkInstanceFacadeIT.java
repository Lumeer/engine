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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkPermissionsType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;

import org.bson.types.ObjectId;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
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

   static {
      Attribute attribute1 = new Attribute(ATTRIBUTE1_NAME);
      Attribute attribute2 = new Attribute(ATTRIBUTE2_NAME);
      ATTRIBUTES = Arrays.asList(attribute1, attribute2);
   }

   private List<String> documentIdsColl1 = new ArrayList<>();
   private List<String> documentIdsColl2 = new ArrayList<>();
   private String linkTypeId1;
   private String linkTypeId2;
   private String collection1Id;
   private String collection2Id;
   private User user;
   private Group group;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

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
   private GroupDao groupDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private PermissionsChecker permissionsChecker;

   @BeforeEach
   public void configureLinkInstances() {
      user = userDao.createUser(new User(USER));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);
      groupDao.setOrganization(storedOrganization);
      group = groupDao.createGroup(new Group("testGroup", Collections.singletonList(user.getId())));
      user.setOrganizations(Collections.singleton(storedOrganization.getId()));
      user = userDao.updateUser(user.getId(), user);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(user.getId(), Collections.singleton(new Role(RoleType.Read)));
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(user.getId(), Collections.singleton(new Role(RoleType.Read)));
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      collectionDao.setProject(storedProject);

      Permissions collectionPermissions = new Permissions();
      collectionPermissions.updateUserPermissions(new Permission(user.getId(), Collections.singleton(new Role(RoleType.Read))));
      Collection collection = new Collection("col1", "col1", "icon", "color", collectionPermissions);
      collection1Id = collectionDao.createCollection(collection).getId();

      Collection collection2 = new Collection("col2", "col2", "icon", "color", collectionPermissions);
      collection2Id = collectionDao.createCollection(collection2).getId();

      LinkType linkType = new LinkType(NAME, Arrays.asList(collection1Id, collection2Id), ATTRIBUTES, null, null, null);
      linkTypeId1 = linkTypeDao.createLinkType(linkType).getId();
      LinkType linkType2 = new LinkType(NAME2, Arrays.asList(collection1Id, collection2Id), ATTRIBUTES, null, null, null);
      linkTypeId2 = linkTypeDao.createLinkType(linkType2).getId();

      documentIdsColl1.clear();
      for (int i = 0; i < 3; i++) {
         documentIdsColl1.add(createDocument(collection1Id).getId());
      }

      documentIdsColl2.clear();
      for (int i = 0; i < 3; i++) {
         documentIdsColl2.add(createDocument(collection2Id).getId());
      }
      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   @Test
   public void testCreateLinkInstance() {
      LinkInstance linkInstance = prepareLinkInstance();
      DataDocument data = new DataDocument()
            .append(KEY1, VALUE1);
      linkInstance.setData(data);

      assertThatThrownBy(() -> linkInstanceFacade.createLinkInstance(linkInstance))
            .isInstanceOf(NoPermissionException.class);

      setLinkTypePermissions(linkTypeDao.getLinkType(linkTypeId1), Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)), true);

      var returnedLinkInstance = linkInstanceFacade.createLinkInstance(linkInstance);
      var id = returnedLinkInstance.getId();
      assertThat(returnedLinkInstance).isNotNull();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();
      assertThat(returnedLinkInstance.getData()).containsEntry(KEY1, VALUE1);

      LinkInstance storedLinkInstance = linkInstanceDao.getLinkInstance(id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getLinkTypeId()).isEqualTo(linkTypeId1);
      assertThat(storedLinkInstance.getDocumentIds()).containsOnly(documentIdsColl1.get(0), documentIdsColl2.get(0));

      var storedData = linkDataDao.getData(linkInstance.getLinkTypeId(), id);
      assertThat(storedData).containsEntry(KEY1, VALUE1);
   }

   @Test
   public void testUpdateLinkInstanceData() {
      setLinkTypePermissions(linkTypeDao.getLinkType(linkTypeId1), Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)), false);

      LinkInstance linkInstance = prepareLinkInstance();
      String id = linkInstanceFacade.createLinkInstance(linkInstance).getId();

      LinkInstance created = linkInstanceFacade.getLinkInstance(linkTypeId1, id);
      assertThat(created).isNotNull();
      assertThat(created.getData()).hasSize(1); // contains only id field
      assertThat(created.getData()).doesNotContainEntry("k1", "v1");
      assertThat(created.getData()).doesNotContainEntry("k2", "v2");

      var updatedLinkInstance = linkInstanceFacade.updateLinkInstanceData(id, new DataDocument("k1", "v1").append("k2", "v2"));
      assertThat(updatedLinkInstance).isNotNull();
      assertThat(updatedLinkInstance.getData()).containsEntry("k1", "v1");
      assertThat(updatedLinkInstance.getData()).containsEntry("k2", "v2");

      LinkInstance storedLinkInstance = linkInstanceFacade.getLinkInstance(linkTypeId1, id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getData()).containsEntry("k1", "v1");
      assertThat(storedLinkInstance.getData()).containsEntry("k2", "v2");

      updatedLinkInstance = linkInstanceFacade.updateLinkInstanceData(id, new DataDocument("k3", "v3").append("k4", "v4"));
      assertThat(updatedLinkInstance).isNotNull();
      assertThat(updatedLinkInstance.getData()).doesNotContainEntry("k1", "v1");
      assertThat(updatedLinkInstance.getData()).doesNotContainEntry("k2", "v2");
      assertThat(updatedLinkInstance.getData()).containsEntry("k3", "v3");
      assertThat(updatedLinkInstance.getData()).containsEntry("k4", "v4");

      storedLinkInstance = linkInstanceFacade.getLinkInstance(linkTypeId1, id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getData()).doesNotContainEntry("k1", "v1");
      assertThat(storedLinkInstance.getData()).doesNotContainEntry("k2", "v2");
      assertThat(storedLinkInstance.getData()).containsEntry("k3", "v3");
      assertThat(storedLinkInstance.getData()).containsEntry("k4", "v4");
   }

   @Test
   public void testPatchLinkInstanceData() {
      setCollectionsGroupRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));
      LinkInstance linkInstance = prepareLinkInstance();
      String id = linkInstanceFacade.createLinkInstance(linkInstance).getId();

      LinkInstance created = linkInstanceFacade.getLinkInstance(linkTypeId1, id);
      assertThat(created).isNotNull();
      assertThat(created.getData()).hasSize(1); // contains only id field
      assertThat(created.getData()).doesNotContainEntry("k1", "v1");
      assertThat(created.getData()).doesNotContainEntry("k2", "v2");

      var patchedLinkInstance = linkInstanceFacade.patchLinkInstanceData(id, new DataDocument("k1", "v1").append("k2", "v2"));
      assertThat(patchedLinkInstance).isNotNull();
      assertThat(patchedLinkInstance.getData()).containsEntry("k1", "v1");
      assertThat(patchedLinkInstance.getData()).containsEntry("k2", "v2");

      LinkInstance storedLinkInstance = linkInstanceFacade.getLinkInstance(linkTypeId1, id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getData()).containsEntry("k1", "v1");
      assertThat(storedLinkInstance.getData()).containsEntry("k2", "v2");

      patchedLinkInstance = linkInstanceFacade.patchLinkInstanceData(id, new DataDocument("k3", "v3").append("k4", "v4"));
      assertThat(patchedLinkInstance).isNotNull();
      assertThat(patchedLinkInstance.getData()).containsEntry("k1", "v1");
      assertThat(patchedLinkInstance.getData()).containsEntry("k2", "v2");
      assertThat(patchedLinkInstance.getData()).containsEntry("k3", "v3");
      assertThat(patchedLinkInstance.getData()).containsEntry("k4", "v4");

      storedLinkInstance = linkInstanceFacade.getLinkInstance(linkTypeId1, id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getData()).containsEntry("k1", "v1");
      assertThat(storedLinkInstance.getData()).containsEntry("k2", "v2");
      assertThat(storedLinkInstance.getData()).containsEntry("k3", "v3");
      assertThat(storedLinkInstance.getData()).containsEntry("k4", "v4");
   }

   @Test
   public void testDeleteLinkInstance() {
      setCollectionsGroupRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));

      LinkInstance created = linkInstanceFacade.createLinkInstance(prepareLinkInstance());
      assertThat(created.getId()).isNotNull();

      linkInstanceFacade.deleteLinkInstance(created.getId());

      assertThatThrownBy(() -> linkInstanceDao.getLinkInstance(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDuplicateLinkInstances() {
      setCollectionsGroupRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));
      var masterDocument = createDocument(collection1Id);
      var slaveDocuments = IntStream.range(0, 10).mapToObj(i -> createDocument(collection2Id)).collect(Collectors.toList());

      final AtomicInteger c = new AtomicInteger(0);
      var links = slaveDocuments.stream().map(d -> {
         var link = prepareLinkInstance();
         link.setDocumentIds(new ArrayList<>(List.of(masterDocument.getId(), d.getId()))); // we need the list to be modifiable
         link.setData(new DataDocument().append("sample", c.addAndGet(1)));
         link.setCreatedBy(user.getId());
         var newLink = linkInstanceDao.createLinkInstance(link);
         linkDataDao.createData(newLink.getLinkTypeId(), newLink.getId(), link.getData());

         return newLink;
      }).collect(Collectors.toList());

      var copiedSlaveDocuments = documentFacade.duplicateDocuments(collection2Id, slaveDocuments.stream().map(Document::getId).collect(Collectors.toList()));
      var documentMap = new HashMap<String, String>();
      copiedSlaveDocuments.forEach(d -> documentMap.put(d.getMetaData().getString(Document.META_ORIGINAL_DOCUMENT_ID), d.getId()));

      var newLinks = linkInstanceFacade.duplicateLinkInstances(masterDocument.getId(), masterDocument.getId(), links.stream().map(LinkInstance::getId).collect(Collectors.toSet()), documentMap);
   }

   private LinkInstance prepareLinkInstance() {
      return new LinkInstance(linkTypeId1, Arrays.asList(documentIdsColl1.get(0), documentIdsColl2.get(0)));
   }

   private Document prepareDocument() {
      DataDocument data = new DataDocument()
            .append(KEY1, VALUE1)
            .append(KEY2, VALUE2);

      return new Document(data);
   }

   private LinkType setLinkTypePermissions(LinkType linkType, final Set<Role> roles, boolean setGroupPermissions) {
      if (roles.isEmpty()) {
         linkType.setPermissions(null);
         linkType.setPermissionsType(LinkPermissionsType.Merge);
      } else {
         Permissions permissions = new Permissions();
         if (setGroupPermissions) {
            permissions.updateUserPermissions(new Permission(user.getId(), roles));
         } else {
            permissions.updateGroupPermissions(new Permission(group.getId(), roles));
         }
         linkType.setPermissions(permissions);
         linkType.setPermissionsType(LinkPermissionsType.Custom);
      }
      return linkTypeDao.updateLinkType(linkType.getId(), linkType, null);
   }

   private void setCollectionsGroupRoles(final Set<Role> roles) {
      for (String collectionId : Arrays.asList(collection1Id, collection2Id)) {
         Permissions collectionPermissions = new Permissions();
         collectionPermissions.updateGroupPermissions(new Permission(group.getId(), roles));
         Collection collection = collectionDao.getCollectionById(collectionId);
         collection.setPermissions(collectionPermissions);
         collectionDao.updateCollection(collectionId, collection, null);
      }
      permissionsChecker.getPermissionAdapter().invalidateCollectionCache();
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
