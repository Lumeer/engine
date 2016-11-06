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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.data.CollectionMetadataElement;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.CollectionEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 *         <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 *         <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Named
@SessionScoped
public class CollectionFacade implements Serializable {

   private static final long serialVersionUID = 8967474543742743308L;

   private final String METADATA_PREXIF = "metadata_"; // TODO: adjust to metadata name rules, or solve by calling a method from MetadataFacade

   private final int PAGE_SIZE = 100;

   @Inject
   // @Named("mongoDbStorage") // we have only one implementation, so mongo is automatically injected
   private DataStorage dataStorage;

   private List<String> collections;

   private MetadataFacade metadataFacade = new MetadataFacade();

   /**
    * Returns a List object of collection names in the database except of metadata collections.
    *
    * @return the list of collection names
    */
   @Produces
   @Named("userCollections")
   public List<String> getAllCollections() {
      if (collections == null) {
         List<String> collectionsAll = dataStorage.getAllCollections();
         collections = new ArrayList<String>();

         // filters out metadata collections
         for (String collection : collectionsAll) {
            if (!collection.startsWith(METADATA_PREXIF)) { // TODO: maybe use MetadataFacade to decide
               collections.add(collection);
            }
         }
      }
      return collections;
   }

   /**
    * Creates a new collection including its metadata collection with the specified name.
    *
    * @param collectionName
    *       the name of the collection to create
    */
   public void createCollection(final String collectionName) {
      // TODO: Do we suppose that REST service takes care of special characters in collection name? If not, we should do that here.
      dataStorage.createCollection(collectionName.toLowerCase());
      dataStorage.createCollection(METADATA_PREXIF + collectionName.toLowerCase()); // creates metadata collection

      collections = null;
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param collectionName
    */
   public void dropCollection(final String collectionName) {
      dataStorage.dropCollection(collectionName);
      dataStorage.dropCollection(METADATA_PREXIF + collectionName.toLowerCase()); // removes metadata collection

      collections = null;
   }

   public List<CollectionMetadataElement> readCollectionMetadata(final String collectionName) {
      // TODO:
      return null;
   }

   public void updateCollectionMetadata(final String collectionName, final CollectionMetadataElement element) {
      // TODO:
   }

   public void dropCollectionMetadata(final String collectionName) {
      dataStorage.dropCollection(METADATA_PREXIF + collectionName.toLowerCase());
   }

   /**
    * Gets the first 100 distinct values of the given attribute in the given collection.
    *
    * @param collectionName
    *       the name of the collection where documents contain the given attribute
    * @param attributeName
    *       the name of the attribute
    * @return the distinct set of values of the given attribute
    */
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) {
      return dataStorage.getAttributeValues(collectionName, attributeName);
   }

   /**
    * Modifies all existing documents in given collection by adding a new column.
    *
    * @param collectionName
    *       the name of the collection where the new column should be added
    * @param columnName
    *       the column name to add
    */
   public void addColumn(final String collectionName, final String columnName) {
      List<DataDocument> documents = getAllDocuments(collectionName);

      for (DataDocument document : documents) {
         String id = (document.get("_id")).toString();

         // TODO: check, if the column name already exists

         document.put(columnName, ""); // blank column value
         dataStorage.updateDocument(collectionName, document, id);
      }
   }

   /**
    * Removes given attribute from all existing document specified by its id.
    *
    * @param collectionName
    *       the name of the collection where the given attribute should be removed
    * @param columnName
    *       the column name to remove
    */
   public void dropColumn(final String collectionName, final String columnName) {
      List<DataDocument> documents = getAllDocuments(collectionName);

      for (DataDocument document : documents) {
         String id = (document.get("_id")).toString();

         // TODO: check, if the column name exists

         dataStorage.removeAttribute(collectionName, id, columnName);
      }
   }

   /**
    * Updates the name of an attribute which is found in all documents of given collection.
    *
    * @param collectionName
    *       the name of the collection where the given attribute should be renamed
    * @param origName
    *       the old name of an attribute
    * @param newName
    *       the new name of an attribute
    */
   public void renameColumn(final String collectionName, final String origName, final String newName) {
      dataStorage.renameAttribute(collectionName, origName, newName);
   }

   public void onCollectionEvent(@Observes(notifyObserver = Reception.IF_EXISTS) final CollectionEvent event) {
      // we do not care which collection got changed, we just invalidate our cache
      collections = null;
   }

   // method used to query something from DB without any sort or filter
   private List<DataDocument> queryFromDb(String collectionName, int page, int limit) {
      return dataStorage.search(collectionName, "", "", (page - 1) * limit, limit);
   }

   private List<DataDocument> getAllDocuments(String collectionName) {
      return dataStorage.search(collectionName, null, null, 0, 0);
   }

}
