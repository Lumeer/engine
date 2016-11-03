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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.CollectionEvent;
import io.lumeer.engine.api.data.CollectionMetadataElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
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

   public void createCollection(final String collectionName) {
      // TODO: Do we suppose that REST service takes care of special characters in collection name? If not, we should do that here.
      dataStorage.createCollection(collectionName.toLowerCase());
      dataStorage.createCollection(METADATA_PREXIF + collectionName.toLowerCase()); // creates metadata collection

      collections = null;
   }

   public void dropCollection(final String collectionName) {
      dataStorage.dropCollection(collectionName);
      dataStorage.dropCollection(METADATA_PREXIF + collectionName.toLowerCase()); // removes metadata collection

      collections = null;
   }

   public List<CollectionMetadataElement> readCollectionMetadata(final String collectionName) {
      return null;
   }

   public void updateCollectionMetadata(final String collectionName, final CollectionMetadataElement element) {
   }

   public void dropCollectionMetadata(final String collectionName) {
      dataStorage.dropCollection(METADATA_PREXIF + collectionName.toLowerCase());
   }

   public Set<Object> getAttributeValues(final String collectionName, final String attributeName) {
      Set<Object> attributeValues = new HashSet<Object>();

      // TODO: resolve column type (if available in collection metadata)

      int page = 1;
      List<DataDocument> documents = queryFromDb(collectionName, page, PAGE_SIZE);
      while (attributeValues.size() < PAGE_SIZE || documents.size() == 0) {
         for (DataDocument document : documents) {
            if (attributeValues.size() > PAGE_SIZE) {
               break;
            }
            for (String key : document.keySet()) {
               attributeValues.add(document.get(key)); // TODO: we should in some way use DataStorage to give us distinct values
            }
         }
         page++;
         documents = queryFromDb(collectionName, page, PAGE_SIZE);
      }
      return attributeValues;
   }

   public void addColumn(final String collectionName, final String columnName) {
      // we add column with blank value to all documents
      // use MetadataFacade + change all documents
   }

   public void renameColumn(final String collectionName, final String origName, final String newName) {
      // use MetadataFacade + change all documents
   }

   public void dropColumn(final String collectionName, final String columnName) {
      // use MetadataFacade + change all documents
   }

   public void onCollectionEvent(@Observes(notifyObserver = Reception.IF_EXISTS) final CollectionEvent event) {
      // we do not care which collection got changed, we just invalidate our cache
      collections = null;
   }

   // method used to query something from DB without any sort or filter
   private List<DataDocument> queryFromDb(String collectionName, int page, int limit) {
      return dataStorage.search(collectionName, "", "", (page - 1) * limit, limit);
   }

}
