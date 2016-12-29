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
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.rest.dao.AccessRightsDao;
import io.lumeer.engine.rest.dao.ViewDao;
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

   @Inject
   DocumentMetadataFacade dmf;

   private final int READ_BP = 2;
   private final int WRITE_BP = 1;
   private final int EXECUTE_BP = 0;
   private final int NO_RIGHTS = -1;
   private final int NOT_FOUND = 0;

   /* change EMPTY_LIST to 0 to specify no rights if no user is presented in array of rights but list exists
      change EMPTY_LIST to -1 to specify all rights for all user if no user is presented in array of rights but list exists
    */
   private final int EMPTY_LIST = 0;
   private final int NULL_LIST = -1;

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
            setRights(dataDocument, LumeerConst.Security.READ, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean setRightsRead(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      setRightsRead(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentId);
      return checkForRead(collectionName, documentId, userName);
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
            setRights(dataDocument, LumeerConst.Security.WRITE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean setRightsWrite(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      setRightsWrite(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentId);
      return checkForWrite(collectionName, documentId, userName);
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
            setRights(dataDocument, LumeerConst.Security.EXECUTE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean setRightsExecute(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      setRightsExecute(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentId);
      return checkForExecute(collectionName, documentId, userName);
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
            setRights(dataDocument, (-1) * LumeerConst.Security.EXECUTE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean removeRightsExecute(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      removeRightsExecute(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentId);
      return !checkForExecute(collectionName, documentId, userName);
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
            setRights(dataDocument, (-1) * LumeerConst.Security.WRITE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove rights for write in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean removeRightsWrite(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      removeRightsWrite(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentId);
      return !checkForWrite(collectionName, documentId, userName);
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
            setRights(dataDocument, (-1) * LumeerConst.Security.READ, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove rights for read in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean removeRightsRead(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      removeRightsRead(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentId);
      return !checkForRead(collectionName, documentId, userName);
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
         return NULL_LIST;
      }
      if (arrayList.size() == 0) {
         return EMPTY_LIST;
      }
      for (DataDocument dataDoc : arrayList) {
         if (dataDoc.containsValue(email)) {
            return dataDoc.getInteger(LumeerConst.Security.RULE);
         }
      }
      return 0;
   }

   private void replaceInList(DataDocument dataDocument, String email, int newInteger) {
      List<DataDocument> arrayList = readList(dataDocument);
      for (DataDocument datadoc : arrayList) {
         if (datadoc.containsValue(email)) {
            datadoc.replace(LumeerConst.Security.RULE, newInteger);
         }
      }
      dataDocument.replace(LumeerConst.Document.USER_RIGHTS, arrayList);
   }

   private void putToList(DataDocument dataDocument, String email, int rights) {
      List<DataDocument> arrayList = readList(dataDocument);
      DataDocument newRule = new DataDocument();
      newRule.put(LumeerConst.Security.USER_ID, email);
      newRule.put(LumeerConst.Security.RULE, rights);
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
    *
    * @param dataDocument
    *       document where this integer is stored
    * @param email
    *       user email which identify one user
    * @return integer as rule of access (linux system rule)
    */
   public int readRightInteger(DataDocument dataDocument, String email) {
      return recordValue(dataDocument, email);
   }

   /**
    * Read all rules of access list and return in as hashmap
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @return return hashmap of all rules
    */
   public HashMap readRightList(String collectionName, String documentId) {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      List<DataDocument> arrayList = readList(dataDocument);
      for (DataDocument dataDoc : arrayList) {
         map.put(dataDoc.getString(LumeerConst.Security.USER_ID), dataDoc.getInteger(LumeerConst.Security.RULE));
      }
      return map;
   }

   /**
    * Return data access object of access rights.
    *
    * @param dataDocument
    *       where access rights are stored
    * @param email
    *       user email (identificator)
    * @return data access object
    */
   public AccessRightsDao getDao(DataDocument dataDocument, String email) {
      AccessRightsDao accessRightsDao = new AccessRightsDao(checkForRead(dataDocument, email), checkForWrite(dataDocument, email), checkForExecute(dataDocument, email), email);
      return accessRightsDao;
   }

   /**
    * Return data access object of access rights.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param email
    *       user email
    * @return data access object
    */
   public AccessRightsDao getDao(String collectionName, String documentId, String email) {
      return getDao(dataStorage.readDocument(collectionName, documentId), email);
   }

   /**
    * This is not tested and maybe not stable
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param email
    *       user email
    * @return data access object
    * @throws CollectionNotFoundException
    *       if collection not found
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public AccessRightsDao getDaoCached(String collectionName, String documentId, String email) throws CollectionNotFoundException, DocumentNotFoundException {
      return getDao((DataDocument) dmf.readDocumentMetadata(collectionName, documentId), email);
   }

   /**Set rights to be same as data access object
    * @param dataDocument
    *     data document where rights to be set
    * @param accessRightsDao
    *      how data should see
    * @return
    *    data document with set rights
    */
   public DataDocument setDao(DataDocument dataDocument, AccessRightsDao accessRightsDao) {
      if (accessRightsDao.isExecute()) {
         setRightsExecute(dataDocument, accessRightsDao.getUserName());
      } else {
         removeRightsExecute(dataDocument, accessRightsDao.getUserName());
      }
      if (accessRightsDao.isRead()) {
         setRightsRead(dataDocument, accessRightsDao.getUserName());
      } else {
         removeRightsExecute(dataDocument, accessRightsDao.getUserName());
      }
      if (accessRightsDao.isWrite()) {
         setRightsWrite(dataDocument, accessRightsDao.getUserName());
      } else {
         removeRightsWrite(dataDocument, accessRightsDao.getUserName());
      }
      return dataDocument;
   }

   /**
    * Set rights to be same as data access object.
    * This is not atomic!
    * @param collectionName
    *       collection name where data document is
    * @param documentId
    *       data document id
    * @param accessRightsDao
    *       data access object
    */
   public void setDao(String collectionName, String documentId, AccessRightsDao accessRightsDao) {
      dataStorage.updateDocument(collectionName, setDao(dataStorage.readDocument(collectionName, documentId), accessRightsDao), documentId);
   }

   /**
    * Set rights to be same as data access object with
    * check. Can be slow. This is not atomic!
    * @param collectionName
    *       collection name where data document is
    * @param documentId
    *       data document id
    * @param accessRightsDao
    *       data access object
    * @return
    *       true if all data was updated successful
    *
    * */
   public boolean setDaoCheck(String collectionName, String documentId, AccessRightsDao accessRightsDao) {
      dataStorage.updateDocument(collectionName, setDao(dataStorage.readDocument(collectionName, documentId), accessRightsDao), documentId);
      DataDocument dataDoc = dataStorage.readDocument(collectionName, documentId);
      return (accessRightsDao.isWrite() == checkForWrite(dataDoc, accessRightsDao.getUserName()))
            & (accessRightsDao.isRead() == checkForRead(dataDoc, accessRightsDao.getUserName()))
            & (accessRightsDao.isExecute() == checkForExecute(dataDoc, accessRightsDao.getUserName()));
   }
}
