/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

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
   private DocumentFacade documentFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   @Inject
   private SecurityFacade securityFacade;

   /**
    * Gets limited info about all collections in database
    *
    * @param user
    *       User identificator
    * @param groups
    *       User groups
    * @param skip
    *       Number of skipped collections
    * @param size
    *       Max number of collections
    * @return list with metadata
    */
   public List<Collection> getCollections(String user, List<String> groups, int skip, int size) {
      return collectionMetadataFacade.getCollections(user, groups, skip, size);
   }

   /**
    * Finds and returns a collection by it's code
    *
    * @param collectionCode
    *       collection code
    */
   public Collection getCollection(final String collectionCode) {
      return collectionMetadataFacade.getCollection(collectionCode);
   }

   /**
    * Creates a new collection and its initial metadata.
    *
    * @param collection
    *       collection attributes
    * @return code
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given user name already exists
    */
   public String createCollection(Collection collection) throws UserCollectionAlreadyExistsException {
      Map<String, String> collectionsCodeAndName = collectionMetadataFacade.getCollectionsCodeName();
      if (collectionsCodeAndName.containsValue(collection.getName())) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(collection.getName()));
      }

      String collectionCode;
      if (collection.getCode() != null) {
         collectionCode = collection.getCode();
      } else {
         Set<String> collectionsFromDb = new HashSet<>(dataStorage.getAllCollections());
         collectionCode = generateCollectionCodeHash(collection.getName());
         int i = 0;
         while (collectionsFromDb.contains(collectionCode)) {
            collectionCode = generateCollectionCodeHash(collection.getName() + i++);
         }
      }

      dataStorage.createCollection(collectionCode);
      List<String> roleNames = new ArrayList<>(LumeerConst.Security.RESOURCE_ROLES.get(LumeerConst.Security.COLLECTION_RESOURCE));
      String collectionId = collectionMetadataFacade.createInitialMetadata(collectionCode, collection, userFacade.getUserEmail(), roleNames);

      String project = projectFacade.getCurrentProjectCode();
      databaseInitializer.onCollectionCreated(project, collectionId);

      return collectionCode;
   }

   /**
    * Creates a new collection and its initial metadata.
    *
    * @param collectionCode
    *       collection code
    * @param collection
    *       collection attributes
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given user name already exists
    */
   public void updateCollection(String collectionCode, Collection collection) throws UserCollectionAlreadyExistsException {
      Map<String, String> collectionsCodeAndName = collectionMetadataFacade.getCollectionsCodeName();
      collectionsCodeAndName.remove(collectionCode);

      if (collectionsCodeAndName.containsValue(collection.getName())) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(collection.getName()));
      }

      if (collection.getCode() != null && !collectionCode.equals(collection.getCode())) {
         dataStorage.renameCollection(collectionCode, collection.getCode());
      }
      collectionMetadataFacade.updateMetadata(collectionCode, collection);
   }

   /**
    * Drops the collection, its links, metadata and shadow.
    *
    * @param collectionCode
    *       collection code
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropCollection(final String collectionCode) throws DbException {
      if (!dataStorage.hasCollection(collectionCode)) {
         return;
      }

      linkingFacade.dropLinksForCollection(collectionCode, null, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.dropLinksForCollection(collectionCode, null, LumeerConst.Linking.LinkDirection.TO);
      collectionMetadataFacade.dropMetadata(collectionCode);
      dataStorage.dropCollection(collectionCode);
      versionFacade.trashShadowCollection(collectionCode);
   }

   public boolean hasCollection(String collectionCode) {
      return collectionMetadataFacade.getCollectionsCodeName().containsKey(collectionCode);
   }

   /**
    * Reads all attributes of given collection.
    *
    * @param collectionCode
    *       collection code
    * @return list of collection attributes
    */
   public List<Attribute> readCollectionAttributes(final String collectionCode) {
      return collectionMetadataFacade.getAttributesInfo(collectionCode);
   }

   /**
    * Gets the first 100 distinct values of the given attribute in the given collection.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       the name of the attribute
    * @return the distinct set of values of the given attribute
    * @throws AttributeNotFoundException
    *       if attribute was not found in metadata collection
    */
   public Set<String> getAttributeValues(final String collectionCode, final String attributeName) throws AttributeNotFoundException {
      if (collectionMetadataFacade.getAttributesNames(collectionCode).contains(attributeName)) {
         return dataStorage.getAttributeValues(collectionCode, attributeName);
      } else {
         throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundInColString(attributeName, collectionCode));
      }
   }

   /**
    * Removes given attribute from all existing documents in a collection.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       name of the attribute to remove
    */
   public void dropAttribute(final String collectionCode, final String attributeName) {
      collectionMetadataFacade.dropAttribute(collectionCode, attributeName);
      List<DataDocument> documents = documentFacade.getAllDocuments(collectionCode);

      for (DataDocument document : documents) {
         dataStorage.dropAttribute(collectionCode, dataStorageDialect.documentIdFilter(document.getId()), attributeName);
      }

      collectionMetadataFacade.setLastTimeUsedNow(collectionCode);
   }

   /**
    * Updates the name of an attribute in all documents of given collection.
    *
    * @param collectionCode
    *       collection code
    * @param origName
    *       old name of an attribute
    * @param newName
    *       new name of an attribute
    * @throws AttributeAlreadyExistsException
    *       when attribute with new name already exists
    */
   public void renameAttribute(final String collectionCode, final String origName, final String newName) throws AttributeAlreadyExistsException {
      collectionMetadataFacade.renameAttribute(collectionCode, origName, newName);
      dataStorage.renameAttribute(collectionCode, origName, newName);

      collectionMetadataFacade.setLastTimeUsedNow(collectionCode);
   }

   /**
    * Adds new constraint for given attribute.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       string with constraint configuration
    * @return true if add is successful, false if some of existing values in collection does not satisfy new constraint
    * @throws InvalidConstraintException
    *       when given constraint configuration is not valid
    */
   public boolean addAttributeConstraint(final String collectionCode, final String attributeName, final String constraintConfiguration) throws InvalidConstraintException {
      // we check if attribute value in all existing documents satisfies new constraint
      ConstraintManager constraintManager = new ConstraintManager(Collections.singletonList(constraintConfiguration));

      List<DataDocument> allDocuments = documentFacade.getAllDocuments(collectionCode);
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

      collectionMetadataFacade.addAttributeConstraint(collectionCode, attributeName, constraintConfiguration);

      collectionMetadataFacade.setLastTimeUsedNow(collectionCode);
      return true;
   }

   /**
    * Drops given constraint configuration from attribute list of constraints
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       constraint configuration to drop
    */
   public void dropAttributeConstraint(final String collectionCode, final String attributeName, final String constraintConfiguration) {
      collectionMetadataFacade.dropAttributeConstraint(collectionCode, attributeName, constraintConfiguration);
      collectionMetadataFacade.setLastTimeUsedNow(collectionCode);
   }

   private static String generateCollectionCodeHash(String collectionName) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-512");

         md.update(collectionName.getBytes());
         byte byteData[] = md.digest();

         //convert the byte to hex format method 1
         StringBuilder hashCodeBuffer = new StringBuilder();
         for (final byte aByteData : byteData) {
            hashCodeBuffer.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
         }
         return hashCodeBuffer.toString().substring(0, 16);
      } catch (NoSuchAlgorithmException e) {
         return null;
      }
   }
}
