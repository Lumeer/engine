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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class DocumentFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "DocumentFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml");
   }

   private final String DUMMY_COLLECTION1 = "collection.testcollection1_0";
   private final String DUMMY_COLLECTION1_ORIGINAL_NAME = "testCollection1";

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_VALUE1 = "param1";
   private final String ID_KEY = "_id";

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testCreateAndDropDocument() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      DataDocument document = new DataDocument(new HashMap<>());
      String documentId = documentFacade.createDocument(DUMMY_COLLECTION1, document);

      Assert.assertEquals(dataStorage.readDocument(DUMMY_COLLECTION1, documentId).get(ID_KEY), documentId);

      documentFacade.dropDocument(DUMMY_COLLECTION1, documentId);

      Assert.assertNull(dataStorage.readDocument(DUMMY_COLLECTION1, documentId));

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testReadAndUpdateDocument() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      DataDocument document = new DataDocument(new HashMap<>());
      String documentId = documentFacade.createDocument(DUMMY_COLLECTION1, document);

      Assert.assertEquals(documentFacade.readDocument(DUMMY_COLLECTION1, documentId).get(ID_KEY).toString(), documentId);

      DataDocument updatedDocument = documentFacade.readDocument(DUMMY_COLLECTION1, documentId);
      String updatedDocumentId = updatedDocument.getString(ID_KEY);
      updatedDocument.put(DUMMY_KEY1, DUMMY_VALUE1);

      documentFacade.updateDocument(DUMMY_COLLECTION1, updatedDocument);
      String value = dataStorage.readDocument(DUMMY_COLLECTION1, updatedDocumentId).getString(DUMMY_KEY1);

      Assert.assertEquals(value, DUMMY_VALUE1);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

}