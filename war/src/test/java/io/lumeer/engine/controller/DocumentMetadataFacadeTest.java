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
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class DocumentMetadataFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "DocumentMetadataFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-dev.properties");
   }

   private final String DUMMY_COLLECTION1 = "collection.testcollection1_0";
   private final String DUMMY_COLLECTION1_ORIGINAL_NAME = "testCollection1";

   private final String META_CREATE_USER_KEY = "meta-create-user";
   private final String META_CREATE_DATE_KEY = "meta-create-date";
   private final String META_CREATE_USER_VALUE = "testUser";

   private final String DUMMY_META_KEY = "meta-key";
   private final String DUMMY_META_VALUE = "param";
   private final String DUMMY_META_UPDATE_VALUE = "paramUpdated";

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testGetDocumentMetadata() throws Exception {
      String documentId = createCollectionAndNewDocument();
      Object metadataValue = documentMetadataFacade.getDocumentMetadata(DUMMY_COLLECTION1, documentId, META_CREATE_USER_KEY);

      Assert.assertEquals(metadataValue.toString(), META_CREATE_USER_VALUE);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testReadDocumentMetadata() throws Exception {
      String documentId = createCollectionAndNewDocument();
      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(DUMMY_COLLECTION1, documentId);

      Assert.assertTrue(documentMetadata.containsKey(META_CREATE_DATE_KEY) && documentMetadata.containsKey(META_CREATE_USER_KEY) && documentMetadata.size() == 2);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testPutAndUpdateAndDropDocumentMetadata() throws Exception {
      String documentId = createCollectionAndNewDocument();
      documentMetadataFacade.putDocumentMetadata(DUMMY_COLLECTION1, documentId, DUMMY_META_KEY, DUMMY_META_VALUE);

      Assert.assertTrue(documentMetadataFacade.getDocumentMetadata(DUMMY_COLLECTION1, documentId, DUMMY_META_KEY).toString().equals(DUMMY_META_VALUE));

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(DUMMY_META_KEY, DUMMY_META_UPDATE_VALUE);
      documentMetadataFacade.updateDocumentMetadata(DUMMY_COLLECTION1, documentId, metadata);

      Assert.assertTrue(documentMetadataFacade.getDocumentMetadata(DUMMY_COLLECTION1, documentId, DUMMY_META_KEY).toString().equals(DUMMY_META_UPDATE_VALUE));

      documentMetadataFacade.dropDocumentMetadata(DUMMY_COLLECTION1, documentId, DUMMY_META_KEY);

      Assert.assertFalse(documentMetadataFacade.readDocumentMetadata(DUMMY_COLLECTION1, documentId).containsKey(DUMMY_META_KEY));

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   private String createCollectionAndNewDocument() throws CollectionAlreadyExistsException, CollectionNotFoundException, UnsuccessfulOperationException, UserCollectionAlreadyExistsException, CollectionMetadataNotFoundException {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      DataDocument document = new DataDocument(new HashMap<>());
      return documentFacade.createDocument(DUMMY_COLLECTION1, document);
   }

}