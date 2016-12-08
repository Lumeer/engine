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
import io.lumeer.engine.api.exception.CollectionMetadataNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
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

   private final String COLLECTION_GET_METADATA = "collectionGetMetadata";
   private final String COLLECTION_READ_METADATA = "collectionReadMetadata";
   private final String COLLECTION_PUT_AND_UPDATE_METADATA = "collectionPutAndUpdateMetadata";

   private final String DUMMY_USER = "testUser";
   private final String DUMMY_META_KEY = "meta-key";
   private final String DUMMY_META_VALUE = "param";
   private final String DUMMY_META_UPDATE_VALUE = "paramUpdated";

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private DataStorage dataStorage;

   @BeforeTest
   public void setUp() {
      if (dataStorage != null) {
         dataStorage.dropCollection(COLLECTION_GET_METADATA);
         dataStorage.dropCollection(COLLECTION_READ_METADATA);
         dataStorage.dropCollection(COLLECTION_PUT_AND_UPDATE_METADATA);
      }
   }

   @Test
   public void testGetDocumentMetadata() throws Exception {
      String documentId = createCollectionAndNewDocument(COLLECTION_GET_METADATA);
      Object metadataValue = documentMetadataFacade.getDocumentMetadata(COLLECTION_GET_METADATA, documentId, LumeerConst.Document.CREATE_BY_USER_KEY);

      Assert.assertEquals(metadataValue.toString(), DUMMY_USER);

      dataStorage.dropCollection(COLLECTION_GET_METADATA);
   }

   @Test
   public void testReadDocumentMetadata() throws Exception {
      String documentId = createCollectionAndNewDocument(COLLECTION_READ_METADATA);
      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(COLLECTION_READ_METADATA, documentId);

      Assert.assertTrue(documentMetadata.containsKey(LumeerConst.Document.CREATE_BY_USER_KEY) && documentMetadata.containsKey(LumeerConst.Document.CREATE_BY_USER_KEY) && documentMetadata.size() == 2);

      dataStorage.dropCollection(COLLECTION_GET_METADATA);
   }

   @Test
   public void testPutAndUpdateAndDropDocumentMetadata() throws Exception {
      String documentId = createCollectionAndNewDocument(COLLECTION_PUT_AND_UPDATE_METADATA);
      documentMetadataFacade.putDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, DUMMY_META_KEY, DUMMY_META_VALUE);

      Assert.assertTrue(documentMetadataFacade.getDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, DUMMY_META_KEY).toString().equals(DUMMY_META_VALUE));

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(DUMMY_META_KEY, DUMMY_META_UPDATE_VALUE);
      documentMetadataFacade.updateDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, metadata);

      Assert.assertTrue(documentMetadataFacade.getDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, DUMMY_META_KEY).toString().equals(DUMMY_META_UPDATE_VALUE));

      documentMetadataFacade.dropDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, DUMMY_META_KEY);

      Assert.assertFalse(documentMetadataFacade.readDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId).containsKey(DUMMY_META_KEY));

      dataStorage.dropCollection(COLLECTION_GET_METADATA);
   }

   private String createCollectionAndNewDocument(final String collection) throws CollectionAlreadyExistsException, CollectionNotFoundException, UnsuccessfulOperationException, UserCollectionAlreadyExistsException, CollectionMetadataNotFoundException, InvalidDocumentKeyException {
      dataStorage.createCollection(collection);

      DataDocument document = new DataDocument(new HashMap<>());
      return documentFacade.createDocument(collection, document);
   }

}