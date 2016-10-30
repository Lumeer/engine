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

   private final String METADATA_PREXIF = "metadata_";

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
            if (!collection.startsWith(METADATA_PREXIF)) {
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
      //TODO
      return null;
   }

   public void updateCollectionMetadata(final String collectionName, final CollectionMetadataElement element) {
      //TODO
   }

   public void dropCollectionMetadata(final String collectionName) {
      dataStorage.dropCollection(METADATA_PREXIF + collectionName.toLowerCase());
   }

   public Set<Object> getAttributeValues(final String collectionName, final String attributeName) {
      Set<Object> attributeValues = new HashSet<Object>();

      //TODO: Iterate over documents in the collection
      DataDocument document = dataStorage.readDocument(collectionName, null);

      for (String key : document.keySet()) {

         if (key.equals(attributeName) && attributeValues.size() < 100) {
            attributeValues.add(document.get(key));
         }
      }

      return attributeValues;
   }

   public void addColumn(final String collectionName, final String columnName) {
      //TODO
   }

   public void renameColumn(final String collectionName, final String origName, final String newName) {
      //TODO
   }

   public void dropColumn(final String collectionName, final String columnName) {
      //TODO
   }

   public void onCollectionEvent(@Observes(notifyObserver = Reception.IF_EXISTS) final CollectionEvent event) {
      // we do not care which collection got changed, we just invalidate our cache
      collections = null;
   }

}
