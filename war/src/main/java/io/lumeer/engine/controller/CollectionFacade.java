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
import io.lumeer.engine.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.exception.AttributeNotFoundException;
import io.lumeer.engine.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.exception.CollectionNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;

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

   private final String ID_COLUMN_KEY = "_id";
   private final String DEFAULT_COLUMN_VALUE = "";
   private final String DEFAULT_COLUMN_TYPE = "";

   @Inject
   // @Named("mongoDbStorage") // we have only one implementation, so mongo is automatically injected
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   // cache of collections - keys are internal names, values are original names
   private Map<String, String> collections;

   /**
    * Returns a Map object of collection names in the database except of metadata collections.
    *
    * @return the map of collection names. Keys are internal names, values are original names.
    */
   @Produces
   @Named("userCollections")
   public Map<String, String> getAllCollections() {
      if (this.collections == null) {
         List<String> collectionsAll = dataStorage.getAllCollections();
         collections = new HashMap<>();

         // filters out metadata collections
         for (String collection : collectionsAll) {
            if (collectionMetadataFacade.isUserCollection(collection)) {
               String originalCollectionName = collectionMetadataFacade.getOriginalCollectionName(collection);
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
    * @throws CollectionAlreadyExistsException
    */
   public void createCollection(final String collectionOriginalName) throws CollectionAlreadyExistsException {
      String internalCollectionName = collectionMetadataFacade.collectionNameToInternalForm(collectionOriginalName);

      if (!isDatabaseCollection(internalCollectionName)) {
         dataStorage.createCollection(internalCollectionName);
         dataStorage.createCollection(collectionMetadataFacade.collectionMetadataCollectionName(internalCollectionName)); // creates metadata collection
         collectionMetadataFacade.createInitialMetadata(collectionOriginalName);

         collections = null;
      } else {
         throw new CollectionAlreadyExistsException(ErrorMessageBuilder.collectionAlreadyExistsString(internalCollectionName));
      }
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param collectionName
    *       internal name of the collection to update
    * @throws CollectionNotFoundException
    */
   public void dropCollection(final String collectionName) throws CollectionNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         dataStorage.dropCollection(collectionName);
         dataStorage.dropCollection(collectionMetadataFacade.collectionMetadataCollectionName(collectionName)); // removes metadata collection

         collections = null;
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Reads a metadata collection of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @return list of all documents from metadata collection
    * @throws CollectionNotFoundException
    */
   public List<DataDocument> readCollectionMetadata(final String collectionName) throws CollectionNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         return dataStorage.search(collectionMetadataFacade.collectionMetadataCollectionName(collectionName), null, null, 0, 0);
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Reads all collection attributes of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @return list of names of all attributes in a collection
    * @throws CollectionNotFoundException
    */
   public List<String> readCollectionAttributes(final String collectionName) throws CollectionNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         return new ArrayList<>(collectionMetadataFacade.getCollectionAttributesInfo(collectionName).keySet());
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Modifies a metadata collection of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @param element
    *       DataDocument with data to be updated
    * @param elementId
    *       id of document to be updated
    * @throws CollectionNotFoundException
    */
   @Deprecated // CollectionMetadataFacade provides specific methods for metadata update
   public void updateCollectionMetadata(final String collectionName, final DataDocument element, String elementId) throws CollectionNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         dataStorage.updateDocument(collectionMetadataFacade.collectionMetadataCollectionName(collectionName), element, elementId, -1);
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Removes a metadata collection of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @throws CollectionNotFoundException
    */
   public void dropCollectionMetadata(final String collectionName) throws CollectionNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         dataStorage.dropCollection(collectionMetadataFacade.collectionMetadataCollectionName(collectionName));
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Gets the first 100 distinct values of the given attribute in the given collection.
    *
    * @param collectionName
    *       the internal name of the collection where documents contain the given attribute
    * @param attributeName
    *       the name of the attribute
    * @return the distinct set of values of the given attribute
    * @throws CollectionNotFoundException
    * @throws AttributeNotFoundException
    */
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) throws CollectionNotFoundException, AttributeNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         if (isCollectionAttribute(collectionName, attributeName)) {
            return dataStorage.getAttributeValues(collectionName, attributeName);
         } else {
            throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundString(attributeName, collectionName));
         }
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Modifies all existing documents in given collection by adding a new attribute.
    *
    * @param collectionName
    *       internal name of the collection where the new attribute should be added
    * @param attributeName
    *       name of the attribute to add
    * @throws AttributeAlreadyExistsException
    * @throws CollectionNotFoundException
    */
   public void addAttribute(final String collectionName, final String attributeName) throws CollectionNotFoundException, AttributeAlreadyExistsException {
      if (isDatabaseCollection(collectionName)) {
         if (collectionMetadataFacade.addCollectionAttribute(collectionName, attributeName, DEFAULT_COLUMN_TYPE)) { // true if attribute doesn't exist in the collection
            List<DataDocument> documents = getAllDocuments(collectionName);

            for (DataDocument document : documents) {
               String id = document.get(ID_COLUMN_KEY).toString();

               document.put(attributeName, DEFAULT_COLUMN_VALUE); // blank attribute value
               dataStorage.updateDocument(collectionName, document, id, -1);
            }
         } else {
            throw new AttributeAlreadyExistsException(ErrorMessageBuilder.attributeAlreadyExistsString(attributeName, collectionName));
         }
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Removes given attribute from all existing document specified by its id.
    *
    * @param collectionName
    *       the internal name of the collection where the given attribute should be removed
    * @param attributeName
    *       name of the attribute to remove
    * @throws AttributeNotFoundException
    * @throws CollectionNotFoundException
    */
   public void dropAttribute(final String collectionName, final String attributeName) throws CollectionNotFoundException, AttributeNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         if (collectionMetadataFacade.dropCollectionAttribute(collectionName, attributeName)) { // true if attribute exists in the collection metadata
            List<DataDocument> documents = getAllDocuments(collectionName);

            for (DataDocument document : documents) {
               String id = document.get(ID_COLUMN_KEY).toString();

               dataStorage.removeAttribute(collectionName, id, attributeName);
            }
         } else {
            throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundString(attributeName, collectionName));
         }
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
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
    * @throws AttributeNotFoundException
    * @throws CollectionNotFoundException
    */
   public void renameAttribute(final String collectionName, final String origName, final String newName) throws CollectionNotFoundException, AttributeNotFoundException {
      if (isDatabaseCollection(collectionName)) {
         if (collectionMetadataFacade.renameCollectionAttribute(collectionName, origName, newName)) { // true if attribute exists in the collection metadata
            dataStorage.renameAttribute(collectionName, origName, newName);
         } else {
            throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundString(origName, collectionName));
         }
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   public void onCollectionEvent(@Observes(notifyObserver = Reception.IF_EXISTS) final CollectionEvent event) {
      // we do not care which collection got changed, we just invalidate our cache
      collections = null;
   }

   /**
    * Returns a list of all DataDocument objects in given collection.
    *
    * @param collectionName
    *       name of the collection
    * @return list of all documents
    */
   private List<DataDocument> getAllDocuments(String collectionName) {
      return dataStorage.search(collectionName, null, null, 0, 0);
   }

   /**
    * Finds out if database has given collection.
    *
    * @param collectionName
    *       name of the collection
    * @return true if database has given collection
    */
   public boolean isDatabaseCollection(String collectionName) {
      return getAllCollections().containsKey(collectionName);
   }

   /**
    * Finds out if given collection has specified attribute.
    *
    * @param collectionName
    *       name of the collection
    * @param attributeName
    *       name of the attribute
    * @return true if given collection has specified attribute
    * @throws CollectionNotFoundException
    */
   private boolean isCollectionAttribute(String collectionName, String attributeName) throws CollectionNotFoundException {
      return readCollectionAttributes(collectionName).contains(attributeName);
   }

}
