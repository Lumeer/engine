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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimpleUser;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class DocumentFacadeIntegrationTest extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String COLLECTION_NAME = "Testing collection";
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String KEY1 = "A";
   private static final String KEY2 = "B";
   private static final String VALUE1 = "firstValue";
   private static final String VALUE2 = "secondValue";

   @Inject
   private DocumentFacade documentFacade;

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

   private Collection collection;

   @Before
   public void configureCollection() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      organizationDao.createOrganization(organization);

      projectDao.setOrganization(organization);
      userDao.setOrganization(organization);

      SimpleUser user = new SimpleUser(USER);
      userDao.createUser(user);

      JsonProject project = new JsonProject();
      project.setCode(PROJECT_CODE);

      JsonPermissions projectPermissions = new JsonPermissions();
      projectPermissions.updateUserPermissions(new JsonPermission(USER, Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(storedProject);

      JsonPermissions collectionPermissions = new JsonPermissions();
      collectionPermissions.updateUserPermissions(new JsonPermission(USER, Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      JsonCollection jsonCollection = new JsonCollection(null, COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
      collection = collectionDao.createCollection(jsonCollection);
   }

   private Document prepareDocument() {
      DataDocument data = new DataDocument()
            .append(KEY1, VALUE1)
            .append(KEY2, VALUE2);

      return new JsonDocument(data);
   }

   private Document createDocument() {
      Document document = prepareDocument();
      document.setCollectionId(collection.getId());
      document.setCreatedBy(USER);
      document.setCreationDate(LocalDateTime.now());
      document.setDataVersion(DocumentFacade.INITIAL_VERSION);
      Document storedDocument = documentDao.createDocument(document);

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), document.getData());

      storedDocument.setData(storedData);
      return storedDocument;
   }

   @Test
   public void testCreateDocument() {
      Document document = prepareDocument();

      LocalDateTime beforeTime = LocalDateTime.now();
      String id = documentFacade.createDocument(collection.getCode(), document).getId();
      assertThat(id).isNotNull();

      Document storedDocument = documentDao.getDocumentById(id);
      assertThat(storedDocument).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(storedDocument.getCollectionCode()).isNull();
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(USER);
      assertions.assertThat(storedDocument.getCreationDate()).isAfterOrEqualTo(beforeTime).isBeforeOrEqualTo(LocalDateTime.now());
      assertions.assertThat(storedDocument.getUpdatedBy()).isNull();
      assertions.assertThat(storedDocument.getUpdateDate()).isNull();
      assertions.assertThat(storedDocument.getDataVersion()).isEqualTo(1);
      assertions.assertThat(storedDocument.getData()).isNull();
      assertions.assertAll();

      DataDocument storedData = dataDao.getData(collection.getId(), id);
      assertThat(storedData).isNotNull();
      assertThat(storedData).containsEntry(KEY1, VALUE1);
      assertThat(storedData).containsEntry(KEY2, VALUE2);
   }

   @Test
   public void testUpdateDocumentData() {
      Document document = createDocument();
      String id = document.getId();

      DataDocument data = new DataDocument(KEY1, VALUE2);

      LocalDateTime beforeUpdateTime = LocalDateTime.now();
      Document updatedDocument = documentFacade.updateDocumentData(collection.getCode(), id, data);
      assertThat(updatedDocument).isNotNull();

      Document storedDocument = documentDao.getDocumentById(id);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(storedDocument.getCollectionCode()).isNull();
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(USER);
      assertions.assertThat(storedDocument.getCreationDate()).isBeforeOrEqualTo(beforeUpdateTime);
      assertions.assertThat(storedDocument.getUpdatedBy()).isEqualTo(USER);
      assertions.assertThat(storedDocument.getUpdateDate()).isAfterOrEqualTo(beforeUpdateTime).isBeforeOrEqualTo(LocalDateTime.now());
      assertions.assertThat(storedDocument.getDataVersion()).isEqualTo(2);
      assertions.assertThat(storedDocument.getData()).isNull();
      assertions.assertAll();

      DataDocument storedData = dataDao.getData(collection.getId(), id);
      assertThat(storedData).isNotNull();
      assertThat(storedData).containsEntry(KEY1, VALUE2);
      assertThat(storedData).doesNotContainKey(KEY2);
   }

   @Test
   public void testPatchDocumentData() {
      Document document = createDocument();
      String id = document.getId();

      DataDocument data = new DataDocument(KEY1, VALUE2);

      LocalDateTime beforeUpdateTime = LocalDateTime.now();
      Document updatedDocument = documentFacade.patchDocumentData(collection.getCode(), id, data);
      assertThat(updatedDocument).isNotNull();

      Document storedDocument = documentDao.getDocumentById(id);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(storedDocument.getCollectionCode()).isNull();
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(USER);
      assertions.assertThat(storedDocument.getCreationDate()).isBeforeOrEqualTo(beforeUpdateTime);
      assertions.assertThat(storedDocument.getUpdatedBy()).isEqualTo(USER);
      assertions.assertThat(storedDocument.getUpdateDate()).isAfterOrEqualTo(beforeUpdateTime).isBeforeOrEqualTo(LocalDateTime.now());
      assertions.assertThat(storedDocument.getDataVersion()).isEqualTo(2);
      assertions.assertThat(storedDocument.getData()).isNull();
      assertions.assertAll();

      DataDocument storedData = dataDao.getData(collection.getId(), id);
      assertThat(storedData).isNotNull();
      assertThat(storedData).containsEntry(KEY1, VALUE2);
      assertThat(storedData).containsEntry(KEY2, VALUE2);
   }

   @Test
   public void testDeleteDocument() {
      String id = createDocument().getId();

      documentFacade.deleteDocument(collection.getCode(), id);

      assertThatThrownBy(() -> documentDao.getDocumentById(id))
            .isInstanceOf(ResourceNotFoundException.class);
      assertThatThrownBy(() -> dataDao.getData(collection.getId(), id))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetDocument() {
      String id = createDocument().getId();

      Document document = documentFacade.getDocument(collection.getCode(), id);
      assertThat(document).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(document.getId()).isEqualTo(id);
      assertions.assertThat(document.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(document.getCollectionCode()).isNull();
      assertions.assertThat(document.getCreatedBy()).isEqualTo(USER);
      assertions.assertThat(document.getCreationDate()).isBeforeOrEqualTo(LocalDateTime.now());
      assertions.assertThat(document.getUpdatedBy()).isNull();
      assertions.assertThat(document.getUpdateDate()).isNull();
      assertions.assertThat(document.getDataVersion()).isEqualTo(1);
      assertions.assertAll();

      DataDocument data = document.getData();
      assertThat(data).isNotNull();
      assertThat(data).containsEntry(KEY1, VALUE1);
      assertThat(data).containsEntry(KEY2, VALUE2);
   }

   @Test
   public void testGetDocuments() {
      String id1 = createDocument().getId();
      String id2 = createDocument().getId();

      Pagination pagination= new Pagination(null, null);
      List<Document> documents = documentFacade.getDocuments(collection.getCode(), pagination);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2);
   }
}
