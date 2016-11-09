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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class MetadataFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "MetadataFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   @Inject
   private MetadataFacade metadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   private String testCollectionInternalName = "collection.collection1";
   private String testCollectionRealName = "Collection_1";

   @Test
   public void testCollectionInternalNameToInternalForm() throws Exception {
      String originalName = "čťH-%/e&äll o1";
      String newName = "collection.hello1";
      Assert.assertEquals(metadataFacade.collectionNameToInternalForm(originalName), newName);
   }

   @Test
   public void testViewNameToInternalForm() throws Exception {
      String originalName = "čťH-%/e&äll o1";
      String newName = "view.hello1";
      Assert.assertEquals(metadataFacade.viewNameToInternalForm(originalName), newName);
   }

   @Test
   public void testGetCollectionColumnsInfo() {
      collectionFacade.createCollection(testCollectionRealName);
      String name = "column 1";
      String type = "int";
      metadataFacade.addCollectionColumn(testCollectionInternalName, name, type);

      Map<String, String> columnsInfo = metadataFacade.getCollectionColumnsInfo(testCollectionInternalName);
      collectionFacade.dropCollection(testCollectionInternalName);

      Assert.assertEquals(columnsInfo.size(), 1);
      Assert.assertTrue(columnsInfo.containsKey(name));
      Assert.assertTrue(columnsInfo.containsValue(type));
   }

   @Test
   public void testAddCollectionColumnNew() {
      collectionFacade.createCollection(testCollectionRealName);
      boolean add = metadataFacade.addCollectionColumn(testCollectionInternalName, "column 1", "int");
      collectionFacade.dropCollection(testCollectionInternalName);

      Assert.assertTrue(add);
   }

   @Test
   public void testAddCollectionColumnExisting() {
      collectionFacade.createCollection(testCollectionRealName);
      metadataFacade.addCollectionColumn(testCollectionInternalName, "column 1", "int");
      boolean add = metadataFacade.addCollectionColumn(testCollectionInternalName, "column 1", "int");
      collectionFacade.dropCollection(testCollectionInternalName);

      Assert.assertFalse(add);
   }

   @Test
   public void testRenameCollectionColumn() {
      collectionFacade.createCollection(testCollectionRealName);
      metadataFacade.addCollectionColumn(testCollectionInternalName, "column 1", "int");
      boolean rename = metadataFacade.renameCollectionColumn(testCollectionInternalName, "column 1", "column 2");
      Map<String, String> columnsInfo = metadataFacade.getCollectionColumnsInfo(testCollectionInternalName);
      collectionFacade.dropCollection(testCollectionInternalName);

      Assert.assertTrue(columnsInfo.containsKey("column 2"));
      Assert.assertTrue(rename);
   }

   @Test
   public void testRetypeCollectionColumn() {
      collectionFacade.createCollection(testCollectionRealName);
      metadataFacade.addCollectionColumn(testCollectionInternalName, "column 1", "int");
      boolean retype = metadataFacade.retypeCollectionColumn(testCollectionInternalName, "column 1", "double");
      Map<String, String> columnsInfo = metadataFacade.getCollectionColumnsInfo(testCollectionInternalName);
      collectionFacade.dropCollection(testCollectionInternalName);

      Assert.assertTrue(columnsInfo.containsValue("double"));
      Assert.assertTrue(retype);
   }

   @Test
   public void testDropCollectionColumn() {
      collectionFacade.createCollection(testCollectionRealName);
      metadataFacade.addCollectionColumn(testCollectionInternalName, "column 1", "int");
      boolean drop = metadataFacade.dropCollectionColumn(testCollectionInternalName, "column 1");
      Map<String, String> columnsInfo = metadataFacade.getCollectionColumnsInfo(testCollectionInternalName);
      collectionFacade.dropCollection(testCollectionInternalName);

      Assert.assertTrue(columnsInfo.isEmpty());
      Assert.assertTrue(drop);
   }

   @Test
   public void testGetOriginalCollectionName() {
      collectionFacade.createCollection(testCollectionRealName);
      String internalName = metadataFacade.collectionNameToInternalForm(testCollectionRealName);
      String realName = metadataFacade.getOriginalCollectionName(internalName);
      collectionFacade.dropCollection(testCollectionInternalName);
      Assert.assertEquals(testCollectionRealName, realName);
   }
}
