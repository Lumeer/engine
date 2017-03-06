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
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.util.Utils;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
   private final String COLLECTION_GET_INTERNAL_NAME = "CollectionMetadataFacadeCollectionGetInternalName";
   private final String COLLECTION_CREATE_INITIAL_METADATA = "CollectionMetadataFacadeCollectionCreateInitialMetadata";
   private final String COLLECTION_SET_LOCK_TIME = "CollectionMetadataFacadeCollectionSetLockTime";
   private final String COLLECTION_ADD_OR_INCREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionAddOrIncrementAttribute";
   private final String COLLECTION_DROP_OR_DECREMENT_ATTRIBUTE = "CollectionMetadataFacadeCollectionDropOrDecrementAttribute";
   private final String COLLECTION_CHECK_ATTRIBUTE_VALUE = "CollectionMetadataFacadeCollectionCheckAttributeValue";
   private final String COLLECTION_SET_GET_DROP_CUSTOM_METADATA = "CollectionMetadataFacadeCollectionSetGetDropCustomMetadata";
   private final String COLLECTION_ADD_ATTRIBUTE_CONSTRAINT = "CollectionMetadataFacadeCollectionAddAttributeConstraint";

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
   public void testGetCollectionAttributesNames() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_NAMES);

      collectionFacade.createCollection(COLLECTION_ATTRIBUTES_NAMES);
      String collection = internalName(COLLECTION_ATTRIBUTES_NAMES);

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      List<String> attributes = collectionMetadataFacade.getCollectionAttributesNames(collection);

      assertThat(attributes).containsOnly(name1, name2);
   }

   @Test
   public void testGetCollectionAttributesInfo() throws Exception {
      setUpCollection(COLLECTION_ATTRIBUTES_INFO);

      collectionFacade.createCollection(COLLECTION_ATTRIBUTES_INFO);
      String collection = internalName(COLLECTION_ATTRIBUTES_INFO);

      List<DataDocument> attributesInfo = collectionMetadataFacade.getCollectionAttributesInfo(collection);
      assertThat(attributesInfo).isEmpty();

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      attributesInfo = collectionMetadataFacade.getCollectionAttributesInfo(collection);

      assertThat(attributesInfo).hasSize(2);
   }

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

      assertThat(columns).contains(newName);
      assertThat(rename).isTrue();

      // we try to rename attribute to name that already exists in collection
      assertThatThrownBy(() -> collectionMetadataFacade.renameCollectionAttribute(collection, oldName, newName))
            .isInstanceOf(AttributeAlreadyExistsException.class);

      // we try to rename non existing attribute
      assertThat(collectionMetadataFacade.renameCollectionAttribute(collection, oldName, "attribute 3")).isFalse();
   }

   @Test
   public void testRetypeCollectionAttribute() throws Exception {
      setUpCollection(COLLECTION_RETYPE_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_RETYPE_ATTRIBUTE);
      String collection = internalName(COLLECTION_RETYPE_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      String oldType = LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_INT;
      boolean retype = collectionMetadataFacade.retypeCollectionAttribute(collection, name, oldType);
      assertThat(collectionMetadataFacade.getAttributeType(collection, name)).isEqualTo(oldType);
      assertThat(retype).isTrue();

      // we try to retype to invalid type
      String newType = "hello";
      retype = collectionMetadataFacade.retypeCollectionAttribute(collection, name, newType);
      assertThat(collectionMetadataFacade.getAttributeType(collection, name)).isEqualTo(oldType);
      assertThat(retype).isFalse();

      // we try to retype non existing attribute
      assertThat(collectionMetadataFacade.retypeCollectionAttribute(collection, "attribute2", LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_STRING)).isFalse();
   }

   @Test
   public void testDropCollectionAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_DROP_ATTRIBUTE);
      String collection = internalName(COLLECTION_DROP_ATTRIBUTE);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.dropCollectionAttribute(collection, name);

      List<String> columns = collectionMetadataFacade.getCollectionAttributesNames(collection);

      assertThat(columns).isEmpty();

      // we try to drop non existing attribute - nothing happens
      collectionMetadataFacade.dropCollectionAttribute(collection, "attribute2");
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
   public void testCollectionMetadataCollectionName() {
      assertThat(collectionMetadataFacade.collectionMetadataCollectionName("collection")).isEqualTo("meta.collection");
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

      assertThat(name).isEqualTo(COLLECTION_CREATE_INITIAL_METADATA);
      assertThat(lock).isEmpty();
   }

   @Test
   public void testSetGetCollectionLockTime() throws Exception {
      setUpCollection(COLLECTION_SET_LOCK_TIME);

      collectionFacade.createCollection(COLLECTION_SET_LOCK_TIME);
      String collection = internalName(COLLECTION_SET_LOCK_TIME);
      String time = Utils.getCurrentTimeString();
      collectionMetadataFacade.setCollectionLockTime(collection, time);
      String timeTest = collectionMetadataFacade.getCollectionLockTime(collection);

      assertThat(time).isEqualTo(timeTest);
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
      List<String> attributeInfo = collectionMetadataFacade.getCollectionAttributesNames(collection);

      assertThat(count).isZero();
      assertThat(attributeInfo).isEmpty();
   }

   @Test
   public void testCheckAttributeValue() throws Exception {
      setUpCollection(COLLECTION_CHECK_ATTRIBUTE_VALUE);

      collectionFacade.createCollection(COLLECTION_CHECK_ATTRIBUTE_VALUE);
      String collection = internalName(COLLECTION_CHECK_ATTRIBUTE_VALUE);

      // check value of type int and also its constraints
      String attributeInt = "int";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeInt);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeInt, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_INT);

      String constraint1 = "greaterThan:3";
      String constraint2 = "lessThan:8";
      collectionMetadataFacade.addAttributeConstraint(collection, attributeInt, constraint1);
      collectionMetadataFacade.addAttributeConstraint(collection, attributeInt, constraint2);

      String intValueValid = "4";
      String intValueInvalidConstraint = "2";
      String intValueInvalidType = "123456789012345"; // this is too long value

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeInt, intValueValid)).isEqualTo(Integer.parseInt(intValueValid));
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeInt, intValueInvalidConstraint)).isNull();
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeInt, intValueInvalidType)).isNull();

      // check value of type long
      String attributeLong = "long";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeLong);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeLong, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_LONG);

      String longValueValid = "4";
      String longValueInvalid = Long.toString(Long.MAX_VALUE).concat("0"); // too long for Long

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeLong, longValueValid)).isEqualTo(Long.parseLong(longValueValid));
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeLong, longValueInvalid)).isNull();

      // check value of type double
      String attributeDouble = "double";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeDouble);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeDouble, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DOUBLE);

      String doubleValueValidInt = "4";
      String doubleValueValidDot = "3.14";
      String doubleValueValidComa = "3,14";
      String doubleValueInvalid = "3.14.3.14";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDouble, doubleValueValidInt)).isEqualTo(Double.parseDouble(doubleValueValidInt));
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDouble, doubleValueValidDot)).isEqualTo(Double.parseDouble(doubleValueValidDot));
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDouble, doubleValueValidComa)).isEqualTo(Double.parseDouble(doubleValueValidDot)); // coma is replaced by dot
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDouble, doubleValueInvalid)).isNull();

      // check value of type decimal
      // TODO

      // check value of type date - everything is accepted because there are no constraints
      String attributeDate = "date";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeDate);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeDate, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DATE);

      // at first, there is no constraint for date, so the string value is checked against default format
      String dateValueValid1 = "2016.12.18 14.34.00.000"; // default date and time format from Utils
      String dateValueInvalid1 = "2016.12.18 14.34.00";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDate, dateValueValid1)).isEqualTo(Utils.getDate(dateValueValid1));
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDate, dateValueInvalid1)).isNull();

      // we add constraint (chosen from DateTimeConstraintType)
      String format = "HH:mm:ss";
      String constraint3 = "time:" + format;
      collectionMetadataFacade.addAttributeConstraint(collection, attributeDate, constraint3);
      SimpleDateFormat sdf = new SimpleDateFormat(format);

      String dateValueValid2 = "14:34:00";
      String dateValueInvalid2 = "14:34";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDate, dateValueValid2)).isEqualTo(sdf.parse(dateValueValid2));
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeDate, dateValueInvalid2)).isNull();

      // check value of type boolean
      String attributeBoolean = "boolean";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeBoolean);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeBoolean, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_BOOLEAN);

      String booleanValueValid1 = "true";
      String booleanValueValid2 = "True";
      String booleanValueValid3 = "FALSE";
      String booleanValueValid4 = "fAlSe";
      String booleanValueInvalid = "yes";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeBoolean, booleanValueValid1)).isEqualTo(true);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeBoolean, booleanValueValid2)).isEqualTo(true);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeBoolean, booleanValueValid3)).isEqualTo(false);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeBoolean, booleanValueValid4)).isEqualTo(false);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeBoolean, booleanValueInvalid)).isNull();

      // check value of type string
      String attributeString = "string";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeDate); // type is set to string by default

      String stringValueValid1 = "everything we put in string";
      String stringValueValid2 = "should be always valid";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeString, stringValueValid1)).isEqualTo(stringValueValid1);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeString, stringValueValid2)).isEqualTo(stringValueValid2);

      // check value of type list
      String attributeList = "list";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeList);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeList, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_LIST);

      List<Object> listValueValid1 = new ArrayList<>();
      List<Object> listValueValid2 = Arrays.asList("hello", "world");
      List<Object> listValueValid3 = null;
      Object listValueInvalid1 = "hmmm";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeList, listValueValid1)).isEqualTo(listValueValid1);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeList, listValueValid2)).isEqualTo(listValueValid2);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeList, listValueValid3)).isEqualTo(listValueValid3);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeList, listValueInvalid1)).isNull();

      // check value of type nested
      String attributeNested = "nested";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeNested);
      collectionMetadataFacade.retypeCollectionAttribute(collection, attributeNested, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_NESTED);

      DataDocument nestedValueValid1 = new DataDocument();
      DataDocument nestedValueValid2 = new DataDocument().append("hello", "world");
      DataDocument nestedValueValid3 = new DataDocument().append("document", new DataDocument("hello", "world"));
      DataDocument nestedValueValid4 = null;
      Object nestedValueInvalid1 = "hmmm";

      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeNested, nestedValueValid1)).isEqualTo(nestedValueValid1);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeNested, nestedValueValid2)).isEqualTo(nestedValueValid2);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeNested, nestedValueValid3)).isEqualTo(nestedValueValid3);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeNested, nestedValueValid4)).isEqualTo(nestedValueValid4);
      assertThat(collectionMetadataFacade.checkAndConvertAttributeValue(collection, attributeNested, nestedValueInvalid1)).isNull();
   }

   @Test
   public void testGetSetDropCustomMetadata() throws Exception {
      setUpCollection(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);

      collectionFacade.createCollection(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);
      String collection = internalName(COLLECTION_SET_GET_DROP_CUSTOM_METADATA);

      // there is no custom metadata - we should obtain empty list
      assertThat(collectionMetadataFacade.getCustomMetadata(collection)).isEmpty();
      assertThat(collectionMetadataFacade.dropCustomMetadata(collection, Arrays.asList("key"))).isFalse();

      String metaKey1 = "meta key 1";
      String metaValue1 = "value 1";

      // we set one custom value
      assertThat(collectionMetadataFacade.setCustomMetadata(collection, new DataDocument(metaKey1, metaValue1))).isTrue();
      assertThat(collectionMetadataFacade.getCustomMetadata(collection).get(metaKey1).toString()).isEqualTo(metaValue1);

      // we try to drop non existing key, but dropAttribute in DataStorage does not return value, so we do not know it
      assertThat(collectionMetadataFacade.dropCustomMetadata(collection, Arrays.asList("random key"))).isTrue();

      // we drop existing key and after that it is not there
      assertThat(collectionMetadataFacade.dropCustomMetadata(collection, Arrays.asList(metaKey1))).isTrue();
      assertThat(collectionMetadataFacade.getCustomMetadata(collection)).doesNotContainKey(metaKey1);
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

   @Test
   public void testIsUserCollection() {
      assertThat(collectionMetadataFacade.isUserCollection("collection.something")).isTrue();
      assertThat(collectionMetadataFacade.isUserCollection("something")).isFalse();
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
   //      Assert.assertTrue(collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, name).isEmpty());
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
   //      Assert.assertEquals(collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, name).size(), 1);
   //   }

   private String internalName(String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

   private void setUpCollection(String originalCollectionName) {
      dataStorage.dropCollection(internalName(originalCollectionName));
      dataStorage.dropCollection("meta." + internalName(originalCollectionName));
   }
}
