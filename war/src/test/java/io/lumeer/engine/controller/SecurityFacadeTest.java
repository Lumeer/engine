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

import io.lumeer.engine.api.LumeerConst;
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

import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com>Jan Kotrady</a>
 */
public class SecurityFacadeTest extends Arquillian {

   /*
   Tieto testy som nepisal optimalne, ale tak,
   aby boli napisane cim skor ... takze ak raz
   bude cas ich upravit, tak ich upravim ...
   Ale tieto testy su kvalitne, testuju poriadne
   pridavanie prav.
    */
   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "VersionFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String SECURITY_TEST_COLLECTION_READ = "securityTestCollectionRead";
   private final String SECURITY_TEST_COLLECTION_WRITE = "securityTestCollectionWrite";
   private final String SECURITY_TEST_COLLECTION_EXECUTE = "securityTestCollectionExecute";
   private final String SECURITY_TEST_COLLECTION_ADD_RIGHTS = "securityTestCollectionAddRights";

   @Inject
   public DataStorage dataStorage;

   @Inject
   public SecurityFacade securityFacade;

   @Inject
   public UserFacade userFacade;

   @Test
   public void testCheckForReadDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, "testUSER"));
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser4", 4);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser5", 5);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser6", 6);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, "testUSER"));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, "newTestUser4"));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, "newTestUser5"));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, "newTestUser6"));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 2);
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, "testUSER"));
   }

   @Test
   public void testCheckForWriteDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, "testUSER"));
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser2", 2);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser3", 3);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser6", 6);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, "testUSER"));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, "newTestUser2"));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, "newTestUser3"));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, "newTestUser6"));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 4);
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, "testUSER"));
   }

   @Test
   public void testCheckForExecuteDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, "testUSER"));
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser1", 1);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser3", 3);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser5", 5);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, "testUSER"));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, "newTestUser1"));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, "newTestUser3"));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, "newTestUser5"));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 2);
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, "testUSER"));
   }

   @Test
   public void testCheckForAddRightsDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, "testUSER"));
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser1", 1);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser3", 3);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser5", 5);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      Assert.assertFalse(securityFacade.checkForAddRights(dataDocument, "testUSER"));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, "newTestUser1"));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, "newTestUser3"));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, "newTestUser5"));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 2);
      Assert.assertFalse(securityFacade.checkForAddRights(dataDocument, "testUSER"));
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, "testUSER");
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, "testUSER"));
   }

   @Test
   public void testCheckForRead() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser4", 4);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser5", 5);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser6", 6);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_READ)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_READ);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_READ, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_READ, id);
      Assert.assertFalse(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, "testUSER"));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, "newTestUser4"));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, "newTestUser5"));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, "newTestUser6"));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_READ, dataDocument, id, -1);
      Assert.assertFalse(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, "testUSER"));
   }

   @Test
   public void testCheckForWrite() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser2", 2);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser3", 3);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser6", 6);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_WRITE)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_WRITE);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_WRITE, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_WRITE, id);
      Assert.assertFalse(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, "testUSER"));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, "newTestUser2"));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, "newTestUser3"));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, "newTestUser6"));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 4);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_WRITE, dataDocument, id, -1);
      Assert.assertFalse(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, "testUSER"));
   }

   @Test
   public void testCheckForExecute() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser1", 1);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser3", 3);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser5", 5);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_EXECUTE)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_EXECUTE);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_EXECUTE, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_EXECUTE, id);
      Assert.assertFalse(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, "testUSER"));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, "newTestUser1"));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, "newTestUser3"));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, "newTestUser5"));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_EXECUTE, dataDocument, id, -1);
      Assert.assertFalse(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, "testUSER"));
   }

   @Test
   public void testCheckForAddRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser1", 1);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser3", 3);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser5", 5);
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("newTestUser7", 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_ADD_RIGHTS)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_ADD_RIGHTS);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id);
      Assert.assertFalse(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "testUSER"));
      Assert.assertTrue(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "newTestUser1"));
      Assert.assertTrue(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "newTestUser3"));
      Assert.assertTrue(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "newTestUser5"));
      Assert.assertTrue(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "newTestUser7"));
      dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put("testUSER", 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument, id, -1);
      Assert.assertFalse(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "testUSER"));
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, "testUSER");
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument, id, -1);

      Assert.assertTrue(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, "testUSER"));
   }

   @Test
   public void testSetRightsRead() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, "testUSER"));
      DataDocument newDataDoc = securityFacade.setRightsRead(dataDocument, "testUSER");
      Assert.assertTrue(securityFacade.checkForRead(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForRead(newDataDoc, "testUSER2"));
      Assert.assertFalse(securityFacade.checkForExecute(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForWrite(newDataDoc, "testUSER"));
   }

   @Test
   public void testSetRightsWrite() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, "testUSER"));
      DataDocument newDataDoc = securityFacade.setRightsWrite(dataDocument, "testUSER");
      Assert.assertTrue(securityFacade.checkForWrite(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForRead(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForExecute(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForWrite(newDataDoc, "testUSER2"));
   }

   @Test
   public void testSetRightsExecute() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, "testUSER"));
      DataDocument newDataDoc = securityFacade.setRightsExecute(dataDocument, "testUSER");
      Assert.assertTrue(securityFacade.checkForExecute(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForRead(newDataDoc, "testUSER"));
      Assert.assertFalse(securityFacade.checkForExecute(newDataDoc, "testUSER2"));
      Assert.assertFalse(securityFacade.checkForWrite(newDataDoc, "testUSER"));
   }

   @Test
   public void testSetAllRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsRead(dataDocument, "user1");
      securityFacade.setRightsWrite(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 6);
      securityFacade.setRightsExecute(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 7);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsRead(dataDocument, "user1");
      securityFacade.setRightsExecute(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 5);
      securityFacade.setRightsWrite(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 7);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsWrite(dataDocument, "user1");
      securityFacade.setRightsExecute(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 3);
      securityFacade.setRightsRead(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 7);
      securityFacade.setRightsWrite(dataDocument, "user1");
      securityFacade.setRightsExecute(dataDocument, "user1");
      securityFacade.setRightsRead(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 7);
      securityFacade.removeRightsExecute(dataDocument, "user1");
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument,"user1"));
      securityFacade.removeRightsRead(dataDocument, "user1");
      Assert.assertFalse(securityFacade.checkForRead(dataDocument,"user1"));
      securityFacade.removeRightsWrite(dataDocument, "user1");
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument,"user1"));
      securityFacade.removeRightsWrite(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 0);
   }

   @Test
   public void testAddingSameRights() throws Exception{
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsExecute(dataDocument, "user1");
      securityFacade.setRightsExecute(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 1);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsWrite(dataDocument, "user1");
      securityFacade.setRightsWrite(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 2);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsRead(dataDocument, "user1");
      securityFacade.setRightsRead(dataDocument, "user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 4);
      securityFacade.removeRightsExecute(dataDocument,"user1");
      securityFacade.removeRightsWrite(dataDocument,"user1");
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 4);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserName());
      securityFacade.setRightsExecute(dataDocument, "user1");
      securityFacade.setRightsWrite(dataDocument,"user1");
      securityFacade.removeRightsRead(dataDocument, "user1");
      System.out.println(dataDocument.toString());
      Assert.assertEquals(dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).get("user1"), 3);
   }
}