package io.lumeer.engine.controller;/*
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

import io.lumeer.engine.api.data.DataStorage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 * @author <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   private final String METADATA_PREXIF = "metadata_";
   private final String DUMMY_COLLECTION1 = "testCollection1".toLowerCase(); // CollectionFacade converts names to lower case
   private final String DUMMY_COLLECTION2 = "testCollection2".toLowerCase();

   @Inject
   private CollectionFacade collectionFacade;

   @Test
   public void testGetAllCollections() throws Exception {
      Assert.assertEquals(collectionFacade.getAllCollections().size(), 0);
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1);
      collectionFacade.createCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(collectionFacade.getAllCollections().size(), 2);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
      collectionFacade.dropCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(collectionFacade.getAllCollections().size(), 0);
   }

   @Test
   public void testDropCollectionMetadata() throws Exception {
      //      boolean isDropped = false;
      //
      //      collectionFacade.createCollection(DUMMY_COLLECTION1);
      //      collectionFacade.dropCollectionMetadata(DUMMY_COLLECTION1);
      //       // we should not use dataStorage here
      //      if (!dataStorage.getAllCollections().contains(METADATA_PREXIF + DUMMY_COLLECTION1.toLowerCase())) {
      //         isDropped = true;
      //      }
      //
      //      Assert.assertEquals(isDropped, true);
   }

   @Test
   public void testGetAttributeValues() throws Exception {

   }

   @Test
   public void testAddColumn() throws Exception {

   }

   @Test
   public void testRenameColumn() throws Exception {

   }

   @Test
   public void testDropColumn() throws Exception {

   }

   @Test
   public void testOnCollectionEvent() throws Exception {

   }
}