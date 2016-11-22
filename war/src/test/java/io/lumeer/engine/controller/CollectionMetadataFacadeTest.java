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
import io.lumeer.engine.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.exception.CollectionNotFoundException;
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
import java.util.Map;
import javax.inject.Inject;
import javax.xml.crypto.Data;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionMetadataFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionMetadataFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   private final String TEST_COLLECTION_INTERNAL_NAME = "collection.collection1";
   private final String TEST_COLLECTION_REAL_NAME = "Collection_1";
   private final String TEST_COLLECTION_METADATA_COLLECTION_NAME = "meta.collection.collection1";

   @Test
   public void testCollectionNameToInternalForm() throws Exception {
      String originalName = "čťH-%/e&äll o1";
      String newName = "collection.hello1";
      Assert.assertEquals(collectionMetadataFacade.collectionNameToInternalForm(originalName), newName);
   }

   @Test
   public void testGetCollectionAttributesNamesAndTypes() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name1);
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name2);

      Map<String, String> columnsInfo = collectionMetadataFacade.getCollectionAttributesNamesAndTypes(TEST_COLLECTION_INTERNAL_NAME);
      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);

      Assert.assertEquals(columnsInfo.size(), 2);
      Assert.assertTrue(columnsInfo.containsKey(name1));
      Assert.assertTrue(columnsInfo.containsKey(name2));
   }

   @Test
   public void testGetCollectionAttributesInfo() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      List<DataDocument> attributesInfo = collectionMetadataFacade.getCollectionAttributesInfo(TEST_COLLECTION_INTERNAL_NAME);
      Assert.assertEquals(attributesInfo.size(), 0);

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name1);
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name2);

      attributesInfo = collectionMetadataFacade.getCollectionAttributesInfo(TEST_COLLECTION_INTERNAL_NAME);
      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);

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
   public void testRenameCollectionAttribute() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String oldName = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, oldName);

      String newName = "attribute 2";
      boolean rename = collectionMetadataFacade.renameCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, oldName, newName);
      Map<String, String> columnsInfo = collectionMetadataFacade.getCollectionAttributesNamesAndTypes(TEST_COLLECTION_INTERNAL_NAME);
      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);

      Assert.assertTrue(columnsInfo.containsKey(newName));
      Assert.assertTrue(rename);
   }

   @Test
   public void testRetypeCollectionAttribute() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);

      String type = "double";
      boolean retype = collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      Map<String, String> columnsInfo = collectionMetadataFacade.getCollectionAttributesNamesAndTypes(TEST_COLLECTION_INTERNAL_NAME);

      Assert.assertEquals(columnsInfo.get(name), type);
      Assert.assertTrue(retype);

      String type2 = "hello";
      retype = collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type2);

      Assert.assertFalse(retype);

      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
   }

   @Test
   public void testDropCollectionAttribute() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);

      boolean drop = collectionMetadataFacade.dropCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name);
      Map<String, String> columnsInfo = collectionMetadataFacade.getCollectionAttributesNamesAndTypes(TEST_COLLECTION_INTERNAL_NAME);
      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);

      Assert.assertTrue(columnsInfo.isEmpty());
      Assert.assertTrue(drop);
   }

   @Test
   public void testSetGetOriginalCollectionName() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME); // set is done in this method
      String realName = collectionMetadataFacade.getOriginalCollectionName(TEST_COLLECTION_INTERNAL_NAME);
      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
      Assert.assertEquals(TEST_COLLECTION_REAL_NAME, realName);
   }

   @Test
   public void testCollectionMetadataCollectionName() {
      Assert.assertEquals(collectionMetadataFacade.collectionMetadataCollectionName(TEST_COLLECTION_INTERNAL_NAME), TEST_COLLECTION_METADATA_COLLECTION_NAME);
   }

   @Test
   public void testCreateInitialMetadata() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      dataStorage.createCollection(collectionMetadataFacade.collectionMetadataCollectionName(TEST_COLLECTION_INTERNAL_NAME));
      collectionMetadataFacade.createInitialMetadata(TEST_COLLECTION_REAL_NAME);

      String name = collectionMetadataFacade.getOriginalCollectionName(TEST_COLLECTION_INTERNAL_NAME);
      String lock = collectionMetadataFacade.getCollectionLockTime(TEST_COLLECTION_INTERNAL_NAME);

      dataStorage.dropCollection(collectionMetadataFacade.collectionMetadataCollectionName(TEST_COLLECTION_INTERNAL_NAME));

      Assert.assertEquals(name, TEST_COLLECTION_REAL_NAME);
      Assert.assertNotEquals(lock, "");
   }

   @Test
   public void testSetGetCollectionLockTime() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);
      String time = Utils.getCurrentTimeString();
      collectionMetadataFacade.setCollectionLockTime(TEST_COLLECTION_INTERNAL_NAME, time);
      String timeTest = collectionMetadataFacade.getCollectionLockTime(TEST_COLLECTION_INTERNAL_NAME);
      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
      Assert.assertEquals(time, timeTest);
   }

   @Test
   public void testAddAttribute() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);
      long count = collectionMetadataFacade.getAttributeCount(TEST_COLLECTION_INTERNAL_NAME, name);

      Assert.assertEquals(count, 1);

      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);
      count = collectionMetadataFacade.getAttributeCount(TEST_COLLECTION_INTERNAL_NAME, name);

      Assert.assertEquals(count, 2);

      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
   }

   @Test
   public void testDropAttribute() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);

      collectionMetadataFacade.dropOrDecrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);
      long count = collectionMetadataFacade.getAttributeCount(TEST_COLLECTION_INTERNAL_NAME, name);

      Assert.assertEquals(count, 1);

      collectionMetadataFacade.dropOrDecrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);
      count = collectionMetadataFacade.getAttributeCount(TEST_COLLECTION_INTERNAL_NAME, name);
      Map<String, String> attributeInfo = collectionMetadataFacade.getCollectionAttributesNamesAndTypes(TEST_COLLECTION_INTERNAL_NAME);

      Assert.assertEquals(count, 0);
      Assert.assertEquals(attributeInfo.size(), 0);

      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
   }

   @Test
   public void testCheckAttributeValue() throws CollectionAlreadyExistsException, CollectionNotFoundException {
      collectionFacade.createCollection(TEST_COLLECTION_REAL_NAME);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(TEST_COLLECTION_INTERNAL_NAME, name);

      String type = "double";
      collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      boolean check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "3.14");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "hm");
      Assert.assertFalse(check);

      type = "int";
      collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "3");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "3.14");
      Assert.assertFalse(check);

      type = "long";
      collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "12345678900");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "hm");
      Assert.assertFalse(check);

      type = "nested";
      collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "hm");
      Assert.assertFalse(check);

      type = "date";
      collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "2016.11.22");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "2016.11.22 21.36");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "hm");
      Assert.assertFalse(check);

      type = "bool";
      collectionMetadataFacade.retypeCollectionAttribute(TEST_COLLECTION_INTERNAL_NAME, name, type);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "true");
      Assert.assertTrue(check);
      check = collectionMetadataFacade.checkAttributeValue(TEST_COLLECTION_INTERNAL_NAME, name, "hm");
      Assert.assertFalse(check);

      collectionFacade.dropCollection(TEST_COLLECTION_INTERNAL_NAME);
   }

   @Test
   public void testIsUserCollectionYes() {
      Assert.assertTrue(collectionMetadataFacade.isUserCollection(TEST_COLLECTION_INTERNAL_NAME));
   }

   @Test
   public void testIsUserCollectionNo() {
      Assert.assertFalse(collectionMetadataFacade.isUserCollection(TEST_COLLECTION_METADATA_COLLECTION_NAME));
   }

}
