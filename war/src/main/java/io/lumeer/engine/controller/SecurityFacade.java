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
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.xml.crypto.Data;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
@SessionScoped
public class SecurityFacade implements Serializable {

   @Inject
   DataStorage dataStorage;

   @Inject
   UserFacade user;

   private final String RULE = "rule";
   private final String USER_ID = "user_email";
   private final int READ_BP = 2;
   private final int WRITE_BP = 1;
   private final int EXECUTE_BP = 0;
   private final int NO_RIGHTS = -1;
   private final int NOT_FOUND = 0;

   private final int READ = 4;
   private final int WRITE = 2;
   private final int EXECUTE = 1;

   // TODO add users group ...
   private DataDocument readGroupRights(DataDocument dataDocument) {
      return null;
   }

   private boolean checkBit(int inBit, int bit) {
      if (inBit == NO_RIGHTS) {
         return true;
      }

      if (inBit == NOT_FOUND) {
         return false;
      }
      if ((inBit >> bit & 1) == 1) {
         return true;
      }
      return false;
   }

   /**
    * Check in datadocument for userName if can read.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can read this document
    */
   public boolean checkForRead(DataDocument dataDocument, String userName) {
      return checkBit(recordValue(dataDocument, userName), READ_BP);
   }

   /**
    * Check in datadocument for userName if can write.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can write to this document
    */
   public boolean checkForWrite(DataDocument dataDocument, String userName) {
      return checkBit(recordValue(dataDocument, userName), WRITE_BP);
   }

   /**
    * Check in datadocument for userName if can execute.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can execute this document
    */
   public boolean checkForExecute(DataDocument dataDocument, String userName) {
      return checkBit(recordValue(dataDocument, userName), EXECUTE_BP);
   }

   /**
    * Check in datadocument for userName if can add rights - same as execute.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can add rights this document
    */
   public boolean checkForAddRights(DataDocument dataDocument, String userName) {
      if (dataDocument.containsKey(LumeerConst.Document.CREATE_BY_USER_KEY)) {
         if (dataDocument.getString(LumeerConst.Document.CREATE_BY_USER_KEY).equals(userName)) {
            return true;
         }
      }
      return checkForExecute(dataDocument, userName);
   }

   private List<String> buildMetaList() {
      List<String> arrayList = new ArrayList<>();
      arrayList.add(LumeerConst.Document.CREATE_BY_USER_KEY);
      arrayList.add(LumeerConst.Document.USER_RIGHTS);
      return arrayList;
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can read this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can read this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForRead(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, documentId, buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return checkForRead(dataDoc, userName);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can write to this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can write to this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForWrite(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, documentId, buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return checkForWrite(dataDoc, userName);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can execute this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can execute this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForExecute(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, documentId, buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return checkForExecute(dataStorage.readDocument(collectionName, documentId), userName);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can add rights to this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can add rights to this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForAddRights(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, documentId, buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (dataDoc.containsKey(LumeerConst.Document.CREATE_BY_USER_KEY)) {
         if (dataDoc.getString(LumeerConst.Document.CREATE_BY_USER_KEY).equals(userName)) {
            return true;
         }
      }
      return checkForExecute(collectionName, documentId, userName);
   }

   /**
    * Set read rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument setRightsRead(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (!checkForRead(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, READ, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set write rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument setRightsWrite(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (!checkForWrite(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, WRITE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set execute rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument setRightsExecute(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (!checkForExecute(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, EXECUTE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove execute rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument removeRightsExecute(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (checkForExecute(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, (-1) * EXECUTE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove write rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument removeRightsWrite(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (checkForWrite(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, (-1) * WRITE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove read rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument removeRightsRead(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (checkForRead(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, (-1) * READ, userName, value);
         }
      }
      return dataDocument;
   }

   private boolean checkMetadata(DataDocument dataDocument) {
      return dataDocument.containsKey(LumeerConst.Document.USER_RIGHTS);
   }

   public void addMetaData(DataDocument dataDocument) {
      List<DataDocument> list = new ArrayList<>();
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, list);
   }

   private List<DataDocument> readList(DataDocument dataDocument) {
      if (!(dataDocument.containsKey(LumeerConst.Document.USER_RIGHTS))) {
         return null;
      }
      return dataDocument.getArrayList(LumeerConst.Document.USER_RIGHTS, DataDocument.class);
   }

   private int recordValue(DataDocument dataDocument, String email) {
      List<DataDocument> arrayList = readList(dataDocument);
      if (arrayList == null) {
         return -1;
      }
      if (arrayList.size() == 0) {
         return -1;
      }
      for (DataDocument dataDoc : arrayList) {
         if (dataDoc.containsValue(email)) {
            return dataDoc.getInteger(RULE);
         }
      }
      return 0;
   }

   private void replaceInList(DataDocument dataDocument, String email, int newInteger) {
      List<DataDocument> arrayList = readList(dataDocument);
      for (DataDocument datadoc : arrayList) {
         if (datadoc.containsValue(email)) {
            datadoc.replace(RULE, newInteger);
         }
      }
      dataDocument.replace(LumeerConst.Document.USER_RIGHTS, arrayList);
   }

   private void putToList(DataDocument dataDocument, String email, int rights) {
      List<DataDocument> arrayList = readList(dataDocument);
      DataDocument newRule = new DataDocument();
      newRule.put(USER_ID, email);
      newRule.put(RULE, rights);
      arrayList.add(newRule);
      dataDocument.replace(LumeerConst.Document.USER_RIGHTS, arrayList);
   }

   private void setRights(DataDocument dataDocument, int addRights, String userName, int rightsInteger) {
      if (!checkMetadata(dataDocument)) {
         addMetaData(dataDocument);
         rightsInteger = NOT_FOUND;
      }
      if (rightsInteger != NOT_FOUND) {
         int newRights = rightsInteger + addRights;
         if ((newRights <= 7) && (newRights >= 0)) {
            replaceInList(dataDocument, userName, newRights);
            //dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).replace(userName, newRights);
         }
      } else {
         putToList(dataDocument, userName, addRights);
         //dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put(userName, addRights);
      }
   }

   /**
    * Read one integer as access rule of document
    * @param dataDocument
    *       document where this integer is stored
    * @param email
    *       user email which identify one user
    * @return
    *       integer as rule of access (linux system rule)
    */
   public int readRightInteger(DataDocument dataDocument, String email) {
      return recordValue(dataDocument, email);
   }

   /**Read all rules of access list and return in as hashmap
    * @param dataDocument
    *       document where are all rules stored
    * @return
    *       return hashmap of all rules
    */
   public HashMap readRightList(DataDocument dataDocument) {
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      List<DataDocument> arrayList = readList(dataDocument);
      for (DataDocument dataDoc : arrayList) {
         map.put(dataDoc.getString(USER_ID), dataDoc.getInteger(RULE));
      }
      return map;
   }
}
