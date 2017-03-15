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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.provider.DataStorageProvider;
import io.lumeer.engine.rest.dao.Attribute;
import io.lumeer.engine.rest.dao.CollectionMetadata;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class CollectionMetadataFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   private DataStorage dataStorage;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserFacade userFacade;

   // do not change collection names, because it can mess up internal name creation in method internalName()
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME1 = "CollectionMetadataFacadeCollečťion&-./ 1";
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME2 = "CollectionMetadataFacadeCollečtion&-./ 1";
   private final String COLLECTION_CREATE_INTERNAL_NAME = "collection.collectionmetadatafacadecollection_1_0";
   private final String COLLECTION_ATTRIBUTES_NAMES = "CollectionMetadataFacadeCollectionAttributesNames";
   private final String COLLECTION_ATTRIBUTES_INFO = "CollectionMetadataFacadeCollectionAttributesInfo";
   private final String COLLECTION_RENAME_ATTRIBUTE = "CollectionMetadataFacadeCollectionRenameAttribute";
   private final String COLLECTION_DROP_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropAttribute";
   private final String COLLECTION_SET_ORIGINAL_NAME = "CollectionMetadataFacadeCollectionSetOriginalName";
   private final String COLLECTION_GET_INTERNAL_NAME = "CollectionMetadataFacadeCollectionGetInternalName";
   private final String COLLECTION_CREATE_INITIAL_METADATA = "CollectionMetadataFacadeCollectionCreateInitialMetadata";
   private final String COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionAddOrIncrementAttribute";
   private final String COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropOrDecrementAttribute";
   private final String COLLECTION_LAST_TIME_USED = "CollectionMetadataFacadeCollectionLastTimeUsed";
   private final String COLLECTION_SET_GET_DROP_CUSTOM_METADATA = "CollectionMetadataFacadeCollectionSetGetDropCustomMetadata";
   private final String COLLECTION_ADD_ATTRIBUTE_CONSTRAINT = "CollectionMetadataFacadeCollectionAddAttributeConstraint";

   @Before
   public void init() {
      dataStorage = dataStorageProvider.getUserStorage();
   }

   @Test
   public void testCreateInternalName() throws Exception {
      dataStorage.dropCollection(COLLECTION_CREATE_INTERNAL_NAME);
      dataStorage.dropCollection("meta." + COLLECTION_CREATE_INTERNAL_NAME);

      assertThat(collectionMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME1)).isEqualTo(COLLECTION_CREATE_INTERNAL_NAME);
      collectionFacade.createCollection(CREATE_INTERNAL_NAME_ORIGINAL_NAME1);

      // different original name, but will be converted to the same internal as previous one
      String internalName2 = "collection.collectionmetadatafacadecollection_1_1";
      assertThat(collectionMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME2)).isEqualTo(internalName2);

      assertThatThrownBy(() -> collectionMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME1))
            .isInstanceOf(UserCollectionAlreadyExistsException.class);
   }

   @Test
   public void testCreateInitialMetadata() throws Exception {
      String collection = internalName(COLLECTION_CREATE_INITIAL_METADATA);
      collectionMetadataFacade.createInitialMetadata(collection, COLLECTION_CREATE_INITIAL_METADATA);

      CollectionMetadata metadata = collectionMetadataFacade.getCollectionMetadata(collection);

      assertThat(metadata.getName()).as("real name").isEqualTo(COLLECTION_CREATE_INITIAL_METADATA);
      assertThat(metadata.getInternalName()).as("internal name").isEqualTo(collection);
      assertThat(metadata.getProjectId()).as("project id").isEqualTo(projectFacade.getCurrentProjectId());
      assertThat(metadata.getAttributes()).as("attributes").isEmpty();
      assertThat(metadata.getLastTimeUsed()).as("last time used").isNotEmpty();
      assertThat(metadata.getRecentlyUsedDocumentIds()).as("recently used documents").isEmpty();
      assertThat(metadata.getCreateDate()).as("create date").isNotEmpty();
      assertThat(metadata.getCreator()).as("create user").isEqualTo(userFacade.getUserEmail());
      assertThat(metadata.getCustomMetadata()).as("custom metadata").isEmpty();
   }

   @Test
   public void testGetCollectionAttributesNames() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_NAMES);

      collectionFacade.createCollection(COLLECTION_ATTRIBUTES_NAMES);
      String collection = internalName(COLLECTION_ATTRIBUTES_NAMES);

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      Set<String> attributes = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(attributes).containsOnly(name1, name2);
   }

   @Test
   public void testGetCollectionAttributesInfo() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_INFO);

      collectionFacade.createCollection(COLLECTION_ATTRIBUTES_INFO);
      String collection = internalName(COLLECTION_ATTRIBUTES_INFO);

      Map<String, Attribute> attributesInfo = collectionMetadataFacade.getAttributesInfo(collection);
      assertThat(attributesInfo).isEmpty();

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      attributesInfo = collectionMetadataFacade.getAttributesInfo(collection);

      assertThat(attributesInfo).hasSize(2);
   }

   @Test
   public void testRenameAttribute() throws Exception {
      setUpCollection(COLLECTION_RENAME_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_RENAME_ATTRIBUTE);
      String collection = internalName(COLLECTION_RENAME_ATTRIBUTE);

      String oldName = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldName);

      String newName = "attribute 2";
      collectionMetadataFacade.renameAttribute(collection, oldName, newName);
      Set<String> columns = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(columns).containsOnly(newName);

      String oldName2 = "attribute 3";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldName2);
      // we try to rename attribute to name that already exists in collection
      assertThatThrownBy(() -> collectionMetadataFacade.renameAttribute(collection, oldName2, newName))
            .isInstanceOf(AttributeAlreadyExistsException.class);
   }

   @Test
   public void testDropAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_DROP_ATTRIBUTE);
      String collection = internalName(COLLECTION_DROP_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.dropAttribute(collection, name);

      Set<String> columns = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(columns).isEmpty();

      // we try to drop non existing attribute - nothing happens
      collectionMetadataFacade.dropAttribute(collection, "attribute2");
   }

   @Test
   public void testSetGetOriginalCollectionName() throws Exception {
      setUpCollection(COLLECTION_SET_ORIGINAL_NAME);

      collectionFacade.createCollection(COLLECTION_SET_ORIGINAL_NAME);
      String collection = internalName(COLLECTION_SET_ORIGINAL_NAME);
      String newName = "my great collection";
      collectionMetadataFacade.setOriginalCollectionName(collection, newName);
      String realName = collectionMetadataFacade.getOriginalCollectionName(collection);

      assertThat(newName).isEqualTo(realName);
   }

   @Test
   public void testGetInternalCollectionName() throws Exception {
      setUpCollection(COLLECTION_GET_INTERNAL_NAME);

      collectionFacade.createCollection(COLLECTION_GET_INTERNAL_NAME);
      String ourInternalName = internalName(COLLECTION_GET_INTERNAL_NAME);
      String realInternalName = collectionMetadataFacade.getInternalCollectionName(COLLECTION_GET_INTERNAL_NAME);

      assertThat(ourInternalName).isEqualTo(realInternalName);
   }



   @Test
   public void testAddOrIncrementAttribute() throws Exception {
      setUpCollection(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);
      String collection = internalName(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      long count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(1);

      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(2);
   }

   @Test
   public void testDropOrDecrementAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE);
      String collection = internalName(COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, name);
      long count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(1);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);
      Set<String> attributeInfo = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(count).isZero();
      assertThat(attributeInfo).isEmpty();
   }

   @Test
   public void testGetSetDropCustomMetadata() throws Exception {
      setUpCollection(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);

      collectionFacade.createCollection(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);
      String collection = internalName(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);

      // there is no custom metadata - we should obtain empty list
      assertThat(collectionMetadataFacade.getCustomMetadata(collection)).isEmpty();

      String metaKey1 = "meta key 1";
      String metaValue1 = "value 1";

      // we set one custom value
      collectionMetadataFacade.setCustomMetadata(collection, new DataDocument(metaKey1, metaValue1));
      assertThat(collectionMetadataFacade.getCustomMetadata(collection).get(metaKey1).toString()).isEqualTo(metaValue1);

      // we try to drop non existing key, but dropAttribute in DataStorage does not return value, so we do not know it
      collectionMetadataFacade.dropCustomMetadata(collection, "random key");

      // we drop existing key and after that it is not there
      collectionMetadataFacade.dropCustomMetadata(collection, metaKey1);
      assertThat(collectionMetadataFacade.getCustomMetadata(collection)).doesNotContainKey(metaKey1);
   }

   @Test
   public void testIsUserCollection() {
      assertThat(collectionMetadataFacade.isUserCollection("collection.something")).isTrue();
      assertThat(collectionMetadataFacade.isUserCollection("something")).isFalse();
   }

   @Test
   public void testCheckAndConvertAttributesValues() throws Exception {
      // TODO !!!
   }

   @Test
   public void testGetAddDropConstraint() throws Exception {
      setUpCollection(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);

      collectionFacade.createCollection(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);
      String collection = internalName(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);

      String attribute = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute);

      List<String> constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).isEmpty();

      String constraint1 = "isNumber";
      String constraint2 = "lessThan:3";

      collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint1);
      collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint2);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).containsOnly(constraint1, constraint2);

      collectionMetadataFacade.dropAttributeConstraint(collection, attribute, constraint1);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).containsOnly(constraint2);

      collectionMetadataFacade.dropAttributeConstraint(collection, attribute, constraint2);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).isEmpty();

      // we try to add dummy constraint
      String constraint3 = "dummy";
      assertThatThrownBy(() -> collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint3))
            .isInstanceOf(InvalidConstraintException.class);
   }

   private String internalName(String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

   private void setUpCollection(String originalCollectionName) {
      dataStorage.dropCollection(internalName(originalCollectionName));
      dataStorage.dropCollection("meta." + internalName(originalCollectionName));
   }
}
