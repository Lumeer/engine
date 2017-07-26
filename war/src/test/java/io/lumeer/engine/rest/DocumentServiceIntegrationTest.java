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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DatabaseInitializer;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.DocumentMetadataFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;

import org.bson.types.Decimal128;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
   private Logger log;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   @UserDataStorage
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
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   private final String TARGET_URI = "http://localhost:8080";

   private final String COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT = "DocumentServiceCollectionCreateReadUpdateAndDropDocument";
   private final String COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA = "DocumentServiceCollectionAddReadAndUpdateDocumentMetadata";
   private final String COLLECTION_SEARCH_HISTORY_CHANGES = "DocumentServiceCollectionSearchHistoryChanges";
   private final String COLLECTION_REVERT_DOCUMENT_VERSION = "DocumentServiceCollectionRevertDocumentVersion";
   private final String COLLECTION_ATTRIBUTE_TYPES = "DocumentServiceCollectionAttributeTypes";

   private Client[] clients = new Client[3];

   @Before
   public void init() {
      // I (Alica) suppose we operate inside some default project which has not been initialized, so we do that here
      databaseInitializer.onProjectCreated(projectFacade.getCurrentProjectCode());

      ClientBuilder builder = ClientBuilder.newBuilder();
      for (int i = 0; i < clients.length; i++) {
         clients[i] = builder.build();
      }
   }

   @After
   public void closeClients() {
      for (Client client : clients) {
         client.close();
      }
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(documentFacade).isNotNull();
   }

   @Test
   public void testCreateReadUpdateAndDropDocument() throws Exception {
      setUpCollections(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT);

      String code = collectionFacade.createCollection(new Collection(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT));

      String[] documentIds = new String[clients.length];

      // 200 - the document will be inserted into the given collection
      for (int i = 0; i < clients.length; i++) {
         Response response = clients[i].target(TARGET_URI)
                                       .path(setPathPrefix(code))
                                       .request()
                                       .buildPost(Entity.json(
                                             new DataDocument("first_attribute", i)
                                                   .append("second_attribute", "test_value")
                                                   .append("third_attribute", "some_test " + i)
                                                   .append("fourth_attribute", null)))
                                       .invoke();

         documentIds[i] = response.readEntity(String.class);
         assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
         assertThat(documentFacade.readDocument(code, documentIds[i])).isNotNull();
         response.close();
      }

      // 200 - read the given document by its id
      for (int i = 0; i < clients.length; i++) {
         Response response = clients[i].target(TARGET_URI)
                                       .path(setPathPrefix(code) + documentIds[i])
                                       .request()
                                       .buildGet()
                                       .invoke();

         DataDocument document = response.readEntity(DataDocument.class);
         assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
         assertThat(documentFacade.readDocument(code, documentIds[i])).isEqualTo(document);
         response.close();
      }

      // 204 - update the document
      for (int i = 0; i < clients.length; i++) {
         Response response = clients[i].target(TARGET_URI)
                                       .path(setPathPrefix(code) + "update/")
                                       .request()
                                       .buildPut(Entity.json(new DataDocument()
                                             .append("_id", documentIds[i])
                                             .append("name", "updatedDocument" + i)))
                                       .invoke();

         assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
         assertThat(documentFacade.readDocument(code, documentIds[i]).getString("name")).isEqualTo("updatedDocument" + i);
         response.close();
      }

      // 204 - drop the given document by its id
      for (int i = 0; i < clients.length; i++) {
         Response response = clients[i].target(TARGET_URI)
                                       .path(setPathPrefix(code) + documentIds[i])
                                       .request()
                                       .buildDelete()
                                       .invoke();

         assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
         response.close();
      }
   }

   @Test
   public void testAttributeTypes() throws Exception {
      final DataDocument doc = new DataDocument().append("dbl", 2.34).append("str", "hello")
                                                 .append("set", new HashSet(Arrays.asList("huu", "hooo")))
                                                 .append("lst", Arrays.asList("a", "b", "C"))
                                                 .append("date", new Date(35)).append("bool", true)
                                                 .append("int", 42).append("decimal", new BigDecimal("35.03535"));

      setUpCollections(COLLECTION_ATTRIBUTE_TYPES);
      final Client client = ClientBuilder.newBuilder().build();

      String code = collectionFacade.createCollection(new Collection(COLLECTION_ATTRIBUTE_TYPES));

      Response response = client.target(TARGET_URI).path(setPathPrefix(code)).request().buildPost(Entity.json(doc)).invoke();
      String documentId = response.readEntity(String.class);

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      response.close();

      final DataDocument responseDoc = documentFacade.readDocument(code, documentId);
      assertThat(responseDoc).isNotNull();
      assertThat(responseDoc.getDouble("dbl")).isInstanceOf(Double.class).isEqualTo(2.34);
      assertThat(responseDoc.getString("str")).isInstanceOf(String.class).isEqualTo("hello");
      assertThat(responseDoc.getArrayList("set", String.class)).isInstanceOf(List.class).contains("hooo", "huu");
      assertThat(responseDoc.getArrayList("lst", String.class)).isInstanceOf(List.class).contains("a", "b", "C");
      assertThat(responseDoc.getInteger("date")).isEqualTo(35); // should be long
      assertThat(responseDoc.getBoolean("bool")).isEqualTo(true);
      assertThat(responseDoc.getInteger("int")).isEqualTo(42);
      assertThat(responseDoc.getDouble("decimal")).isEqualTo(35.03535); // should be decimal

      log.info("@@@@@@@@@@@");
      responseDoc.forEach((k, v) ->
            log.info(k + " (" + v.getClass().getName() + ") = " + v.toString())
      );

      String nId = documentFacade.createDocument(code, doc);
      final DataDocument readDoc = documentFacade.readDocument(code, nId);

      assertThat(readDoc.getDouble("dbl")).isInstanceOf(Double.class).isEqualTo(2.34);
      assertThat(readDoc.getString("str")).isInstanceOf(String.class).isEqualTo("hello");
      assertThat(readDoc.getArrayList("set", String.class)).isInstanceOf(List.class).contains("hooo", "huu");
      assertThat(readDoc.getArrayList("lst", String.class)).isInstanceOf(List.class).contains("a", "b", "C");
      assertThat(readDoc.getDate("date")).isEqualTo(new Date(35));
      assertThat(readDoc.getBoolean("bool")).isEqualTo(true);
      assertThat(readDoc.getInteger("int")).isEqualTo(42);
      assertThat(readDoc.getObject("decimal")).isInstanceOf(Decimal128.class).isEqualTo(new Decimal128(new BigDecimal("35.03535"))); // should be decimal

      log.info("@@@@@@@@@@@");
      readDoc.forEach((k, v) ->
            log.info(k + " (" + v.getClass().getName() + ") = " + v.toString())
      );
   }

   @Test
   public void testAddReadAndUpdateDocumentMetadata() throws Exception {
      setUpCollections(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA);
      final Client client = ClientBuilder.newBuilder().build();
      final String attributeName = "_meta-update-user";
      final Object metaObjectValue = 123;

      String code = collectionFacade.createCollection(new Collection(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA));
      String documentId = documentFacade.createDocument(code, new DataDocument());

      // 204 - add the document metadata
      Response response = client.target(TARGET_URI).path(setPathPrefix(code) + documentId + "/meta/" + attributeName).request().buildPost(Entity.entity(metaObjectValue, MediaType.APPLICATION_JSON)).invoke();
      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(code, documentId);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(documentMetadata.get(attributeName)).isEqualTo(metaObjectValue);
      response.close();

      // 200 - read the document metadata
      Response response2 = client.target(TARGET_URI).path(setPathPrefix(code) + documentId + "/meta/").request().buildGet().invoke();
      Map<String, Object> metaDocument = response2.readEntity(Map.class);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(metaDocument).isEqualTo(documentMetadata);
      response2.close();

      // 204 - update the document metadata
      DataDocument updatedMetaDocument = new DataDocument();
      updatedMetaDocument.put(attributeName, "updatedValue");
      Response response3 = client.target(TARGET_URI).path(setPathPrefix(code) + documentId + "/meta").request().buildPut(Entity.json(updatedMetaDocument)).invoke();
      assertThat(response3.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(documentMetadataFacade.readDocumentMetadata(code, documentId).get(attributeName)).isEqualTo("updatedValue");
      response3.close();

      client.close();
   }

   @Test
   public void testSearchHistoryChanges() throws Exception {
      setUpCollections(COLLECTION_SEARCH_HISTORY_CHANGES);
      final Client client = ClientBuilder.newBuilder().build();

      String code = collectionFacade.createCollection(new Collection(COLLECTION_SEARCH_HISTORY_CHANGES));
      String documentId = documentFacade.createDocument(code, new DataDocument());

      // only document exists, no changes in the past, code 200, listsize = 1
      Response response = client.target(TARGET_URI).path(setPathPrefix(code) + documentId + "/versions/").request().buildGet().invoke();
      List<DataDocument> changedDocuments = response.readEntity(new GenericType<List<DataDocument>>() {
      });
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(changedDocuments).hasSize(1);
      response.close();

      // two changes performed in the past + original document, code 200, listsize = 3
      DataDocument documentVersionOne = new DataDocument("_id", documentId);
      DataDocument documentVersionTwo = new DataDocument("_id", documentId);
      documentVersionOne.put("dummyVersionOneAttribute", 1);
      documentVersionTwo.put("dummyVersionTwoAttribute", 2);
      documentFacade.updateDocument(code, documentVersionOne);
      documentFacade.updateDocument(code, documentVersionTwo);

      Response response2 = client.target(TARGET_URI).path(setPathPrefix(code) + documentId + "/versions/").request().buildGet().invoke();
      List<DataDocument> changedDocuments2 = response2.readEntity(new GenericType<List<DataDocument>>() {
      });
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(changedDocuments2).hasSize(3);
      response2.close();

      client.close();
   }

   @Test
   public void testRevertDocumentVersion() throws Exception {
      setUpCollections(COLLECTION_REVERT_DOCUMENT_VERSION);
      final Client client = ClientBuilder.newBuilder().build();

      String code = collectionFacade.createCollection(new Collection(COLLECTION_REVERT_DOCUMENT_VERSION));

      String documentId = documentFacade.createDocument(code, new DataDocument());
      DataDocument documentVersionOne = new DataDocument("_id", documentId);
      DataDocument documentVersionTwo = new DataDocument("_id", documentId);
      documentVersionOne.put("dummyVersionOneAttribute", 1);
      documentVersionTwo.put("dummyVersionTwoAttribute", 2);
      documentFacade.updateDocument(code, documentVersionOne);
      documentFacade.updateDocument(code, documentVersionTwo);

      DataDocument documentVersion2 = documentFacade.readDocument(code, documentId);
      int versionTwo = documentVersion2.getInteger(LumeerConst.Document.METADATA_VERSION_KEY);
      assertThat(versionTwo).isEqualTo(2);

      Response response = client.target(TARGET_URI).path(setPathPrefix(code) + documentId + "/versions/" + 1).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      DataDocument currentDocument = documentFacade.readDocument(code, documentId);
      int versionThree = currentDocument.getInteger(LumeerConst.Document.METADATA_VERSION_KEY);
      boolean isFirstVersion = !currentDocument.containsKey("dummyVersionTwoAttribute");
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(versionThree).isEqualTo(3);
      assertThat(isFirstVersion).isTrue();
      response.close();

      client.close();
   }

   private void setUpCollections(final String collectionName) throws DbException {
      String code = collectionMetadataFacade.getCollectionCodeFromName(collectionName);
      if (dataStorage.hasCollection(code)) {
         collectionFacade.dropCollection(code);
      }
   }

   private String setPathPrefix(final String collectionCode) {
      return PATH_CONTEXT + "/rest/organizations/" + organizationFacade.getOrganizationCode() + "/projects/" + projectFacade.getCurrentProjectCode() + "/collections/" + collectionCode + "/documents/";
   }

}
