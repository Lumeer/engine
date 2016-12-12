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
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
@SessionScoped
public class SecurityFacade implements Serializable {

   @Inject
   DataStorage dataStorage;

   @Inject
   UserFacade user;

   private final int READ_BP = 2;
   private final int WRITE_BP = 1;
   private final int EXECUTE_BP = 0;

   private final int READ = 4;
   private final int WRITE = 2;
   private final int EXECUTE = 1;

   private DataDocument readUserRights(DataDocument dataDocument) {
      DataDocument rights = dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS);
      return rights;
   }

   // TODO add users group ...
   private DataDocument readGroupRights(DataDocument dataDocument) {
      return null;
   }

   private boolean checkBit(DataDocument rights, int bit, String userName) {
      if (rights == null) {
         return true;
      }
      if (rights.containsKey(userName)) {
         int rightsInteger = rights.getInteger(userName);
         /*String checkString = String.format("%3s", Integer.toBinaryString(rightsInteger)).replace(' ', '0');
         if (checkString.charAt(2 - bit) == '1') {

         }*/
         if ((rightsInteger >> bit & 1) == 1) {
            return true;
         }
      } else {
         return false;
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
      return checkBit(readUserRights(dataDocument), READ_BP, userName);
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
      return checkBit(readUserRights(dataDocument), WRITE_BP, userName);
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
      return checkBit(readUserRights(dataDocument), EXECUTE_BP, userName);
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
    */
   public boolean checkForRead(String collectionName, String documentId, String userName) {
      return checkForRead(dataStorage.readDocument(collectionName, documentId), userName);
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
      DataDocument dataDoc = dataStorage.readDocument(collectionName, documentId);
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
      DataDocument dataDoc = dataStorage.readDocument(collectionName, documentId);
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
      DataDocument dataDoc = dataStorage.readDocument(collectionName, documentId);
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
      if (checkForAddRights(dataDocument, user.getUserName())) {
         setRights(dataDocument, READ, userName);
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
      if (checkForAddRights(dataDocument, user.getUserName())) {
         setRights(dataDocument, WRITE, userName);
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
      if (checkForAddRights(dataDocument, user.getUserName())) {
         setRights(dataDocument, EXECUTE, userName);
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
   public DataDocument removeRightsExecute(DataDocument dataDocument, String userName){
      if (checkForAddRights(dataDocument, user.getUserName())) {
         setRights(dataDocument, (-1)*EXECUTE, userName);
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
   public DataDocument removeRightsWrite(DataDocument dataDocument, String userName){
      if (checkForAddRights(dataDocument, user.getUserName())) {
         setRights(dataDocument, (-1)* WRITE, userName);
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
   public DataDocument removeRightsRead(DataDocument dataDocument, String userName){
      if (checkForAddRights(dataDocument, user.getUserName())) {
         setRights(dataDocument, (-1) * READ, userName);
      }
      return dataDocument;
   }

   private boolean checkMetadata(DataDocument dataDocument) {
      return dataDocument.containsKey(LumeerConst.Document.METADATA_PREFIX + LumeerConst.View.VIEW_USER_RIGHTS_KEY);
   }

   private void addMetaData(DataDocument dataDocument) {
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, new DataDocument());
   }

   private void setRights(DataDocument dataDocument, int addRights, String userName) {
      if (!checkMetadata(dataDocument)) {
         addMetaData(dataDocument);
      }
      if (dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).containsKey(userName)) {
         int newRights = dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).getInteger(userName) + addRights;
         if ((newRights <= 7) && (newRights >= 0)) {
            dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).replace(userName, newRights);
         }
      } else {
         dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put(userName, addRights);
      }
   }
}
