/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.DocumentMetadataFacade;
import io.lumeer.engine.controller.OrganisationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.provider.DataStorageProvider;
import io.lumeer.engine.rest.dao.AccessRightsDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class DocumentServiceIntegrationTest extends IntegrationTestBase {

   @Inject
   private CollectionFacade collectionFacade;

   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private OrganisationFacade organisationFacade;

   @Inject
   private ProjectFacade projectFacade;

   private final String TARGET_URI = "http://localhost:8080";

   private final String COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT = "DocumentServiceCollectionCreateReadUpdateAndDropDocument";
   private final String COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA = "DocumentServiceCollectionAddReadAndUpdateDocumentMetadata";
   private final String COLLECTION_SEARCH_HISTORY_CHANGES = "DocumentServiceCollectionSearchHistoryChanges";
   private final String COLLECTION_REVERT_DOCUMENT_VERSION = "DocumentServiceCollectionRevertDocumentVersion";
   private final String COLLECTION_READ_ACCESS_RIGHTS = "DocumentServiceCollectionrReadAccessRights";
   private final String COLLECTION_UPDATE_ACCESS_RIGHTS = "DocumentServiceCollectionrUpdateAccessRights";

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Before
   public void init() {
      dataStorage = dataStorageProvider.getUserStorage();
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(documentFacade).isNotNull();
   }

   @Test
   public void testCreateReadUpdateAndDropDocument() throws Exception {
      setUpCollections(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT);
      final Client client = ClientBuilder.newBuilder().build();

      // 200 - the document will be inserted into the given collection
      collectionFacade.createCollection(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT);
      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT)).request().buildPost(Entity.json(new DataDocument())).invoke();
      String documentId = response.readEntity(String.class);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(documentFacade.readDocument(getInternalName(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT), documentId)).isNotNull();
      response.close();

      // 200 - read the given document by its id
      Response response2 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT) + documentId).request().buildGet().invoke();
      DataDocument document = response2.readEntity(DataDocument.class);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(documentFacade.readDocument(getInternalName(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT), documentId)).isEqualTo(document);
      response2.close();

      // 204 - update the document
      DataDocument updatedDocument = new DataDocument();
      updatedDocument.put("_id", documentId);
      updatedDocument.put("name", "updatedDocument");
      Response response3 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT) + "update/").request().buildPut(Entity.json(updatedDocument)).invoke();
      assertThat(response3.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(documentFacade.readDocument(getInternalName(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT), documentId).getString("name")).isEqualTo("updatedDocument");
      response3.close();

      // 204 - drop the given document by its id
      Response response4 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT) + documentId).request().buildDelete().invoke();
      assertThat(response4.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response4.close();

      client.close();
   }

   @Test
   public void testAddReadAndUpdateDocumentMetadata() throws Exception {
      setUpCollections(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA);
      final Client client = ClientBuilder.newBuilder().build();
      final String attributeName = "_meta-update-user";
      final Object metaObjectValue = 123;

      collectionFacade.createCollection(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA), new DataDocument());

      // 204 - add the document metadata
      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA) + documentId + "/meta/" + attributeName).request().buildPost(Entity.entity(metaObjectValue, MediaType.APPLICATION_JSON)).invoke();
      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(getInternalName(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA), documentId);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(documentMetadata.get(attributeName)).isEqualTo(metaObjectValue);
      response.close();

      // 200 - read the document metadata
      Response response2 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA) + documentId + "/meta/").request().buildGet().invoke();
      Map<String, Object> metaDocument = response2.readEntity(Map.class);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(metaDocument).isEqualTo(documentMetadata);
      response2.close();

      // 204 - update the document metadata
      DataDocument updatedMetaDocument = new DataDocument();
      updatedMetaDocument.put(attributeName, "updatedValue");
      Response response3 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA) + documentId + "/meta").request().buildPut(Entity.json(updatedMetaDocument)).invoke();
      assertThat(response3.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(documentMetadataFacade.readDocumentMetadata(getInternalName(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA), documentId).get(attributeName)).isEqualTo("updatedValue");
      response3.close();

      client.close();
   }

   @Test
   public void testSearchHistoryChanges() throws Exception {
      setUpCollections(COLLECTION_SEARCH_HISTORY_CHANGES);
      final Client client = ClientBuilder.newBuilder().build();

      collectionFacade.createCollection(COLLECTION_SEARCH_HISTORY_CHANGES);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_SEARCH_HISTORY_CHANGES), new DataDocument());

      // only document exists, no changes in the past, code 200, listsize = 1
      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_SEARCH_HISTORY_CHANGES) + documentId + "/versions/").request().buildGet().invoke();
      List<DataDocument> changedDocuments = response.readEntity(ArrayList.class);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(changedDocuments).hasSize(1);
      response.close();

      // two changes performed in the past + original document, code 200, listsize = 3
      DataDocument documentVersionOne = new DataDocument("_id", documentId);
      DataDocument documentVersionTwo = new DataDocument("_id", documentId);
      documentVersionOne.put("dummyVersionOneAttribute", 1);
      documentVersionTwo.put("dummyVersionTwoAttribute", 2);
      documentFacade.updateDocument(getInternalName(COLLECTION_SEARCH_HISTORY_CHANGES), documentVersionOne);
      documentFacade.updateDocument(getInternalName(COLLECTION_SEARCH_HISTORY_CHANGES), documentVersionTwo);

      Response response2 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_SEARCH_HISTORY_CHANGES) + documentId + "/versions/").request().buildGet().invoke();
      List<DataDocument> changedDocuments2 = response2.readEntity(ArrayList.class);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(changedDocuments2).hasSize(3);
      response2.close();

      client.close();
   }

   @Test
   public void testRevertDocumentVersion() throws Exception {
      setUpCollections(COLLECTION_REVERT_DOCUMENT_VERSION);
      final Client client = ClientBuilder.newBuilder().build();

      collectionFacade.createCollection(COLLECTION_REVERT_DOCUMENT_VERSION);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), new DataDocument());
      DataDocument documentVersionOne = new DataDocument("_id", documentId);
      DataDocument documentVersionTwo = new DataDocument("_id", documentId);
      documentVersionOne.put("dummyVersionOneAttribute", 1);
      documentVersionTwo.put("dummyVersionTwoAttribute", 2);
      documentFacade.updateDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentVersionOne);
      documentFacade.updateDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentVersionTwo);

      DataDocument documentVersion2 = documentFacade.readDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentId);
      int versionTwo = documentVersion2.getInteger(LumeerConst.Document.METADATA_VERSION_KEY);
      assertThat(versionTwo).isEqualTo(2);

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_REVERT_DOCUMENT_VERSION) + documentId + "/versions/" + 1).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      DataDocument currentDocument = documentFacade.readDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentId);
      int versionThree = currentDocument.getInteger(LumeerConst.Document.METADATA_VERSION_KEY);
      boolean isFirstVersion = !currentDocument.containsKey("dummyVersionTwoAttribute");
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(versionThree).isEqualTo(3);
      assertThat(isFirstVersion).isTrue();
      response.close();

      client.close();
   }

   @Test
   public void testReadAccessRights() throws Exception {
      setUpCollections(COLLECTION_READ_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao DEFAULT_ACCESS_RIGHT = new AccessRightsDao(true, true, true, user);

      collectionFacade.createCollection(COLLECTION_READ_ACCESS_RIGHTS);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_READ_ACCESS_RIGHTS), new DataDocument());

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_READ_ACCESS_RIGHTS) + documentId + "/rights").request().buildGet().invoke();
      List<AccessRightsDao> rights = response.readEntity(new GenericType<List<AccessRightsDao>>() {
      });
      AccessRightsDao readRights = rights.get(0);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(readRights.isRead()).isEqualTo(DEFAULT_ACCESS_RIGHT.isRead());
      assertThat(readRights.isWrite()).isEqualTo(DEFAULT_ACCESS_RIGHT.isWrite());
      assertThat(readRights.isExecute()).isEqualTo(DEFAULT_ACCESS_RIGHT.isExecute());
      assertThat(readRights.getUserName()).isEqualTo(DEFAULT_ACCESS_RIGHT.getUserName());

      response.close();
      client.close();
   }

   @Test
   public void testUpdateAccessRights() throws Exception {
      setUpCollections(COLLECTION_UPDATE_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao accessRightsDao = new AccessRightsDao(false, false, true, user);

      collectionFacade.createCollection(COLLECTION_UPDATE_ACCESS_RIGHTS);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_UPDATE_ACCESS_RIGHTS), new DataDocument());

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_UPDATE_ACCESS_RIGHTS) + documentId + "/rights").request().buildPut(Entity.entity(accessRightsDao, MediaType.APPLICATION_JSON)).invoke();
      AccessRightsDao readAccessRights = securityFacade.getDao(getInternalName(COLLECTION_UPDATE_ACCESS_RIGHTS), documentId, user);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readAccessRights.isRead()).isFalse();
      assertThat(readAccessRights.isWrite()).isFalse();
      assertThat(readAccessRights.isExecute()).isTrue();
      response.close();
      client.close();
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(getInternalName(collectionName))) {
         collectionFacade.dropCollection(getInternalName(collectionName));
      }
   }

   private String setPathPrefix(final String collectionName) {
      return PATH_CONTEXT + "/rest/" + organisationFacade.getOrganisationId() + "/" + projectFacade.getProjectId() + "/collections/" + collectionName + "/documents/";
   }

   private String getInternalName(final String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

}
