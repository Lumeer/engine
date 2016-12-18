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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String COLLECTION_CREATE_AND_DROP = "collectionCreateAndDrop";
   private final String COLLECTION_READ_AND_UPDATE = "collectionReadAndUpdate";
   private final String COLLECTION_GETATTRS_AND_DROPATTR = "collectionGetAttrsAndDropAttr";

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_VALUE1 = "param1";
   private final String ID_KEY = "_id";

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testCreateAndDropDocument() throws Exception {
      String coll = setUpCollection(COLLECTION_CREATE_AND_DROP);

      DataDocument document = new DataDocument();
      String documentId = documentFacade.createDocument(coll, document);
      DataDocument inserted = documentFacade.readDocument(coll, documentId);
      Assert.assertNotNull(inserted);

      documentFacade.dropDocument(coll, documentId);
      Assert.assertNull(dataStorage.readDocument(coll, documentId));
   }

   @Test
   public void testReadAndUpdateDocument() throws Exception {
      String coll = setUpCollection(COLLECTION_READ_AND_UPDATE);

      DataDocument document = new DataDocument(DUMMY_KEY1, DUMMY_VALUE1);
      String documentId = documentFacade.createDocument(coll, document);
      DataDocument inserted = documentFacade.readDocument(coll, documentId);
      Assert.assertNotNull(inserted);
      Assert.assertEquals(inserted.getId(), documentId);

      for (Iterator<Map.Entry<String, Object>> it = inserted.entrySet().iterator(); it.hasNext(); ) {
         Map.Entry<String, Object> entry = it.next();
         if (entry.getKey().startsWith(LumeerConst.Document.METADATA_PREFIX)) {
            it.remove();
         }
      }

      String changed = DUMMY_VALUE1 + "_changed";
      inserted.put(DUMMY_KEY1, changed);
      documentFacade.updateDocument(coll, inserted);
      DataDocument updated = dataStorage.readDocument(coll, documentId);
      Assert.assertNotNull(updated);
      Assert.assertEquals(updated.getString(DUMMY_KEY1), changed);
   }

   @Test
   public void testGetAttributes() throws Exception {
      String coll = setUpCollection(COLLECTION_GETATTRS_AND_DROPATTR);

      DataDocument document = new DataDocument();
      document.put("a", 1);
      document.put("b", 2);
      document.put("c", 3);
      document.put("d", 4);

      String docId = dataStorage.createDocument(coll, document);

      Set<String> attrs = documentFacade.getDocumentAttributes(coll, docId);
      Assert.assertTrue(attrs.contains("a"));
      Assert.assertTrue(attrs.contains("c"));
      Assert.assertFalse(attrs.contains("x"));
      Assert.assertFalse(attrs.contains("g"));

      documentFacade.dropAttribute(coll, docId, "a");
      documentFacade.dropAttribute(coll, docId, "d");

      DataDocument update = new DataDocument(ID_KEY, docId);
      update.put("f", 2);
      update.put("x", 10);
      documentFacade.updateDocument(coll, update);

      attrs = documentFacade.getDocumentAttributes(coll, docId);

      Assert.assertFalse(attrs.contains("a"));
      Assert.assertFalse(attrs.contains("d"));
      Assert.assertTrue(attrs.contains("f"));
      Assert.assertTrue(attrs.contains("x"));

   }

   private String setUpCollection(final String collection) {
      try {
         collectionFacade.dropCollection(collectionMetadataFacade.getInternalCollectionName(collection));
      } catch (DbException e) {
         // nothing to do
      }
      try {
         return collectionFacade.createCollection(collection);
      } catch (DbException e) {
         e.printStackTrace();
      }
      return null;
   }

}