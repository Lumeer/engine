/*
 * -----------------------------------------------------------------------\
 * Lumeer
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

   @Inject
   // @Named("mongoDbStorage") // we have only one implementation, so mongo is automatically injected
   private DataStorage dataStorage;

   @Inject
   private MetadataFacade metadataFacade;

   // cache of collections - keys are internal names, values are original names
   private Map<String, String> collections;

   /**
    * @return map of collection names - keys are internal names, values are original names
    */
   @Produces
   @Named("userCollections")
   public Map<String, String> getAllCollections() {
      if (this.collections == null) {
         List<String> collectionsAll = dataStorage.getAllCollections();
         collections = new HashMap<>();

         // filters out metadata collections
         for (String collection : collectionsAll) {
            if (metadataFacade.isUserCollection(collection)) {
               String originalCollectionName = metadataFacade.getOriginalCollectionName(collection);
               collections.put(collection, originalCollectionName);
            }
         }
      }
      return collections;
   }

   /**
    * Creates a new collection including its metadata collection with the specified name.
    *
    * @param collectionOriginalName
    *       name of the collection to create (name given by user)
    */
   public void createCollection(final String collectionOriginalName) {
      String internalCollectionName = metadataFacade.collectionNameToInternalForm(collectionOriginalName);
      dataStorage.createCollection(internalCollectionName);
      dataStorage.createCollection(metadataFacade.collectionMetadataCollectionName(internalCollectionName)); // creates metadata collection
      metadataFacade.setOriginalCollectionName(collectionOriginalName);

      collections = null;
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param collectionName
    *       internal name of the collection to update
    */
   public void dropCollection(final String collectionName) {
      dataStorage.dropCollection(collectionName);
      dataStorage.dropCollection(metadataFacade.collectionMetadataCollectionName(collectionName)); // removes metadata collection

      collections = null;
   }

   /**
    * @param collectionName
    *       internal collection name
    * @return list of all documents from metadata collection
    */
   public List<DataDocument> readCollectionMetadata(final String collectionName) {
      return dataStorage.search(metadataFacade.collectionMetadataCollectionName(collectionName), null, null, 0, 0);
   }

   /**
    * @param collectionName
    *       internal collection name
    * @return list of names of all attributes in a collection
    */
   public List<String> readCollectionAttributes(final String collectionName) {
      return new ArrayList<>(metadataFacade.getCollectionColumnsInfo(collectionName).keySet());
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param element
    *       DataDocument with data to be updated
    * @param elementId
    *       id of document to be updated
    */
   public void updateCollectionMetadata(final String collectionName, final DataDocument element, String elementId) {
      dataStorage.updateDocument(metadataFacade.collectionMetadataCollectionName(collectionName), element, elementId);
   }

   /**
    * Drops collection metadata.
    *
    * @param collectionName
    *       internal collection name
    */
   public void dropCollectionMetadata(final String collectionName) {
      dataStorage.dropCollection(metadataFacade.collectionMetadataCollectionName(collectionName));
   }

   /**
    * Gets the first 100 distinct values of the given attribute in the given collection.
    *
    * @param collectionName
    *       the internal name of the collection where documents contain the given attribute
    * @param attributeName
    *       the name of the attribute
    * @return the distinct set of values of the given attribute
    */
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) {
      return dataStorage.getAttributeValues(collectionName, attributeName);
   }

   /**
    * Modifies all existing documents in given collection by adding a new attribute.
    *
    * @param collectionName
    *       internal name of the collection where the new column should be added
    * @param attributeName
    *       name of the column to add
    */
   public void addAttribute(final String collectionName, final String attributeName) {
      List<DataDocument> documents = getAllDocuments(collectionName);

      for (DataDocument document : documents) {
         String id = (document.get("_id")).toString();

         // TODO: check, if the column name already exists
         // TODO: update metadata (with MetadataFacade)

         document.put(attributeName, ""); // blank column value
         dataStorage.updateDocument(collectionName, document, id);
      }
   }

   /**
    * Removes given attribute from all existing document specified by its id.
    *
    * @param collectionName
    *       the internal name of the collection where the given attribute should be removed
    * @param attributeName
    *       name of the column to remove
    */
   public void dropAttribute(final String collectionName, final String attributeName) {
      List<DataDocument> documents = getAllDocuments(collectionName);

      for (DataDocument document : documents) {
         String id = (document.get("_id")).toString();

         // TODO: check, if the column name exists
         // TODO: update metadata (with MetadataFacade)

         dataStorage.removeAttribute(collectionName, id, attributeName);
      }
   }

   /**
    * Updates the name of an attribute which is found in all documents of given collection.
    *
    * @param collectionName
    *       internal name of the collection where the given attribute should be renamed
    * @param origName
    *       old name of an attribute
    * @param newName
    *       new name of an attribute
    */
   public void renameAttribute(final String collectionName, final String origName, final String newName) {
      // TODO: update metadata (with MetadataFacade)
      dataStorage.renameAttribute(collectionName, origName, newName);
   }

   public void onCollectionEvent(@Observes(notifyObserver = Reception.IF_EXISTS) final CollectionEvent event) {
      // we do not care which collection got changed, we just invalidate our cache
      collections = null;
   }

   private List<DataDocument> getAllDocuments(String collectionName) {
      return dataStorage.search(collectionName, null, null, 0, 0);
   }

}
