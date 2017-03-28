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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com>Jan Kotrady</a>
 */
@RunWith(Arquillian.class)
public class SecurityFacadeIntegrationTest extends IntegrationTestBase {

   /*
   Tieto testy som nepisal optimalne, ale tak,
   aby boli napisane cim skor ... takze ak raz
   bude cas ich upravit, tak ich upravim ...
   Ale tieto testy su kvalitne, testuju poriadne
   pridavanie prav.
    */

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
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Test
   public void testCheckForReadDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isTrue();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER4, 4);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER6, 6);
      addRights(dataDocument, TEST_USER7, 7);
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER4)).isTrue();
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER5)).isTrue();
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER6)).isTrue();
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 2);
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isFalse();
   }

   @Test
   public void testCheckForWriteDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isTrue();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER2, 2);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER6, 6);
      addRights(dataDocument, TEST_USER7, 7);
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER2)).isTrue();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER3)).isTrue();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER6)).isTrue();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 4);
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isFalse();
   }

   @Test
   public void testCheckForExecuteDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isTrue();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER1, 1);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER7, 7);
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER1)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER3)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER5)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 2);
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isFalse();
   }

   @Test
   public void testCheckForAddRightsDataDoc() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForAddRights(dataDocument, TEST_USER)).isTrue();
      securityFacade.addMetaData(dataDocument);
      addRights(dataDocument, TEST_USER1, 1);
      addRights(dataDocument, TEST_USER3, 3);
      addRights(dataDocument, TEST_USER5, 5);
      addRights(dataDocument, TEST_USER7, 7);
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER1)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER3)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER5)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 2);
      assertThat(securityFacade.checkForAddRights(dataDocument, TEST_USER)).isFalse();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, TEST_USER);
      assertThat(securityFacade.checkForAddRights(dataDocument, TEST_USER)).isTrue();
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
      String documentIdFilter = dataStorageDialect.documentIdFilter(id);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_READ, documentIdFilter);
      assertThat(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER4)).isTrue();
      assertThat(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER5)).isTrue();
      assertThat(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER6)).isTrue();
      assertThat(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_READ, dataDocument, documentIdFilter);
      assertThat(securityFacade.checkForRead(SECURITY_TEST_COLLECTION_READ, id, TEST_USER)).isFalse();
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
      String documentIdFilter = dataStorageDialect.documentIdFilter(id);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_WRITE, documentIdFilter);
      assertThat(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER2)).isTrue();
      assertThat(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER3)).isTrue();
      assertThat(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER6)).isTrue();
      assertThat(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 4);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_WRITE, dataDocument, documentIdFilter);
      assertThat(securityFacade.checkForWrite(SECURITY_TEST_COLLECTION_WRITE, id, TEST_USER)).isFalse();
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
      String documentIdFilter = dataStorageDialect.documentIdFilter(id);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_EXECUTE, documentIdFilter);
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER1)).isTrue();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER3)).isTrue();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER5)).isTrue();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 4);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_EXECUTE, dataDocument, documentIdFilter);
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_EXECUTE, id, TEST_USER)).isFalse();
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
      String documentIdFilter = dataStorageDialect.documentIdFilter(id);
      dataDocument = dataStorage.readDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, documentIdFilter);
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER1)).isTrue();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER3)).isTrue();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER5)).isTrue();
      assertThat(securityFacade.checkForExecute(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER7)).isTrue();
      addRights(dataDocument, TEST_USER, 2);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument, documentIdFilter);
      assertThat(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER)).isFalse();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, TEST_USER);
      dataStorage.updateDocument(SECURITY_TEST_COLLECTION_ADD_RIGHTS, dataDocument, documentIdFilter);
      assertThat(securityFacade.checkForAddRights(SECURITY_TEST_COLLECTION_ADD_RIGHTS, id, TEST_USER)).isTrue();
   }

   @Test
   public void testSetRightsRead() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isTrue();
      DataDocument newDataDoc = securityFacade.setRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.checkForRead(newDataDoc, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForRead(newDataDoc, TEST_USER2)).isFalse();
      assertThat(securityFacade.checkForExecute(newDataDoc, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForWrite(newDataDoc, TEST_USER)).isFalse();

   }

   @Test
   public void testSetRightsWrite() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isTrue();
      DataDocument newDataDoc = securityFacade.setRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.checkForWrite(newDataDoc, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForRead(newDataDoc, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(newDataDoc, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForWrite(newDataDoc, TEST_USER2)).isFalse();
   }

   @Test
   public void testSetRightsExecute() throws Exception {
      DataDocument dataDocument = new DataDocument();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isTrue();
      DataDocument newDataDoc = securityFacade.setRightsExecute(dataDocument, TEST_USER);
      assertThat(securityFacade.checkForExecute(newDataDoc, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForRead(newDataDoc, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(newDataDoc, TEST_USER2)).isFalse();
      assertThat(securityFacade.checkForWrite(newDataDoc, TEST_USER)).isFalse();
   }

   @Test
   public void testSetAllRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(6);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(7);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(5);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(7);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(3);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(7);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(7);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isFalse();
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isFalse();
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isFalse();
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(0);
   }

   @Test
   public void testAddingSameRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(1);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(2);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      securityFacade.setRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(4);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(4);
      dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsExecute(dataDocument, TEST_USER);
      securityFacade.setRightsWrite(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(3);
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
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(7);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(3);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      securityFacade.removeRightsExecute(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      securityFacade.removeRightsRead(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(2);
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      securityFacade.removeRightsWrite(dataDocument, TEST_USER);
      assertThat(securityFacade.readRightInteger(dataDocument, TEST_USER)).isEqualTo(0);
   }

   @Test
   public void testNoRights() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForAddRights(dataDocument, TEST_USER)).isTrue();
      assertThat(securityFacade.checkForRead(dataDocument, userFacade.getUserEmail())).isTrue();
      assertThat(securityFacade.checkForWrite(dataDocument, userFacade.getUserEmail())).isTrue();
      assertThat(securityFacade.checkForExecute(dataDocument, userFacade.getUserEmail())).isTrue();
      assertThat(securityFacade.checkForAddRights(dataDocument, userFacade.getUserEmail())).isTrue();

      securityFacade.addMetaData(dataDocument);
      assertThat(securityFacade.checkForRead(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForWrite(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForExecute(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForAddRights(dataDocument, TEST_USER)).isFalse();
      assertThat(securityFacade.checkForRead(dataDocument, userFacade.getUserEmail())).isFalse();
      assertThat(securityFacade.checkForWrite(dataDocument, userFacade.getUserEmail())).isFalse();
      assertThat(securityFacade.checkForExecute(dataDocument, userFacade.getUserEmail())).isFalse();
      assertThat(securityFacade.checkForAddRights(dataDocument, userFacade.getUserEmail())).isTrue();
   }

   @Test
   public void testReadDaoList() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      securityFacade.setRightsExecute(dataDocument, "test@gmail.com");
      securityFacade.setRightsRead(dataDocument, "test@gmail.com");
      securityFacade.setRightsWrite(dataDocument, "test@gmail.com");

      securityFacade.setRightsRead(dataDocument, "test2@gmail.com");
      securityFacade.setRightsWrite(dataDocument, "test2@gmail.com");

      securityFacade.setRightsExecute(dataDocument, "test4@gmail.com");
      securityFacade.setRightsRead(dataDocument, "test4@gmail.com");

      securityFacade.setRightsExecute(dataDocument, "test3@gmail.com");
      securityFacade.setRightsWrite(dataDocument, "test3@gmail.com");
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

}