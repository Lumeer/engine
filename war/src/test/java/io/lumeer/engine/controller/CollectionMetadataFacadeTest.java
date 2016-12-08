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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.CollectionMetadataNotFoundException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.util.Utils;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionMetadataFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionMetadataFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   // do not change collection names, because it can mess up internal name creation in method internalName()
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME1 = "CollectionMetadataFacadeCollečťion&-./ 1";
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME2 = "CollectionMetadataFacadeCollečtion&-./ 1";
   private final String COLLECTION_CREATE_INTERNAL_NAME = "collection.collectionmetadatafacadecollection_1_0";
   private final String COLLECTION_ATTRIBUTES_NAMES = "CollectionMetadataFacadeCollectionAttributesNames";
   private final String COLLECTION_ATTRIBUTES_INFO = "CollectionMetadataFacadeCollectionAttributesInfo";
   private final String COLLECTION_RENAME_ATTRIBUTE = "CollectionMetadataFacadeCollectionRenameAttribute";
   private final String COLLECTION_RETYPE_ATTRIBUTE = "CollectionMetadataFacadeCollectionRetypeAttribute";
   private final String COLLECTION_DROP_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropAttribute";
   private final String COLLECTION_SET_ORIGINAL_NAME = "CollectionMetadataFacadeCollectionSetOriginalName";
   private final String COLLECTION_CREATE_INITIAL_METADATA = "CollectionMetadataFacadeCollectionCreateInitialMetadata";
   private final String COLLECTION_SET_LOCK_TIME = "CollectionMetadataFacadeCollectionSetLockTime";
   private final String COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionAddOrIncrementAttribute";
   private final String COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropOrDecrementAttribute";
   private final String COLLECTION_CHECK_ATTRIBUTE_VALUE = "CollectionMetadataFacadeCollectionCheckAttributeValue";
   //private final String COLLECTION_ADD_ATTRIBUTE_CONSTRAINT = "CollectionMetadataFacadeCollectionAddAttributeConstraint";

   @Test
   public void testCreateInternalName() throws Exception {
      dataStorage.dropCollection(COLLECTION_CREATE_INTERNAL_NAME);
      dataStorage.dropCollection("meta." + COLLECTION_CREATE_INTERNAL_NAME);

      Assert.assertEquals(collectionMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME1), COLLECTION_CREATE_INTERNAL_NAME);
      collectionFacade.createCollection(CREATE_INTERNAL_NAME_ORIGINAL_NAME1);

      // different original name, but will be converted to the same internal as previous one
      String internalName2 = "collection.collectionmetadatafacadecollection_1_1";
      Assert.assertEquals(collectionMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME2), internalName2);

      boolean pass = false;
      try {
         collectionMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME1);
      } catch (UserCollectionAlreadyExistsException e) {
         pass = true;
      }
      Assert.assertTrue(pass);
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

      List<String> attributes = collectionMetadataFacade.getCollectionAttributesNames(collection);

      Assert.assertEquals(attributes.size(), 2);
      Assert.assertTrue(attributes.contains(name1));
      Assert.assertTrue(attributes.contains(name2));
   }

   @Test
   public void testGetCollectionAttributesInfo() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_INFO);

      collectionFacade.createCollection(COLLECTION_ATTRIBUTES_INFO);
      String collection = internalName(COLLECTION_ATTRIBUTES_INFO);

      List<DataDocument> attributesInfo = collectionMetadataFacade.getCollectionAttributesInfo(collection);
      Assert.assertEquals(attributesInfo.size(), 0);

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      attributesInfo = collectionMetadataFacade.getCollectionAttributesInfo(collection);

      Assert.assertEquals(attributesInfo.size(), 2);
   }

   //   @Test
   //   public void testAddCollectionAttributeNew() throws CollectionAlreadyExistsException, CollectionNotFoundException {
   //      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);
   //      boolean add = collectionMetadataFacade.addCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, "column 1", "int", -1);
   //      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
   //
   //      Assert.assertTrue(add);
   //   }
   //
   //   @Test
   //   public void testAddCollectionAttributeExisting() throws CollectionAlreadyExistsException, CollectionNotFoundException {
   //      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);
   //      collectionMetadataFacade.addCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, "column 1", "int", -1);
   //      boolean add = collectionMetadataFacade.addCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, "column 1", "int", -1);
   //      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
   //
   //      Assert.assertFalse(add);
   //   }

   @Test
   public void testRenameCollectionAttribute() throws Exception {
      setUpCollection(COLLECTION_RENAME_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_RENAME_ATTRIBUTE);
      String collection = internalName(COLLECTION_RENAME_ATTRIBUTE);

      String oldName = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldName);

      String newName = "attribute 2";
      boolean rename = collectionMetadataFacade.renameCollectionAttribute(collection, oldName, newName);
      List<String> columns = collectionMetadataFacade.getCollectionAttributesNames(collection);

      Assert.assertTrue(columns.contains(newName));
      Assert.assertTrue(rename);

      // we try to rename non existing attribute
      boolean pass = false;
      try {
         collectionMetadataFacade.renameCollectionAttribute(collection, oldName, newName);
      } catch (CollectionMetadataNotFoundException e) {
         pass = true;
      }
      Assert.assertTrue(pass);
   }

   @Test
   public void testRetypeCollectionAttribute() throws Exception {
      setUpCollection(COLLECTION_RETYPE_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_RETYPE_ATTRIBUTE);
      String collection = internalName(COLLECTION_RETYPE_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      String oldType = "double";
      boolean retype = collectionMetadataFacade.retypeCollectionAttribute(collection, name, oldType);
      Assert.assertEquals(collectionMetadataFacade.getAttributeType(collection, name), oldType);
      Assert.assertTrue(retype);

      oldType = "int";
      retype = collectionMetadataFacade.retypeCollectionAttribute(collection, name, oldType);
      Assert.assertEquals(collectionMetadataFacade.getAttributeType(collection, name), oldType);
      Assert.assertTrue(retype);

      String newType = "hello";
      retype = collectionMetadataFacade.retypeCollectionAttribute(collection, name, newType);

      Assert.assertEquals(collectionMetadataFacade.getAttributeType(collection, name), oldType);
      Assert.assertFalse(retype);

      // we try to retype non existing attribute
      boolean pass = false;
      try {
         collectionMetadataFacade.retypeCollectionAttribute(collection, "attribute2", "int");
      } catch (CollectionMetadataNotFoundException e) {
         pass = true;
      }
      Assert.assertTrue(pass);
   }

   @Test
   public void testDropCollectionAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_DROP_ATTRIBUTE);
      String collection = internalName(COLLECTION_DROP_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      boolean drop = collectionMetadataFacade.dropCollectionAttribute(collection, name);

      List<String> columns = collectionMetadataFacade.getCollectionAttributesNames(collection);

      Assert.assertTrue(columns.isEmpty());
      Assert.assertTrue(drop);

      // we try to drop non existing attribute
      boolean pass = false;
      try {
         collectionMetadataFacade.dropCollectionAttribute(collection, "attribute2");
      } catch (CollectionMetadataNotFoundException e) {
         pass = true;
      }
      Assert.assertTrue(pass);
   }

   @Test
   public void testSetGetOriginalCollectionName() throws Exception {
      setUpCollection(COLLECTION_SET_ORIGINAL_NAME);

      collectionFacade.createCollection(COLLECTION_SET_ORIGINAL_NAME); // set is done in this method
      String collection = internalName(COLLECTION_SET_ORIGINAL_NAME);
      String realName = collectionMetadataFacade.getOriginalCollectionName(collection);

      Assert.assertEquals(COLLECTION_SET_ORIGINAL_NAME, realName);
   }

   @Test
   public void testCollectionMetadataCollectionName() {
      Assert.assertEquals(collectionMetadataFacade.collectionMetadataCollectionName("collection"), "meta.collection");
   }

   @Test
   public void testCreateInitialMetadata() throws Exception {
      String collection = internalName(COLLECTION_CREATE_INITIAL_METADATA);
      String metaCollection = collectionMetadataFacade.collectionMetadataCollectionName(collection);

      // set up collection (delete one created during previous test)
      dataStorage.dropCollection(metaCollection);

      dataStorage.createCollection(metaCollection);
      collectionMetadataFacade.createInitialMetadata(collection, COLLECTION_CREATE_INITIAL_METADATA);

      String name = collectionMetadataFacade.getOriginalCollectionName(collection);
      String lock = collectionMetadataFacade.getCollectionLockTime(collection);

      Assert.assertEquals(name, COLLECTION_CREATE_INITIAL_METADATA);
      Assert.assertNotEquals(lock, "");
   }

   @Test
   public void testSetGetCollectionLockTime() throws Exception {
      setUpCollection(COLLECTION_SET_LOCK_TIME);

      collectionFacade.createCollection(COLLECTION_SET_LOCK_TIME);
      String collection = internalName(COLLECTION_SET_LOCK_TIME);
      String time = Utils.getCurrentTimeString();
      collectionMetadataFacade.setCollectionLockTime(collection, time);
      String timeTest = collectionMetadataFacade.getCollectionLockTime(collection);

      Assert.assertEquals(time, timeTest);
   }

   @Test
   public void testAddOrIncrementAttribute() throws Exception {
      setUpCollection(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);
      String collection = internalName(COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      long count = collectionMetadataFacade.getAttributeCount(collection, name);

      Assert.assertEquals(count, 1);

      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);

      Assert.assertEquals(count, 2);
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

      Assert.assertEquals(count, 1);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);
      List<String> attributeInfo = collectionMetadataFacade.getCollectionAttributesNames(collection);

      Assert.assertEquals(count, 0);
      Assert.assertEquals(attributeInfo.size(), 0);
   }

   @Test
   public void testCheckAttributeValue() throws Exception {
      setUpCollection(COLLECTION_CHECK_ATTRIBUTE_VALUE);

      collectionFacade.createCollection(COLLECTION_CHECK_ATTRIBUTE_VALUE);
      String collection = internalName(COLLECTION_CHECK_ATTRIBUTE_VALUE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      String type = "double";
      collectionMetadataFacade.retypeCollectionAttribute(collection, name, type);
      boolean check = collectionMetadataFacade.checkAttributeValue(collection, name, "3.14");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "hm");
      Assert.assertFalse(check);

      type = "int";
      collectionMetadataFacade.retypeCollectionAttribute(collection, name, type);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "3");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "3.14");
      Assert.assertFalse(check);

      type = "long";
      collectionMetadataFacade.retypeCollectionAttribute(collection, name, type);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "12345678900");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "hm");
      Assert.assertFalse(check);

      type = "nested";
      collectionMetadataFacade.retypeCollectionAttribute(collection, name, type);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "hm");
      Assert.assertFalse(check);

      type = "date";
      collectionMetadataFacade.retypeCollectionAttribute(collection, name, type);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "2016.11.22");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "2016.11.22 21.36");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "hm");
      Assert.assertFalse(check);

      type = "bool";
      collectionMetadataFacade.retypeCollectionAttribute(collection, name, type);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "true");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(collection, name, "hm");
      Assert.assertFalse(check);
   }

   @Test
   public void testIsUserCollection() {
      Assert.assertTrue(collectionMetadataFacade.isUserCollection("collection.something"));
      Assert.assertFalse(collectionMetadataFacade.isUserCollection("something"));
   }

   //   @Test
   //   public void testAddGetAttributeConstraints() throws CollectionAlreadyExistsException, UserCollectionAlreadyExistsException, CollectionNotFoundException {
   //      setUpCollection(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);
   //
   //      collectionFacade.createCollection(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);
   //      String collection = internalName(COLLECTION_ADD_ATTRIBUTE_CONSTRAINT);
   //
   //      String name = "attribute 1";
   //      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
   //      collectionMetadataFacade.retypeCollectionAttribute(collection, name, collectionMetadataFacade.COLLECTION_ATTRIBUTE_TYPE_INT);
   //
   //      Assert.assertTrue(collectionMetadataFacade.getAttributeConstraints(collection, name).isEmpty());
   //
   //      boolean valid = collectionMetadataFacade.addAttributeConstraint(collection, name, collectionMetadataFacade.COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GT, "10");
   //      Assert.assertTrue(valid);
   //
   //      valid = collectionMetadataFacade.addAttributeConstraint(collection, name, collectionMetadataFacade.COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GT, "a");
   //      Assert.assertFalse(valid);
   //
   //      valid = collectionMetadataFacade.addAttributeConstraint(collection, name, collectionMetadataFacade.COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_REGEX, "a");
   //      Assert.assertFalse(valid);
   //
   //      Assert.assertEquals(collectionMetadataFacade.getAttributeConstraints(collection, name).size(), 1);
   //   }

   private String internalName(String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

   private void setUpCollection(String originalCollectionName) {
      dataStorage.dropCollection(internalName(originalCollectionName));
      dataStorage.dropCollection("meta." + internalName(originalCollectionName));
   }
}
