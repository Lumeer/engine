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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class DocumentFacadeTest {

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
   private final String COLLECTION_REPLACE = "collectionReplace";
   private final String COLLECTION_REVERT = "collectionRevert";
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
   public void testRevertDocument() throws Exception {
      String coll = setUpCollection(COLLECTION_REVERT);
      String attr = "a";
      DataDocument document = new DataDocument(attr, "0");
      String documentId = documentFacade.createDocument(coll, document);

      DataDocument document1 = new DataDocument(attr, "1");
      document1.setId(documentId);
      documentFacade.updateDocument(coll, document1);

      DataDocument document2 = new DataDocument(attr, "2");
      document2.setId(documentId);
      documentFacade.updateDocument(coll, document2);

      DataDocument document3 = new DataDocument(attr, "3");
      document3.setId(documentId);
      documentFacade.updateDocument(coll, document3);

      DataDocument readed = documentFacade.readDocument(coll, documentId);
      Assert.assertEquals(readed.getString(attr), "3");

      documentFacade.revertDocument(coll, documentId, 0);
      readed = documentFacade.readDocument(coll, documentId);
      Assert.assertEquals(readed.getString(attr), "0");

      documentFacade.revertDocument(coll, documentId, 1);
      readed = documentFacade.readDocument(coll, documentId);
      Assert.assertEquals(readed.getString(attr), "1");

      documentFacade.revertDocument(coll, documentId, 2);
      readed = documentFacade.readDocument(coll, documentId);
      Assert.assertEquals(readed.getString(attr), "2");

      documentFacade.revertDocument(coll, documentId, 3);
      readed = documentFacade.readDocument(coll, documentId);
      Assert.assertEquals(readed.getString(attr), "3");
   }

   @Test
   public void testReplaceDocument() throws Exception {
      String coll = setUpCollection(COLLECTION_REPLACE);

      DataDocument document = new DataDocument("a", 1).append("b", 2).append("c", 3);
      String documentId = documentFacade.createDocument(coll, document);

      DataDocument replace = new DataDocument("d", 4).append("e", 5).append("f", 6);
      replace.setId(documentId);
      documentFacade.replaceDocument(coll, replace);

      DataDocument readed = documentFacade.readDocument(coll, documentId);

      Assert.assertTrue(!readed.containsKey("a"));
      Assert.assertTrue(!readed.containsKey("b"));
      Assert.assertTrue(!readed.containsKey("c"));

      Assert.assertTrue(readed.containsKey("d"));
      Assert.assertTrue(readed.containsKey("e"));
      Assert.assertTrue(readed.containsKey("f"));
      Assert.assertTrue(readed.containsKey(LumeerConst.Document.USER_RIGHTS));

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