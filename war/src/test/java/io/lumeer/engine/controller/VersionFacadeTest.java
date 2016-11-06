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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com>Jan Kotrady</a>
 */
public class VersionFacadeTest extends Arquillian {

   //test not working, because not getting correct id from mongoDbStorage
   //need to create new mongoDbStorage method createDocument (for example cd) which return some constant id
   //and use it in line 85 in VersioNFacade class
   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   private final String collectionTest = "tt";
   private final String collectionShadow = "tt.shadow";
   private final String versionString = "_metadata-version";
   private final String updaterString = "_metadata-updater";
   private final String documentIdString = "_id";

   @Inject
   private VersionFacade versionFacade;
   @Inject
   private DataStorage dataStorage;
   @Inject CollectionFacade collectionFacade;

   @Test
   public void testGetVersion() throws Exception {
      dataStorage.createCollection(collectionTest);
      DataDocument dataDocument = createTestDocument();
      String documentId = dataStorage.createDocument(collectionTest, dataDocument);
      Assert.assertEquals(versionFacade.getDocumentVersion(collectionTest,documentId),1);
      dataStorage.dropDocument(collectionTest, documentId);
      dataStorage.dropCollection(collectionTest);
   }

   @Test
   public void testGetVersionFromDocument() throws Exception {
      DataDocument dataDocument = createTestDocument();
      Assert.assertEquals(versionFacade.getDocumentVersion(dataDocument),1);
   }

   @Test
   public void testNewVersion() throws Exception {
      dataStorage.createCollection(collectionTest);
      dataStorage.createCollection(collectionShadow);
      DataDocument dataDocument = createTestDocument();
      String documentId = dataStorage.createDocument(collectionTest, dataDocument);
      versionFacade.newDocumentVersion(collectionTest,dataStorage.readDocument(collectionTest,documentId));
      Assert.assertEquals((int)dataStorage.readDocument(collectionTest,documentId).getInteger(versionString), 2);
      Assert.assertEquals((int)dataStorage.search(collectionShadow,null,null,0,1).get(0).getInteger(versionString),1);
      dataStorage.dropCollection(collectionTest);
      dataStorage.dropCollection(collectionShadow);
   }

   @Test
   public void testWithoutMeta() throws Exception{
      dataStorage.createCollection(collectionTest);
      String documentId = dataStorage.createDocument(collectionTest, createEmptyDocument());
      versionFacade.newDocumentVersion(collectionTest, dataStorage.readDocument(collectionTest,documentId));
      Assert.assertEquals((int)dataStorage.readDocument(collectionTest,documentId).getInteger(versionString), 1);
      Assert.assertEquals((int)dataStorage.search(collectionShadow,null,null,0,1).get(0).getInteger(versionString),0);
      dataStorage.dropCollection(collectionTest);
      dataStorage.dropCollection(collectionShadow);
   }

   @Test
   public void testNewVersionById() throws Exception {
      dataStorage.createCollection(collectionShadow);
      DataDocument dataDocument = createTestDocument();
      String documentId = dataStorage.createDocument(collectionTest, dataDocument);
      dataDocument = dataStorage.readDocument(collectionTest,documentId);
      dataDocument.replace("dog","pig");
      versionFacade.newDocumentVersionById(collectionTest,dataDocument);
      Assert.assertEquals((int)dataStorage.readDocument(collectionTest,documentId).getInteger(versionString), 2);
      Assert.assertEquals(dataStorage.readDocument(collectionTest,documentId).getString("dog"),"pig");
      Assert.assertEquals((int)dataStorage.search(collectionShadow,null,null,0,1).get(0).getInteger(versionString),1);
      Assert.assertEquals(dataStorage.search(collectionShadow,null,null,0,1).get(0).getString("dog"),"cat");
      dataStorage.dropCollection(collectionTest);
      dataStorage.dropCollection(collectionShadow);
   }
   /*@Test
   public void verifyUpdateTest() throws Exception{
      dataStorage.createCollection(collectionTest);
      dataStorage.createCollection(collectionShadow);
      DataDocument dataDocument = createTestDocument();
      String documentId = dataStorage.createDocument(collectionTest, dataDocument);
      DataDocument dataDocumentWithID = dataStorage.readDocument(collectionTest,documentId);
      versionFacade.newDocumentVersion(collectionTest,dataDocumentWithID);
      List<DataDocument> searchDocuments = dataStorage.search(collectionShadow, null, null, 0, 1);
      versionFacade.verifyDocumentUpdate(collectionShadow, searchDocuments.get(0));
      dataStorage.dropCollection(collectionTest);
      dataStorage.dropCollection(collectionShadow);
   }

   @Test
   public void revertTest() throws Exception{
      //cannot do
   }
   ...

   */

   private DataDocument createTestDocument() {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put("dog", "cat");
      dataDocument.put(versionString, 1);
      dataDocument.put(updaterString, "user1");
      return dataDocument;
   }

   private DataDocument createEmptyDocument() {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put("dog", "cat");
      return dataDocument;
   }
}