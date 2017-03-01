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

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class DocumentMetadataFacadeIntegrationTest extends IntegrationTestBase {

   private final String COLLECTION_GET_METADATA = "collectionGetMetadata";
   private final String COLLECTION_READ_METADATA = "collectionReadMetadata";
   private final String COLLECTION_PUT_AND_UPDATE_METADATA = "collectionPutAndUpdateMetadata";

   private final String DUMMY_META_KEY = LumeerConst.Document.METADATA_PREFIX + "key";
   private final String DUMMY_META_VALUE = "param";
   private final String DUMMY_META_UPDATE_VALUE = "paramUpdated";

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testGetDocumentMetadata() throws Exception {
      String documentId = setupCollectionAndCreateNewDocument(COLLECTION_GET_METADATA);
      Object metadataValue = documentMetadataFacade.getDocumentMetadata(COLLECTION_GET_METADATA, documentId, LumeerConst.Document.CREATE_BY_USER_KEY);
      assertThat(metadataValue).isNotNull();
   }

   @Test
   public void testReadDocumentMetadata() throws Exception {
      String documentId = setupCollectionAndCreateNewDocument(COLLECTION_READ_METADATA);
      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(COLLECTION_READ_METADATA, documentId);

      assertThat(documentMetadata).containsKey(LumeerConst.Document.CREATE_BY_USER_KEY);
      assertThat(documentMetadata).containsKey(LumeerConst.Document.CREATE_DATE_KEY);
      assertThat(documentMetadata).hasSize(5);
   }

   @Test
   public void testPutAndUpdateAndDropDocumentMetadata() throws Exception {
      String documentId = setupCollectionAndCreateNewDocument(COLLECTION_PUT_AND_UPDATE_METADATA);
      documentMetadataFacade.putDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, LumeerConst.Document.CREATE_BY_USER_KEY, DUMMY_META_VALUE);
      assertThat(documentMetadataFacade.getDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, LumeerConst.Document.CREATE_BY_USER_KEY).toString()).isEqualTo(DUMMY_META_VALUE);

      DataDocument metadata = new DataDocument(LumeerConst.Document.CREATE_BY_USER_KEY, DUMMY_META_UPDATE_VALUE);
      documentMetadataFacade.updateDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, metadata);
      assertThat(documentMetadataFacade.getDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, LumeerConst.Document.CREATE_BY_USER_KEY).toString()).isEqualTo(DUMMY_META_UPDATE_VALUE);

      documentMetadataFacade.dropDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId, LumeerConst.Document.CREATE_BY_USER_KEY);
      assertThat(documentMetadataFacade.readDocumentMetadata(COLLECTION_PUT_AND_UPDATE_METADATA, documentId)).doesNotContainKey(LumeerConst.Document.CREATE_BY_USER_KEY);
   }

   private String setupCollectionAndCreateNewDocument(final String collection) throws DbException, InvalidConstraintException {
      dataStorage.dropCollection(collection);
      dataStorage.dropCollection(collectionMetadataFacade.collectionMetadataCollectionName(collection));
      dataStorage.createCollection(collection);
      dataStorage.createCollection(collectionMetadataFacade.collectionMetadataCollectionName(collection));

      DataDocument document = new DataDocument("a", 1).append("b", 2).append("c", 3);
      return documentFacade.createDocument(collection, document);
   }

}