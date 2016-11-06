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

import java.io.Serializable;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import javax.inject.Inject;
import javax.xml.crypto.Data;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class VersionFacade implements Serializable {
   private final String shadow = ".shadow";
   private final String versionString = "_metadata-version";
   private final String updaterString = "_metadata-updater";
   private final String documentIdString = "_id";

   @Inject
   private DataStorage dataStorage;
   //@Inject
   private String userName = "testUser";

   /**
    * Return document version
    *
    * @param collectionName
    *       collection name in which document is stored
    * @param documentId
    *       the id of readed document
    * @return version of readed document as integer
    */
   public int getDocumentVersion(String collectionName, String documentId){
      return getDocumentVersion(dataStorage.readDocument(collectionName, documentId));
   }

   /**
    * Return username, which last change document
    *
    * @param collectionName
    *       collection name, where document it stored
    * @param documentId
    *       the id of readed document
    * @return  user which update this document as string
    */
   public String getDocumentUpdater(String collectionName, String documentId){
      return getDocumentUpdater(dataStorage.readDocument(collectionName, documentId));
   }

   /**
    * Return username, which last change document
    *
    * @param document
    *       document which contains username string stored as metadata
    * @return user which update this document as string
    */
   public String getDocumentUpdater(DataDocument document){
      return document.getString(updaterString);
   }

   /**
    * Return document version
    *
    * @param document
    *       document where this id is stored
    * @return integer, document version
    */
   public int getDocumentVersion(DataDocument document){
      return document.getInteger(versionString);
   }

   public boolean verifyDocumentUpdate(String collectionName, DataDocument document){
      return true;
   }

   /**
    * Create shadow collection if not exists. Create shadow copy of document
    * and change document meta data version to new version (old version + 1). Both, document
    * in shadow collection and in collection will have same data except id. After
    * this new version you should change data in document in collection. Old document is safely
    * in shadow collection
    * @param collectionName
    *       collection name, where document is stored
    * @param document
    *       document, which will be stored in shadow collection
    *       and then updated in new collection to new version.
    *       After that it is possible to change data and save it
    * @return hmm
    */
   public int newDocumentVersion(String collectionName, DataDocument document){
      createMetadata(document);
      return newDocumentVersion(collectionName, document, getDocumentVersion(document) + 1);
   }

   /**
    * Create metadata if not exists
    * @param document
    *       document where to create metadata
    */
   private void createMetadata(DataDocument document){
      if (!document.containsKey(versionString)){
         document.put(versionString, 0);
         document.put(updaterString, userName);
      }
   }

   /**
    * Create shadow collection if not exists. Same as newDocumentVersion but
    * can create version with specified integer
    * @param collectionName
    *       collection where this document is stored
    * @param document
    *       document to back and change version
    * @param toVersion
    *       create new version with this integer. Not check if this version exists!
    * @return hmm
    */
   public int newDocumentVersion(String collectionName, DataDocument document, int toVersion){
      createShadow(collectionName);
      createMetadata(document);
      LinkedHashMap<String, Object> hashMap = new LinkedHashMap<String, Object>();
      hashMap.put(documentIdString, document.get(documentIdString));
      hashMap.put(versionString, getDocumentVersion(document));
      Object idOfDocument = document.get(documentIdString);
      String idSringOfDocument = document.get(documentIdString).toString();
      document.put(documentIdString, hashMap);
      /*
         create new createDocument method in DataStorage to return some constant string
       */
      dataStorage.createDocument(collectionName + shadow, document);
      if (verifyDocumentUpdate(collectionName + shadow, document)) {
         //throw new Exception("cannot create document in" + collectionName + ".shadow colleciton");
         document.put(documentIdString, idOfDocument);
         document.replace(versionString, toVersion);
         document.replace(updaterString, userName);
         dataStorage.updateDocument(collectionName, document, idSringOfDocument);
         if (!verifyDocumentUpdate(collectionName, document)) {
            //throw new Exception("cannot create document in" + collectionName + " colleciton");
            return -1;
         }
      } else { //TODO throw EXCEPTION ?
         return -1;
      }
      return toVersion;
   }

   private void createShadow(String collectionName){
      if (!(dataStorage.getAllCollections().contains(collectionName + shadow))){
         dataStorage.createCollection(collectionName + shadow);
      }
   }

   /**
    * Take document, parse id, backup document in collection (to .shadow collection)
    * and replace document in collection with document as input. Create shadow if not
    * exists
    * @param collectionName
    *       collection where document is stored
    * @param document
    *       document to store in collection and backup document with same id
    * @return return document new version
    */
   public int newDocumentVersionById(String collectionName, DataDocument document){
      String documentId = document.get(documentIdString).toString();
      DataDocument data = dataStorage.readDocument(collectionName,documentId);
      int version = newDocumentVersion(collectionName,data);
      document.replace(versionString,document.getInteger(versionString)+1);
      document.replace(updaterString,userName);
      dataStorage.updateDocument(collectionName,document,documentId);
      return version;
   }

   public void revertDocumentVersion(String collectionName, DataDocument document, int revertTo){
      //need to read data
   }

   public List<DataDocument> getDocumentVersions(String collectionName, String documentId){
      //need to read data
      return null;
   }
}
