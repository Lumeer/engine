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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.Constraint;
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.ChangeCollectionName;
import io.lumeer.engine.api.event.CreateCollection;
import io.lumeer.engine.api.event.DropCollection;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
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

   @Inject
   private Event<CreateCollection> createCollectionEvent;

   @Inject
   private Event<DropCollection> dropCollectionEvent;

   // cache of collections - keys are internal names, values are original names
   private Map<String, String> collections;

   private long cacheLastUpdated = 0L;

   /**
    * Returns a Map object of collection names in the database except of metadata collections.
    *
    * @return the map of collection names. Keys are internal names, values are original names.
    */
   public Map<String, String> getAllCollections() {
      if (this.collections == null || cacheLastUpdated + 5000 < System.currentTimeMillis()) {
         cacheLastUpdated = System.currentTimeMillis();

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
      } else {
         throw new CollectionAlreadyExistsException(ErrorMessageBuilder.collectionAlreadyExistsString(internalCollectionName));
      }

      createCollectionEvent.fire(new CreateCollection(collectionOriginalName, internalCollectionName));

      return internalCollectionName;
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param collectionName
    *       internal name of the collection to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropCollection(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(collectionName)) {
         linkingFacade.dropCollectionLinks(collectionName, null, LumeerConst.Linking.LinkDirection.FROM);
         linkingFacade.dropCollectionLinks(collectionName, null, LumeerConst.Linking.LinkDirection.TO);
         dropCollectionMetadata(collectionName);
         dataStorage.dropCollection(collectionName);
         dataStorage.dropCollection(collectionName + LumeerConst.Collection.COLLECTION_SHADOW_SUFFIX); // TODO: find more intelligent way to drop shadow collection

         dropCollectionEvent.fire(new DropCollection(null, collectionName));
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
    */
   public List<DataDocument> readCollectionMetadata(final String collectionName) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
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
    */
   public List<String> readCollectionAttributes(final String collectionName) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         return collectionMetadataFacade.getCollectionAttributesNames(collectionName);
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
            throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundInColString(attributeName, collectionName));
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
    */
   public void dropAttribute(final String collectionName, final String attributeName) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
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
    */
   public void renameAttribute(final String collectionName, final String origName, final String newName) throws CollectionNotFoundException, AttributeAlreadyExistsException {
      if (dataStorage.hasCollection(collectionName)) {

         collectionMetadataFacade.renameCollectionAttribute(collectionName, origName, newName);
         dataStorage.renameAttribute(collectionName, origName, newName);
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Retypes given attribute. That means changing type in metadata and converting all existing values of the attribute to new type.
    *
    * @param collectionName
    *       internal name
    * @param attributeName
    *       attribute name
    * @param newType
    *       new type
    * @return true if retype was successful, false if new type is not valid or some values in collection cannot be converted to new type
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    */
   public boolean retypeAttribute(final String collectionName, final String attributeName, final String newType) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         if (!LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_VALUES.contains(newType)) { // new type is not from our list
            return false;
         }

         List<DataDocument> allDocuments = getAllDocuments(collectionName);
         List<Object> newValues = new ArrayList<>();
         boolean isValid = true;
         for (DataDocument document : allDocuments) {
            Object newValue = collectionMetadataFacade.checkValueTypeAndConvert(document.get(attributeName), newType);
            if (newValue == null) {
               isValid = false; // we have found invalid value, so the retype cannot be done
               break;
            }
            newValues.add(newValue);
         }

         if (isValid) {
            collectionMetadataFacade.retypeCollectionAttribute(collectionName, attributeName, newType);
            // TODO: How to use BatchFacade to retype value in all documents?
            for (int i = 0; i < allDocuments.size(); i++) {
               dataStorage.updateDocument(collectionName, new DataDocument(attributeName, newValues.get(i)), allDocuments.get(i).getId());
            }
            return true;
         } else {
            return false;
         }
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Adds new constraint for given attribute.
    *
    * @param collectionName
    *       internal name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       string with constraint configuration
    * @return true if add is successful, false if some of existing values in collection does not satisfy new constraint
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    * @throws InvalidConstraintException
    *       when given constraint configuration is not valid
    */
   public boolean addAttributeConstraint(final String collectionName, final String attributeName, final String constraintConfiguration) throws CollectionNotFoundException, InvalidConstraintException {
      if (dataStorage.hasCollection(collectionName)) {

         // we check if attribute value in all existing documents satisfies new constraint
         ConstraintManager constraintManager = new ConstraintManager(Collections.singletonList(constraintConfiguration));

         List<DataDocument> allDocuments = getAllDocuments(collectionName);
         for (DataDocument document : allDocuments) {
            // TODO: fix fixable value
            String value = document.get(attributeName).toString();
            if (value == null) { // document does not contain given attribute
               continue;
            }
            if (!(constraintManager.isValid(value) == Constraint.ConstraintResult.VALID)) {
               return false; // we have found invalid value, so the constraint cannot be added
            }
         }

         collectionMetadataFacade.addAttributeConstraint(collectionName, attributeName, constraintConfiguration);
         return true;
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Drops given constraint configuration from attribute list of constraints
    *
    * @param collectionName
    *       internal name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       constraint configuration to drop
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    */
   public void dropAttributeConstraint(final String collectionName, final String attributeName, final String constraintConfiguration) throws CollectionNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         collectionMetadataFacade.dropAttributeConstraint(collectionName, attributeName, constraintConfiguration);
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   public void onCollectionCreate(@Observes(notifyObserver = Reception.IF_EXISTS) final CreateCollection event) {
      if (collections != null) {
         collections.put(event.getInternalName(), event.getUserName());
      }
   }

   public void onCollectionDrop(@Observes(notifyObserver = Reception.IF_EXISTS) final DropCollection event) {
      if (collections != null) {
         collections.remove(event.getInternalName());
      }
   }

   public void onCollectionRename(@Observes(notifyObserver = Reception.IF_EXISTS) final ChangeCollectionName event) {
      if (collections != null) {
         collections.put(event.getInternalName(), event.getUserName());
      }
   }

   private void checkCollectionForWrite(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
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

}
