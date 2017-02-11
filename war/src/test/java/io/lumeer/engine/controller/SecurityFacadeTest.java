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

import java.util.List;
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

   private final String RULE = "rule";
   private final String USER_ID = "user_email";

   private final String TEST_USER1 = "!#$@%$^&)(*^%$&#@^$!@!test1@bo.com";
   private final String TEST_USER2 = "!#$@%$^&)(*^%$&#@^$!@!test2@f{}o.com";
   private final String TEST_USER3 = "!#$@%$^&)(*^%$&#@^$!@!test3@po.com";
   private final String TEST_USER4 = "!#$@%$^&)(*^%$&#@^$!@!test4@mo.com";
   private final String TEST_USER5 = "!#$@%$^&)(*^%$&#@^$!@!test5@no.com";
   private final String TEST_USER6 = "!#$@%$^&)(*^%$&#@^$!@!test6@ro.com";
   private final String TEST_USER7 = "!#$@%$^&)(*^%$&#@^$!@!test7@phi.com";
   private final String TEST_USER = "testUSER@fi.com";

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
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER));
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER4, 4);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER6, 6);
      addRights(dataDocument, TEST_USER7, 7);
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER4));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER5));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER6));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER7));
      addRights(dataDocument, TEST_USER, 2);
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, TEST_USER));
   }

   @Test
   public void testCheckForWriteDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER));
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER2, 2);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER6, 6);
      addRights(dataDocument, TEST_USER7, 7);
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER2));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER3));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER6));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER7));
      addRights(dataDocument, TEST_USER, 4);
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, TEST_USER));
   }

   @Test
   public void testCheckForExecuteDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER));
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER1, 1);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER7, 7);
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER1));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER3));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER5));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER7));
      addRights(dataDocument, TEST_USER, 2);
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, TEST_USER));
   }

   @Test
   public void testCheckForAddRightsDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, TEST_USER));
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER1, 1);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER7, 7);
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER1));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER3));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER5));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER7));
      addRights(dataDocument, TEST_USER, 2);
      Assert.assertFalse(securityFacade.checkForAddRights(dataDocument, TEST_USER));
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, TEST_USER);
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, TEST_USER));
   }

   @Test
   public void testCheckForRead() throws Exception {
      DataDocument dataDocument = new DataDocument();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER4, 4);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER6, 6);
      addRights(dataDocument, TEST_USER7, 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_READ)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_READ);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_READ, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_READ, id);
      print(securityFacade.readRightsMap(SECURITY_TEST_COLLECTION_READ, id).toString());
      Assert.assertFalse(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER4));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER5));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER6));
      Assert.assertTrue(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER7));
      addRights(dataDocument, TEST_USER, 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_READ, dataDocument, id);
      Assert.assertFalse(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER));
   }

   @Test
   public void testCheckForWrite() throws Exception {
      DataDocument dataDocument = new DataDocument();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER2, 2);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER6, 6);
      addRights(dataDocument, TEST_USER7, 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_WRITE)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_WRITE);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_WRITE, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_WRITE, id);
      Assert.assertFalse(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER2));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER3));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER6));
      Assert.assertTrue(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER7));
      addRights(dataDocument, TEST_USER, 4);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_WRITE, dataDocument, id);
      Assert.assertFalse(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER));
   }

   @Test
   public void testCheckForExecute() throws Exception {
      DataDocument dataDocument = new DataDocument();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER1, 1);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER7, 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_EXECUTE)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_EXECUTE);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_EXECUTE, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_EXECUTE, id);
      Assert.assertFalse(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER1));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER3));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER5));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER7));
      addRights(dataDocument, TEST_USER, 4);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_EXECUTE, dataDocument, id);
      Assert.assertFalse(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER));
   }

   @Test
   public void testCheckForAddRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER1, 1);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER7, 7);
      if (dataStorage.hasCollection(SECURITY_TEST_COLLECTION_ADD_RIGHTS)) {
         dataStorage.dropCollection(SECURITY_TEST_COLLECTION_ADD_RIGHTS);
      }
      String id = dataStorage.createDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id);
      Assert.assertFalse(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER1));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER3));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER5));
      Assert.assertTrue(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER7));
      addRights(dataDocument, TEST_USER, 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument, id);
      Assert.assertFalse(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER));
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, TEST_USER);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument, id);
      Assert.assertTrue(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER));
   }

   @Test
   public void testSetRightsRead() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER));
      DataDocument newDataDoc = securityFacade.setRightsRead(dataDocument, TEST_USER);
      print(newDataDoc);
      Assert.assertTrue(securityFacade.checkForRead(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForRead(newDataDoc, TEST_USER2));
      Assert.assertFalse(securityFacade.checkForExecute(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForWrite(newDataDoc, TEST_USER));

   }

   @Test
   public void testSetRightsWrite() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER));
      DataDocument newDataDoc = securityFacade.setRightsWrite(dataDocument, TEST_USER);
      Assert.assertTrue(securityFacade.checkForWrite(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForRead(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForExecute(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForWrite(newDataDoc, TEST_USER2));
   }

   @Test
   public void testSetRightsExecute() throws Exception {
      DataDocument dataDocument = new DataDocument();
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER));
      DataDocument newDataDoc = securityFacade.setRightsExecute(dataDocument, TEST_USER);
      Assert.assertTrue(securityFacade.checkForExecute(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForRead(newDataDoc, TEST_USER));
      Assert.assertFalse(securityFacade.checkForExecute(newDataDoc, TEST_USER2));
      Assert.assertFalse(securityFacade.checkForWrite(newDataDoc, TEST_USER));
   }

   @Test
   public void testSetAllRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 6);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 7);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 5);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 7);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 3);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 7);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 7);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, TEST_USER));
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, TEST_USER));
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, TEST_USER));
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 0);
   }

   @Test
   public void testAddingSameRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 1);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 2);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 4);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 4);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 3);
   }

   @Test
   public void testRemoveRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER2);
      securityFacade.setRightsExecute(dataDocument, TEST_USER2);
      securityFacade.setRightsRead(dataDocument, TEST_USER2);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 7);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 3);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 2);
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      Assert.assertEquals(securityFacade.readRightInteger(dataDocument, TEST_USER), 0);
   }

   @Test
   public void testNoRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, TEST_USER));
      Assert.assertTrue(securityFacade.checkForRead(dataDocument, userFacade.getUserEmail()));
      Assert.assertTrue(securityFacade.checkForWrite(dataDocument, userFacade.getUserEmail()));
      Assert.assertTrue(securityFacade.checkForExecute(dataDocument, userFacade.getUserEmail()));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, userFacade.getUserEmail()));

      securityFacade.addMetaData(dataDocument);
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, TEST_USER));
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, TEST_USER));
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, TEST_USER));
      Assert.assertFalse(securityFacade.checkForAddRights(dataDocument, TEST_USER));
      Assert.assertFalse(securityFacade.checkForRead(dataDocument, userFacade.getUserEmail()));
      Assert.assertFalse(securityFacade.checkForWrite(dataDocument, userFacade.getUserEmail()));
      Assert.assertFalse(securityFacade.checkForExecute(dataDocument, userFacade.getUserEmail()));
      Assert.assertTrue(securityFacade.checkForAddRights(dataDocument, userFacade.getUserEmail()));
   }

   @Test
   public void testReadDaoList() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsExecute(dataDocument,"test@gmail.com");
      securityFacade.setRightsRead(dataDocument, "test@gmail.com");
      securityFacade.setRightsWrite(dataDocument,"test@gmail.com");

      securityFacade.setRightsRead(dataDocument, "test2@gmail.com");
      securityFacade.setRightsWrite(dataDocument,"test2@gmail.com");

      securityFacade.setRightsExecute(dataDocument,"test4@gmail.com");
      securityFacade.setRightsRead(dataDocument, "test4@gmail.com");

      securityFacade.setRightsExecute(dataDocument,"test3@gmail.com");
      securityFacade.setRightsWrite(dataDocument,"test3@gmail.com");

      if (dataStorage.hasCollection("securityFacadeTestList")){
         dataStorage.dropCollection("securityFacadeTestList");
      }
      dataStorage.createCollection("securityFacadeTestList");
      String id = dataStorage.createDocument("securityFacadeTestList",dataDocument);
      System.out.println("=================================");
      System.out.println(securityFacade.getDaoList("securityFacadeTestList",id).get(0).toString());
      System.out.println(securityFacade.getDaoList("securityFacadeTestList",id).get(1).toString());
      System.out.println(securityFacade.getDaoList("securityFacadeTestList",id).get(2).toString());
      System.out.println(securityFacade.getDaoList("securityFacadeTestList",id).get(3).toString());
      System.out.println(securityFacade.readQueryString("test@gmail.com"));
      System.out.println("========= dao from document===========");
      System.out.println(securityFacade.getDaoList(dataDocument).get(0).toString());
      System.out.println(securityFacade.getDaoList(dataDocument).get(1).toString());
      System.out.println(securityFacade.getDaoList(dataDocument).get(2).toString());
      System.out.println(securityFacade.getDaoList(dataDocument).get(3).toString());
   }

   private void addRights(DataDocument dataDocument, String email, Integer rights) {
      List<DataDocument> arrayList = readList(dataDocument);
      DataDocument newRule = new DataDocument();
      newRule.put(USER_ID, email);
      newRule.put(RULE, rights);
      arrayList.add(newRule);
      dataDocument.replace(LumeerConst.Document.USER_RIGHTS, arrayList);
   }

   private List<DataDocument> readList(DataDocument dataDocument) {
      return dataDocument.getArrayList(LumeerConst.Document.USER_RIGHTS, DataDocument.class);
   }

   private void print(DataDocument dataDocument) {
      System.out.println("=====================");
      System.out.println("=====================");
      System.out.println("=====================");
      System.out.println(dataDocument.toString());
   }

   private void print(String dataDocument) {
      System.out.println("=====================");
      System.out.println("=====================");
      System.out.println("=====================");
      System.out.println(dataDocument);
   }
}