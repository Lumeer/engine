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
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Named
@SessionScoped
public class CollectionFacade implements Serializable {

   private static final long serialVersionUID = 8967474543742743308L;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private UserFacade userFacade;

   // cache of collections - keys are internal names, values are original names
   private Map<String, String> collections;

   /**
    * Returns a Map object of collection names in the database except of metadata collections.
    *
    * @return the map of collection names. Keys are internal names, values are original names.
    */
   public Map<String, String> getAllCollections() {
      if (this.collections == null) {
         List<String> collectionsAll = dataStorage.getAllCollections();
         collections = new HashMap<>();

         // filters out metadata, shadow and other collections
         for (String collection : collectionsAll) {
            if (collectionMetadataFacade.isUserCollection(collection)) {
               String originalCollectionName = null;
               try {
                  originalCollectionName = collectionMetadataFacade.getOriginalCollectionName(collection);
               }
               // metadata was not found for some user collection, so we won't inform about that collection
               catch (CollectionMetadataDocumentNotFoundException e) {
                  continue;
               } catch (CollectionNotFoundException e) {
                  continue;
               }
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
    * @return name of internal collection
    * @throws CollectionAlreadyExistsException
    *       if collection with created internal name already exists
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given user name already exists
    * @throws CollectionNotFoundException
    *       when newly created metadata collection for the collection was not found and initial metadata could not be created
    */
   public String createCollection(final String collectionOriginalName) throws CollectionAlreadyExistsException, UserCollectionAlreadyExistsException, CollectionNotFoundException {
      String internalCollectionName = collectionMetadataFacade.createInternalName(collectionOriginalName);

      if (!dataStorage.hasCollection(internalCollectionName)) {
         dataStorage.createCollection(internalCollectionName);
         dataStorage.createCollection(collectionMetadataFacade.collectionMetadataCollectionName(internalCollectionName)); // creates metadata collection
         collectionMetadataFacade.createInitialMetadata(internalCollectionName, collectionOriginalName);

         collections = null;
      } else {
         throw new CollectionAlreadyExistsException(ErrorMessageBuilder.collectionAlreadyExistsString(internalCollectionName));
      }
      return internalCollectionName;
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param collectionName
    *       internal name of the collection to update
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the collection
    */
   public void dropCollection(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (dataStorage.hasCollection(collectionName)) {
         if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, getCurrentUser())) {
            throw new UnauthorizedAccessException();
         }

         linkingFacade.dropCollectionLinks(collectionName);
         dropCollectionMetadata(collectionName);
         dataStorage.dropCollection(collectionName);
         dataStorage.dropCollection(collectionName + ".shadow"); // TODO: find more intelligent way to drop shadow collection

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
    *       if collection was not found in database
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the collection
    */
   public List<DataDocument> readCollectionMetadata(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (dataStorage.hasCollection(collectionName)) {
         if (!collectionMetadataFacade.checkCollectionForRead(collectionName, getCurrentUser())) {
            throw new UnauthorizedAccessException();
         }
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
    *       if collection was not found in database
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the collection
    */
   public List<String> readCollectionAttributes(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (dataStorage.hasCollection(collectionName)) {
         if (!collectionMetadataFacade.checkCollectionForRead(collectionName, getCurrentUser())) {
            throw new UnauthorizedAccessException();
         }
         return collectionMetadataFacade.getCollectionAttributesNames(collectionName);
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
    *       if collection was not found in database
    */
   @Deprecated // CollectionMetadataFacade provides specific methods for metadata update
   public void updateCollectionMetadata(final String collectionName, final DataDocument element, String elementId) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
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
    *       if collection was not found in database
    */
   private void dropCollectionMetadata(final String collectionName) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         dataStorage.dropCollection(collectionMetadataFacade.collectionMetadataCollectionName(collectionName));
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Gets the first 100 distinct values of the given attribute in the given collection.
    * Access rights are not checked there because the method is to be used by HintFacade.
    *
    * @param collectionName
    *       the internal name of the collection where documents contain the given attribute
    * @param attributeName
    *       the name of the attribute
    * @return the distinct set of values of the given attribute
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    * @throws AttributeNotFoundException
    *       if attribute was not found in metadata collection
    */
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) throws CollectionNotFoundException, AttributeNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
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
    * Removes given attribute from all existing document specified by its id.
    *
    * @param collectionName
    *       the internal name of the collection where the given attribute should be removed
    * @param attributeName
    *       name of the attribute to remove
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the collection
    */
   public void dropAttribute(final String collectionName, final String attributeName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (dataStorage.hasCollection(collectionName)) {

         if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, getCurrentUser())) {
            throw new UnauthorizedAccessException();
         }

         collectionMetadataFacade.dropCollectionAttribute(collectionName, attributeName);
         List<DataDocument> documents = getAllDocuments(collectionName);

         for (DataDocument document : documents) {
            String id = document.getId();
            dataStorage.dropAttribute(collectionName, id, attributeName);
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
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    * @throws AttributeAlreadyExistsException
    *       when attribute with new name already exists
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the collection
    */
   public void renameAttribute(final String collectionName, final String origName, final String newName) throws CollectionNotFoundException, AttributeAlreadyExistsException, UnauthorizedAccessException {
      if (dataStorage.hasCollection(collectionName)) {

         if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, getCurrentUser())) {
            throw new UnauthorizedAccessException();
         }

         collectionMetadataFacade.renameCollectionAttribute(collectionName, origName, newName);
         dataStorage.renameAttribute(collectionName, origName, newName);
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
    * Finds out if given collection has specified attribute.
    *
    * @param collectionName
    *       name of the collection
    * @param attributeName
    *       name of the attribute
    * @return true if given collection has specified attribute
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    */
   private boolean isCollectionAttribute(String collectionName, String attributeName) throws CollectionNotFoundException {
      return collectionMetadataFacade.getCollectionAttributesNames(collectionName).contains(attributeName);
   }

   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }

}
