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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.provider.DataStorageProvider;
import io.lumeer.engine.rest.dao.Attribute;
import io.lumeer.engine.rest.dao.CollectionMetadata;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
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

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private ConfigurationFacade configurationFacade;

   // do not change collection names, because it can mess up internal name creation in method internalName()
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME1 = "CollectionMetadataFacadeCollečťion&-./ 1";
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME2 = "CollectionMetadataFacadeCollečtion&-./ 1";
   private final String COLLECTION_CREATE_INTERNAL_NAME = "collection.collectionmetadatafacadecollection_1_0";
   private final String COLLECTION_ATTRIBUTES_NAMES = "CollectionMetadataFacadeCollectionAttributesNames";
   private final String COLLECTION_ATTRIBUTES_INFO = "CollectionMetadataFacadeCollectionAttributesInfo";
   private final String COLLECTION_ATTRIBUTE_INFO = "CollectionMetadataFacadeCollectionAttributeInfo";
   private final String COLLECTION_RENAME_ATTRIBUTE = "CollectionMetadataFacadeCollectionRenameAttribute";
   private final String COLLECTION_DROP_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropAttribute";
   private final String COLLECTION_SET_ORIGINAL_NAME = "CollectionMetadataFacadeCollectionSetOriginalName";
   private final String COLLECTION_GET_INTERNAL_NAME = "CollectionMetadataFacadeCollectionGetInternalName";
   private final String COLLECTION_CREATE_INITIAL_METADATA = "CollectionMetadataFacadeCollectionCreateInitialMetadata";
   private final String COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionAddOrIncrementAttribute";
   private final String COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropOrDecrementAttribute";
   private final String COLLECTION_CHECK_ATTRIBUTES_VALUES = "CollectionMetadataFacadeCollectionCheckAttributesValues";
   private final String COLLECTION_LAST_TIME_USED = "CollectionMetadataFacadeCollectionLastTimeUsed";
   private final String COLLECTION_SET_GET_DROP_CUSTOM_METADATA = "CollectionMetadataFacadeCollectionSetGetDropCustomMetadata";
   private final String COLLECTION_ADD_ATTRIBUTE_CONSTRAINT = "CollectionMetadataFacadeCollectionAddAttributeConstraint";
   private final String COLLECTION_RECENTLY_USED_DOCUMENTS = "CollectionMetadataFacadeCollectionRecentlyUsedDocuments";

   @Test
   public void testCreateInternalName() throws Exception {
      dataStorage.dropCollection(COLLECTION_CREATE_INTERNAL_NAME);

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
      assertThat(metadata.getLastTimeUsed()).as("last time used").isBeforeOrEqualsTo(new Date());
      assertThat(metadata.getRecentlyUsedDocumentIds()).as("recently used documents").isEmpty();
      assertThat(metadata.getCreateDate()).as("create date").isBeforeOrEqualsTo(new Date());
      assertThat(metadata.getCreator()).as("create user").isEqualTo(userFacade.getUserEmail());
      assertThat(metadata.getCustomMetadata()).as("custom metadata").isEmpty();
   }

   @Test
   public void testGetAttributesNames() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_NAMES);

      String collection = collectionFacade.createCollection(COLLECTION_ATTRIBUTES_NAMES);

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      Set<String> attributes = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(attributes).containsOnly(name1, name2);
   }

   @Test
   public void testGetAttributesInfo() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_INFO);

      String collection = collectionFacade.createCollection(COLLECTION_ATTRIBUTES_INFO);

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
   public void testGetAttributeInfo() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTE_INFO);

      String collection = collectionFacade.createCollection(COLLECTION_ATTRIBUTE_INFO);

      // nested attribute
      String parent = "attribute";
      String child = "child";
      String attributeName = parent + "." + child;
      Attribute attribute = collectionMetadataFacade.getAttributeInfo(collection, attributeName);

      // non existing
      assertThat(attribute).isNull();

      collectionMetadataFacade.addOrIncrementAttribute(collection, parent);
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeName);
      attribute = collectionMetadataFacade.getAttributeInfo(collection, attributeName);

      // existing
      assertThat(attribute).isNotNull();
      assertThat(attribute.getNameWithoutParent()).isEqualTo(child);

      // double nested attribute
      String child2 = "child 2";
      String attributeName2 = attributeName + "." + child2;
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeName2);
      attribute = collectionMetadataFacade.getAttributeInfo(collection, attributeName2);

      assertThat(attribute).isNotNull();
      assertThat(attribute.getNameWithoutParent()).isEqualTo(child2);
   }

   @Test
   public void testRenameAttribute() throws Exception {
      setUpCollection(COLLECTION_RENAME_ATTRIBUTE);

      String collection = collectionFacade.createCollection(COLLECTION_RENAME_ATTRIBUTE);

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

      // nested attribute
      String oldNameNested = newName + ".child";
      String newNameNested = newName + ".child new";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldNameNested);
      collectionMetadataFacade.renameAttribute(collection, oldNameNested, newNameNested);
      Attribute attribute = collectionMetadataFacade.getAttributeInfo(collection, newNameNested);

      assertThat(attribute).isNotNull();
   }

   @Test
   public void testDropAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_ATTRIBUTE);

      String collection = collectionFacade.createCollection(COLLECTION_DROP_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.dropAttribute(collection, name);

      Set<String> columns = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(columns).isEmpty();

      // we try to drop non existing attribute - nothing happens
      collectionMetadataFacade.dropAttribute(collection, "attribute2");

      // nested attribute
      String nestedAttribute = name + ".child";
      assertThat(collectionMetadataFacade.getAttributeInfo(collection, nestedAttribute)).isNull();

      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.dropAttribute(collection, nestedAttribute);

      assertThat(collectionMetadataFacade.getAttributeInfo(collection, nestedAttribute)).isNull();
      assertThat(collectionMetadataFacade.getAttributeInfo(collection, name)).isNotNull(); // parent is not changed

   }

   @Test
   public void testSetGetOriginalCollectionName() throws Exception {
      setUpCollection(COLLECTION_SET_ORIGINAL_NAME);

      String collection = collectionFacade.createCollection(COLLECTION_SET_ORIGINAL_NAME);
      String newName = "my great collection";
      collectionMetadataFacade.setOriginalCollectionName(collection, newName);
      String realName = collectionMetadataFacade.getOriginalCollectionName(collection);

      assertThat(newName).isEqualTo(realName);
   }

   @Test
   public void testGetInternalCollectionName() throws Exception {
      setUpCollection(COLLECTION_GET_INTERNAL_NAME);

      String ourInternalName = collectionFacade.createCollection(COLLECTION_GET_INTERNAL_NAME);
      String realInternalName = collectionMetadataFacade.getInternalCollectionName(COLLECTION_GET_INTERNAL_NAME);

      assertThat(ourInternalName).isEqualTo(realInternalName);
   }

   @Test
   public void testAddOrIncrementAttribute() throws Exception {
      setUpCollection(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);

      String collection = collectionFacade.createCollection(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      long count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(1);

      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(2);

      // nested attribute
      String nestedAttribute = name + ".child";
      count = collectionMetadataFacade.getAttributeCount(collection, nestedAttribute);

      assertThat(count).isZero();

      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      count = collectionMetadataFacade.getAttributeCount(collection, nestedAttribute);

      assertThat(count).isEqualTo(1);
   }

   @Test
   public void testDropOrDecrementAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE);

      String collection = collectionFacade.createCollection(COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE);

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

      // nested attribute
      String nestedAttribute = name + ".child";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.dropOrDecrementAttribute(collection, nestedAttribute);

      count = collectionMetadataFacade.getAttributeCount(collection, nestedAttribute);

      assertThat(count).isZero();
   }

   @Test
   public void testGetSetDropCustomMetadata() throws Exception {
      setUpCollection(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);

      String collection = collectionFacade.createCollection(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);

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
      setUpCollection(COLLECTION_CHECK_ATTRIBUTES_VALUES);

      String collection = collectionFacade.createCollection(COLLECTION_CHECK_ATTRIBUTES_VALUES);

      String attribute = "attribute";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute);
      String constraint1 = "greaterThan:3";
      collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint1);
      int valueValid1 = 4;
      int valueInvalid1 = 2;

      String attribute2 = "attribute2";
      String child = "child";
      String nestedAttribute = attribute2 + "." + child;
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute2);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      String constraint2 = "lessThan:8";
      collectionMetadataFacade.addAttributeConstraint(collection, nestedAttribute, constraint2);
      int valueValid2 = 4;
      int valueInvalid2 = 9;

      String attribute3 = "attribute3";
      String nestedAttribute2 = attribute3 + "." + child;
      String doubleChild = "double child";
      String doubleNestedAttribute = nestedAttribute2 + "." + doubleChild;
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute3);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute2);
      collectionMetadataFacade.addOrIncrementAttribute(collection, doubleNestedAttribute);
      String constraint3 = "isNumber";
      collectionMetadataFacade.addAttributeConstraint(collection, doubleNestedAttribute, constraint3);
      int valueValid3 = 5;
      String valueInvalid3 = "a";

      DataDocument validDoc = new DataDocument()
            .append(attribute, valueValid1)
            .append(attribute2,
                  new DataDocument(child, valueValid2))
            .append(attribute3,
                  new DataDocument(child,
                        new DataDocument(
                              doubleChild,
                              valueValid3)));

      DataDocument invalidDoc = new DataDocument()
            .append(attribute, valueInvalid1)
            .append(attribute2,
                  new DataDocument(child, valueInvalid2))
            .append(attribute3,
                  new DataDocument(child,
                        new DataDocument(
                              doubleChild,
                              valueInvalid3)));

      DataDocument validAfterConvert = collectionMetadataFacade.checkAndConvertAttributesValues(collection, validDoc);
      DataDocument invalidAfterConvert = collectionMetadataFacade.checkAndConvertAttributesValues(collection, invalidDoc);

      // we have to get values as strings, because ConstraintManager always returns strings as a result of validation
      assertThat(validAfterConvert.getString(attribute))
            .as("valid attribute")
            .isEqualTo(Integer.toString(valueValid1));
      assertThat(validAfterConvert.getDataDocument(attribute2).getString(child))
            .as("valid nested attribute")
            .isEqualTo(Integer.toString(valueValid2));
      assertThat(validAfterConvert.getDataDocument(attribute3).getDataDocument(child).getString(doubleChild))
            .as("valid double nested attribute")
            .isEqualTo(Integer.toString(valueValid3));

      assertThat(invalidAfterConvert.getString(attribute))
            .as("invalid attribute")
            .isNull();
      assertThat(invalidAfterConvert.getDataDocument(attribute2).getString(child))
            .as("invalid nested attribute")
            .isNull();
      assertThat(invalidAfterConvert.getDataDocument(attribute3).getDataDocument(child).getString(doubleChild))
            .as("invalid double nested attribute")
            .isNull();
   }

   @Test
   public void testGetSetLastTimeUsed() throws Exception {
      setUpCollection(COLLECTION_LAST_TIME_USED);

      String collection = collectionFacade.createCollection(COLLECTION_LAST_TIME_USED);

      collectionMetadataFacade.setLastTimeUsedNow(collection);
      Date last = collectionMetadataFacade.getLastTimeUsed(collection);

      assertThat(last).isAfterOrEqualsTo(collectionMetadataFacade.getCollectionMetadata(collection).getCreateDate());
      assertThat(new Date()).isAfterOrEqualsTo(last);
   }

   @Test
   public void testGetAddDropConstraint() throws Exception {
      setUpCollection(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);

      String collection = collectionFacade.createCollection(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);

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

      // nested attribute
      String nestedAttribute = attribute + ".child";
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.addAttributeConstraint(collection, nestedAttribute, constraint1);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, nestedAttribute);
      assertThat(constraints).containsOnly(constraint1);
   }

   @Test
   public void testGetAddRecentlyUsedDocumentsIds() throws Exception {
      setUpCollection(COLLECTION_RECENTLY_USED_DOCUMENTS);

      String collection = collectionFacade.createCollection(COLLECTION_RECENTLY_USED_DOCUMENTS);

      List<String> ids = new ArrayList<>();

      // we add so many ids, as is the size of the list
      for (int i = 0; i < configurationFacade.getConfigurationInteger(LumeerConst.NUMBER_OF_RECENT_DOCS_PROPERTY).get(); i++) {
         String id = "id" + i;
         ids.add(id);
         collectionMetadataFacade.addRecentlyUsedDocumentId(collection, id);
      }

      List<String> recentlyUsed = collectionMetadataFacade.getRecentlyUsedDocumentsIds(collection);
      assertThat(recentlyUsed).containsOnlyElementsOf(ids); // all ids are there
      assertThat(recentlyUsed.get(0)).isEqualTo(ids.get(ids.size() - 1)); // the last one added is at the beginning of the list

      collectionMetadataFacade.addRecentlyUsedDocumentId(collection, ids.get(1)); // we add id1 again
      List<String> recentlyUsed1 = collectionMetadataFacade.getRecentlyUsedDocumentsIds(collection);
      assertThat(recentlyUsed1.get(0)).isEqualTo(ids.get(1)); // now id1 is at the beginning of the list

      String newId = "new id";
      collectionMetadataFacade.addRecentlyUsedDocumentId(collection, newId); // we add totally new id so we exceed the capacity of the list
      List<String> recentlyUsed2 = collectionMetadataFacade.getRecentlyUsedDocumentsIds(collection);
      assertThat(recentlyUsed2.get(0)).isEqualTo(newId); // new id is at the beginning of the list
      assertThat(recentlyUsed2).doesNotContain(ids.get(0)); // the first (and firstly added) id is no more in the list
   }

   private String internalName(String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

   private void setUpCollection(String originalCollectionName) {
      dataStorage.dropCollection(internalName(originalCollectionName));
   }
}
