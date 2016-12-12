/*
 * -----------------------------------------------------------------------\
 * Lummer
 *  
 * Copyright (C) 2016 the original author or authors.
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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests the collection service while deployed on the application server.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class CollectionServiceTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionServiceTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String TARGET_URI = "http://localhost:8080";
   private final String PATH_PREFIX = "CollectionServiceTest/rest/collections/";

   private final String DUMMY_COLLECTION_1 = "testCollection1";
   private final String DUMMY_COLLECTION_2 = "testCollection2";
   private final String DUMMY_COLLECTION_3 = "newTestCollection";

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionService collectionService;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Test
   public void testRegister() throws Exception {
      Assert.assertNotNull(collectionService);
   }

   @Test
   public void testRestClient() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      // the path prefix '/lumeer-engine/' does not work in test classes
      Response response = client.target(TARGET_URI).path("/lumeer-engine/rest/collections/").request(MediaType.APPLICATION_JSON_TYPE).buildGet().invoke();
   }

   @Test
   public void testGetAllCollections() throws Exception {
      dropUsedCollections();
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      collectionFacade.createCollection(DUMMY_COLLECTION_2);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();

      ArrayList<String> collections = response.readEntity(ArrayList.class);
      Assert.assertTrue(collections.equals(new ArrayList<String>(collectionFacade.getAllCollections().keySet())) && response.getStatus() == Response.Status.OK.getStatusCode());
      response.close();
      client.close();
   }

   @Test
   public void testCreateCollection() throws Exception {
      dropUsedCollections();

      // #1 first time collection creation, status code = 200
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      String internalName = response.readEntity(String.class);
      Assert.assertTrue(dataStorage.hasCollection(internalName) && response.getStatus() == Response.Status.OK.getStatusCode() && internalName.equals(internalName(DUMMY_COLLECTION_1)));
      response.close();

      // #2 collection already exists, status code = 400
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Assert.assertTrue(dataStorage.hasCollection(internalName) && response2.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
      client.close();
      response2.close();
   }

   @Test
   public void testDropCollection() throws Exception {
      dropUsedCollections();

      // #1 nothing to delete, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1).request().buildDelete().invoke();
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 collection exists, ready to delete, status code = 204
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1).request().buildDelete().invoke();
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1)) && response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode());
      client.close();
      response2.close();
   }

   @Test
   public void testDropAttribute() throws Exception {
      dropUsedCollections();
      final String requestBody = "testAttributeToAdd";

      // #1 the given collection does not exist, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/attributes/").request().method("DELETE", Entity.entity(requestBody, MediaType.APPLICATION_JSON));
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attribute exists, ready to drop the attribute, status code = 204
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      collectionMetadataFacade.addOrIncrementAttribute(internalName(DUMMY_COLLECTION_1), requestBody);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/attributes/").request().method("DELETE", Entity.entity(requestBody, MediaType.APPLICATION_JSON));
      Assert.assertTrue(dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && !collectionMetadataFacade.getCollectionAttributesNames(internalName(DUMMY_COLLECTION_1)).contains(requestBody));
      client.close();
      response2.close();
   }

   @Test
   public void testRenameAttribute() throws Exception {
      dropUsedCollections();

      final String oldAttributeName = "testAttribute";
      final String newAttributeName = "testNewAttribute";
      final String dummyAttributeName = "dummyTestAttribute";

      // #1 the given collection does not exist, status code = 400 or 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/attributes/rename/" + oldAttributeName + "/" + newAttributeName).request().buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attribute exists, ready to rename the attribute, status code = 204
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      collectionMetadataFacade.addOrIncrementAttribute(internalName(DUMMY_COLLECTION_1), oldAttributeName);
      collectionMetadataFacade.addOrIncrementAttribute(internalName(DUMMY_COLLECTION_1), dummyAttributeName);

      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/attributes/rename/" + oldAttributeName + "/" + newAttributeName).request().buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Assert.assertTrue(dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && collectionMetadataFacade.getCollectionAttributesNames(internalName(DUMMY_COLLECTION_1)).contains(newAttributeName)
            && !collectionMetadataFacade.getCollectionAttributesNames(internalName(DUMMY_COLLECTION_1)).contains(oldAttributeName));
      client.close();
      response2.close();
   }

   @Test
   public void testQuerySearch() throws Exception {
      // TODO:
   }

   @Test
   public void testOptionSearch() throws Exception {
      // TODO:
   }

   @Test
   public void testAddCollectionMetadata() throws Exception {
      // TODO:
   }

   @Test
   public void testReadCollectionMetadata() throws Exception {
      dropUsedCollections();

      // #1 no metadata collection exists, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1)) && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the metadata collection of the given collection exists
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      ArrayList<DataDocument> collectionMetadata = response2.readEntity(ArrayList.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode() && collectionMetadata.equals(collectionFacade.readCollectionMetadata(internalName(DUMMY_COLLECTION_1))));
      response2.close();
      client.close();
   }

   @Test
   public void testUpdateCollectionMetadata() throws Exception {
      // TODO:
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
      dropUsedCollections();

      final String dummyAttribute1 = "dummyAttribute1";
      final String dummyAttribute2 = "dummyAttribute2";
      final String dummyAttribute3 = "dummyAttribute3";

      // #1 if the given collection does not exist, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attributes exists, status code = 200
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      collectionMetadataFacade.addOrIncrementAttribute(internalName(DUMMY_COLLECTION_1), dummyAttribute1);
      collectionMetadataFacade.addOrIncrementAttribute(internalName(DUMMY_COLLECTION_1), dummyAttribute2);
      collectionMetadataFacade.addOrIncrementAttribute(internalName(DUMMY_COLLECTION_1), dummyAttribute3);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      ArrayList<String> collectionAttributes = response2.readEntity(ArrayList.class);
      Assert.assertTrue(dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response2.getStatus() == Response.Status.OK.getStatusCode()
            && collectionAttributes.equals(collectionFacade.readCollectionAttributes(internalName(DUMMY_COLLECTION_1))));
      client.close();
      response2.close();
   }

   @Test
   public void testGetDocumentVersion() throws Exception {
      dropUsedCollections();

      String documentId;
      int versionId;

      // #1 if the given collection does not exist, status code = 404
     /* final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + DUMMY_COLLECTION_1 + "/documents/" + documentId + "versions/" + versionId).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Assert.assertTrue(!dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attributes exists, status code = 200
      collectionFacade.createCollection(DUMMY_COLLECTION_1);
      DataDocument document = new DataDocument();
      document.put("dummyKey", "dummyValue");
      documentFacade.createDocument(DUMMY_COLLECTION_1, document);*/
      // TODO:

      /* Response response2 = client.target("http://localhost:8080").path("CollectionServiceTest/rest/collections/" + DUMMY_COLLECTION_1 + "/documents/" + documentId + "/versions/" + versionId).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      ArrayList<String> collectionAttributes = response2.readEntity(ArrayList.class);
      Assert.assertTrue(dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))
            && response2.getStatus() == Response.Status.OK.getStatusCode()
            && collectionAttributes.equals(collectionFacade.readCollectionAttributes(internalName(DUMMY_COLLECTION_1))));
      client.close();
      response2.close();*/
   }

   @Test
   public void testReadAccessRights() throws Exception {
      // TODO:
   }

   @Test
   public void testUpdateAccessRights() throws Exception {
      // TODO:
   }

   private void dropUsedCollections() throws CollectionNotFoundException {
      if (dataStorage.hasCollection(internalName(DUMMY_COLLECTION_1))) {
         collectionFacade.dropCollection(internalName(DUMMY_COLLECTION_1));
      }

      if (dataStorage.hasCollection(internalName(DUMMY_COLLECTION_2))) {
         collectionFacade.dropCollection(internalName(DUMMY_COLLECTION_2));
      }

      if (dataStorage.hasCollection(internalName(DUMMY_COLLECTION_3))) {
         collectionFacade.dropCollection(internalName(DUMMY_COLLECTION_3));
      }

   }

   private String internalName(String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }
}
