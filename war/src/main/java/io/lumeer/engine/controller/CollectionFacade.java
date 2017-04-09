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

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.Constraint;
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.ChangeCollectionName;
import io.lumeer.engine.api.event.CreateCollection;
import io.lumeer.engine.api.event.DropCollection;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.rest.dao.Attribute;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private Event<CreateCollection> createCollectionEvent;

   @Inject
   private Event<DropCollection> dropCollectionEvent;

   // cache of collections - keys are internal names, values are original names
   private Map<String, String> collections;

   /**
    * Returns a Map object of collection names in the database.
    *
    * @return the map of collection names. Keys are internal names, values are original names.
    */
   public Map<String, String> getAllCollections() {
      List<DataDocument> result = dataStorage.searchIncludeAttrs(
            LumeerConst.Collection.METADATA_COLLECTION,
            null,
            Arrays.asList(
                  LumeerConst.Collection.INTERNAL_NAME_KEY,
                  LumeerConst.Collection.REAL_NAME_KEY));

      Map<String, String> collections = new HashMap<>();

      for (DataDocument d : result) {
         collections.put(
               d.getString(LumeerConst.Collection.INTERNAL_NAME_KEY),
               d.getString(LumeerConst.Collection.REAL_NAME_KEY));
      }

      this.collections = collections;
      return collections;
   }

   /**
    * Returns a list of collection names sorted by last time used date in descending order.
    *
    * @return a list of internal collection names.
    */
   public List<String> getAllCollectionsByLastTimeUsed() {
      List<DataDocument> result = dataStorage.search(
            LumeerConst.Collection.METADATA_COLLECTION,
            null,
            dataStorageDialect.documentFieldSort(LumeerConst.Collection.LAST_TIME_USED_KEY, LumeerConst.SORT_DESCENDING_ORDER),
            0, 0);

      List<String> collections = new ArrayList<>();

      for (DataDocument d : result) {
         collections.add(d.getString(LumeerConst.Collection.INTERNAL_NAME_KEY));
      }

      return collections;
   }

   /**
    * Creates a new collection and its initial metadata.
    *
    * @param collectionOriginalName
    *       name of the collection to create (name given by user)
    * @return name of internal collection
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given user name already exists
    */
   public String createCollection(final String collectionOriginalName) throws UserCollectionAlreadyExistsException {
      String internalCollectionName = collectionMetadataFacade.createInternalName(collectionOriginalName);

      dataStorage.createCollection(internalCollectionName);
      collectionMetadataFacade.createInitialMetadata(internalCollectionName, collectionOriginalName);

      createCollectionEvent.fire(new CreateCollection(collectionOriginalName, internalCollectionName));

      return internalCollectionName;
   }

   /**
    * Drops the collection, its links, metadata and shadow.
    *
    * @param collectionName
    *       internal name of the collection to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropCollection(final String collectionName) throws DbException {
      if (!dataStorage.hasCollection(collectionName)) {
         return;
      }
      linkingFacade.dropCollectionLinks(collectionName, null, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.dropCollectionLinks(collectionName, null, LumeerConst.Linking.LinkDirection.TO);
      dropCollectionMetadata(collectionName);
      dataStorage.dropCollection(collectionName);
      versionFacade.trashShadowCollection(collectionName);

      dropCollectionEvent.fire(new DropCollection(null, collectionName));
   }

   /**
    * Reads all attributes of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @return map, keys are attributes' names, values are objects with attributes' metadata
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   public Map<String, Attribute> readCollectionAttributes(final String collectionName) throws CollectionMetadataDocumentNotFoundException {
      return collectionMetadataFacade.getAttributesInfo(collectionName);
   }

   /**
    * Drops a metadata of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   private void dropCollectionMetadata(final String collectionName) throws CollectionMetadataDocumentNotFoundException {
      String documentId = collectionMetadataFacade.getCollectionMetadataDocument(collectionName).getId();
      dataStorage.dropDocument(LumeerConst.Collection.METADATA_COLLECTION, dataStorageDialect.documentIdFilter(documentId));
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
    *       if collection was not found in database
    * @throws AttributeNotFoundException
    *       if attribute was not found in metadata collection
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) throws CollectionNotFoundException, AttributeNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         if (collectionMetadataFacade.getAttributesNames(collectionName).contains(attributeName)) {
            return dataStorage.getAttributeValues(collectionName, attributeName);
         } else {
            throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundInColString(attributeName, collectionName));
         }
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Removes given attribute from all existing documents in a collection.
    *
    * @param collectionName
    *       the internal name of the collection where the given attribute should be removed
    * @param attributeName
    *       name of the attribute to remove
    * @throws CollectionNotFoundException
    *       if collection was not found in database
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   public void dropAttribute(final String collectionName, final String attributeName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         collectionMetadataFacade.dropAttribute(collectionName, attributeName);
         List<DataDocument> documents = getAllDocuments(collectionName);

         for (DataDocument document : documents) {
            dataStorage.dropAttribute(collectionName, dataStorageDialect.documentIdFilter(document.getId()), attributeName);
         }

         collectionMetadataFacade.setLastTimeUsedNow(collectionName);
      } else {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
   }

   /**
    * Updates the name of an attribute in all documents of given collection.
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
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   public void renameAttribute(final String collectionName, final String origName, final String newName) throws CollectionNotFoundException, AttributeAlreadyExistsException, CollectionMetadataDocumentNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         collectionMetadataFacade.renameAttribute(collectionName, origName, newName);
         dataStorage.renameAttribute(collectionName, origName, newName);

         collectionMetadataFacade.setLastTimeUsedNow(collectionName);
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
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   public boolean addAttributeConstraint(final String collectionName, final String attributeName, final String constraintConfiguration) throws CollectionNotFoundException, InvalidConstraintException, CollectionMetadataDocumentNotFoundException {
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

         collectionMetadataFacade.setLastTimeUsedNow(collectionName);
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
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata is not found
    */
   public void dropAttributeConstraint(final String collectionName, final String attributeName, final String constraintConfiguration) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (dataStorage.hasCollection(collectionName)) {
         collectionMetadataFacade.dropAttributeConstraint(collectionName, attributeName, constraintConfiguration);
         collectionMetadataFacade.setLastTimeUsedNow(collectionName);
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

}
