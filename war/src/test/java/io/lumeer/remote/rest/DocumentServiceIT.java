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
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class DocumentServiceIT extends ServiceIntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";
   private static final String COLLECTION_CODE = "TCOLL";

   private static final String COLLECTION_NAME = "Testing collection";
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String KEY1 = "A";
   private static final String KEY2 = "B";
   private static final String VALUE1 = "firstValue";
   private static final String VALUE2 = "secondValue";

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String DOCUMENTS_PATH_PREFIX = "/" + PATH_CONTEXT + "/rest/" + "organizations/" + ORGANIZATION_CODE + "/projects/" + PROJECT_CODE + "/collections";
   private static final String DOCUMENTS_URL_PREFIX = SERVER_URL + DOCUMENTS_PATH_PREFIX;

   private User user;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   private Collection collection;

   @Before
   public void configureCollection() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      this.user = userDao.createUser(user);

      JsonPermissions organizationPermissions = new JsonPermissions();
      Permission userPermission = new SimplePermission(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      JsonProject project = new JsonProject();
      project.setCode(PROJECT_CODE);

      JsonPermissions projectPermissions = new JsonPermissions();
      projectPermissions.updateUserPermissions(new JsonPermission(this.user.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(storedProject);

      JsonPermissions collectionPermissions = new JsonPermissions();
      collectionPermissions.updateUserPermissions(new JsonPermission(this.user.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      JsonCollection jsonCollection = new JsonCollection(COLLECTION_CODE, COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
      collection = collectionDao.createCollection(jsonCollection);

      documentDao.setProject(storedProject);
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
      document.setCreatedBy(this.user.getId());
      document.setCreationDate(ZonedDateTime.now());
      document.setDataVersion(DocumentFacade.INITIAL_VERSION);
      Document storedDocument = documentDao.createDocument(document);

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), document.getData());

      storedDocument.setData(storedData);
      return storedDocument;
   }

   @Test
   public void testCreateDocument() {
      Document document = prepareDocument();
      Entity entity = Entity.json(document);

      ZonedDateTime beforeTime = ZonedDateTime.now();

      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      JsonDocument returnedDocument = response.readEntity(JsonDocument.class);
      assertThat(returnedDocument).isNotNull();

      String id = returnedDocument.getId();
      assertThat(id).isNotNull();
      assertThat(ObjectId.isValid(id)).isTrue();

      SoftAssertions returnedAssertions = new SoftAssertions();
      returnedAssertions.assertThat(returnedDocument.getCollectionId()).isEqualTo(collection.getId());
      returnedAssertions.assertThat(returnedDocument.getCreatedBy()).isEqualTo(this.user.getId());
      returnedAssertions.assertThat(returnedDocument.getCreationDate()).isAfterOrEqualTo(beforeTime).isBeforeOrEqualTo(ZonedDateTime.now());
      returnedAssertions.assertThat(returnedDocument.getUpdatedBy()).isNull();
      returnedAssertions.assertThat(returnedDocument.getUpdateDate()).isNull();
      returnedAssertions.assertThat(returnedDocument.getDataVersion()).isEqualTo(1);
      returnedAssertions.assertThat(returnedDocument.getData()).isEqualTo(document.getData());
      returnedAssertions.assertAll();

      Document storedDocument = documentDao.getDocumentById(id);
      assertThat(storedDocument).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(this.user.getId());
      assertions.assertThat(storedDocument.getCreationDate()).isAfterOrEqualTo(beforeTime).isBeforeOrEqualTo(ZonedDateTime.now());
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
   public void testUpdateDocument() {
      String id = createDocument().getId();

      DataDocument data = new DataDocument(KEY1, VALUE2);
      Entity entity = Entity.json(data);

      ZonedDateTime beforeUpdateTime = ZonedDateTime.now();
      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(id).path("data")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Document storedDocument = documentDao.getDocumentById(id);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(this.user.getId());
      assertions.assertThat(storedDocument.getCreationDate()).isBeforeOrEqualTo(beforeUpdateTime);
      assertions.assertThat(storedDocument.getUpdatedBy()).isEqualTo(this.user.getId());
      assertions.assertThat(storedDocument.getUpdateDate()).isAfterOrEqualTo(beforeUpdateTime).isBeforeOrEqualTo(ZonedDateTime.now());
      assertions.assertThat(storedDocument.getDataVersion()).isEqualTo(2);
      assertions.assertThat(storedDocument.getData()).isNull();
      assertions.assertAll();

      DataDocument storedData = dataDao.getData(collection.getId(), id);
      assertThat(storedData).isNotNull();
      assertThat(storedData).containsEntry(KEY1, VALUE2);
      assertThat(storedData).doesNotContainKey(KEY2);
   }

   @Test
   public void testPatchDocument() {
      String id = createDocument().getId();

      DataDocument data = new DataDocument(KEY1, VALUE2);
      Entity entity = Entity.json(data);

      ZonedDateTime beforeUpdateTime = ZonedDateTime.now();
      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(id).path("data")
                                .request(MediaType.APPLICATION_JSON)
                                .build("PATCH", entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Document storedDocument = documentDao.getDocumentById(id);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(collection.getId());
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(this.user.getId());
      assertions.assertThat(storedDocument.getCreationDate()).isBeforeOrEqualTo(beforeUpdateTime);
      assertions.assertThat(storedDocument.getUpdatedBy()).isEqualTo(this.user.getId());
      assertions.assertThat(storedDocument.getUpdateDate()).isAfterOrEqualTo(beforeUpdateTime).isBeforeOrEqualTo(ZonedDateTime.now());
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

      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(id)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      assertThatThrownBy(() -> documentDao.getDocumentById(id))
            .isInstanceOf(ResourceNotFoundException.class);
      assertThatThrownBy(() -> dataDao.getData(collection.getId(), id))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   @Ignore("Works manually but there is unexpected exception in tests")
   public void testGetDocument() {
      String id = createDocument().getId();

      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(id)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      JsonDocument document = response.readEntity(JsonDocument.class);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(document.getId()).isEqualTo(id);
      assertions.assertThat(document.getCollectionId()).isNull();
      assertions.assertThat(document.getCreatedBy()).isEqualTo(this.user.getId());
      assertions.assertThat(document.getCreationDate()).isBeforeOrEqualTo(ZonedDateTime.now());
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
   @Ignore("Works manually but there is unexpected exception in tests")
   public void testGetAllDocuments() {
      String id1 = createDocument().getId();
      String id2 = createDocument().getId();

      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonDocument> documents = response.readEntity(new GenericType<List<JsonDocument>>() {
      });
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2);
   }

   @Test
   public void testUpdateMetaData() {
      Document doc = createDocument();

      assertThat(doc.getMetaData()).isNull();
      doc.setMetaData(new DataDocument("testKey", "testValue"));

      Entity entity = Entity.json(doc.getMetaData());

      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(doc.getId()).path("meta")
            .request(MediaType.APPLICATION_JSON)
            .buildPut(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Document storedDocument = documentDao.getDocumentById(doc.getId());
      assertThat(storedDocument.getMetaData()).isNotNull().hasSize(1).containsOnlyKeys("testKey").containsValue("testValue");

      doc.setMetaData(new DataDocument("testKey2", "testValue2"));

      entity = Entity.json(doc.getMetaData());

      response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(doc.getId()).path("meta")
            .request(MediaType.APPLICATION_JSON)
            .buildPut(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      storedDocument = documentDao.getDocumentById(doc.getId());
      assertThat(storedDocument.getMetaData()).isNotNull().hasSize(1).containsOnlyKeys("testKey2").containsValue("testValue2");
   }

   @Test
   public void testPatchMetaData() {
      Document doc = createDocument();

      assertThat(doc.getMetaData()).isNull();
      doc.setMetaData(new DataDocument("testKey", "testValue"));

      Entity entity = Entity.json(doc.getMetaData());

      Response response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(doc.getId()).path("meta")
            .request(MediaType.APPLICATION_JSON)
            .build("PATCH", entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Document storedDocument = documentDao.getDocumentById(doc.getId());
      assertThat(storedDocument.getMetaData()).isNotNull().hasSize(1).containsOnlyKeys("testKey").containsValue("testValue");

      doc.setMetaData(new DataDocument("testKey2", "testValue2"));

      entity = Entity.json(doc.getMetaData());

      response = client.target(DOCUMENTS_URL_PREFIX).path(collection.getId()).path("documents").path(doc.getId()).path("meta")
            .request(MediaType.APPLICATION_JSON)
            .build("PATCH", entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      storedDocument = documentDao.getDocumentById(doc.getId());
      assertThat(storedDocument.getMetaData()).isNotNull().hasSize(2).containsOnlyKeys("testKey", "testKey2").containsValues("testValue", "testValue2");
   }
}
