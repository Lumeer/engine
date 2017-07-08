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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.dto.Collection;

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

   private final String DUMMY_META_VALUE = "param";
   private final String DUMMY_META_UPDATE_VALUE = "paramUpdated";

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private CollectionFacade collectionFacade;

   @Test
   public void testGetDocumentMetadata() throws Exception {
      String oldCode = collectionMetadataFacade.getCollectionCodeFromName(COLLECTION_GET_METADATA);
      if (oldCode != null) {
         dataStorage.dropCollection(oldCode);
      }
      String code = collectionFacade.createCollection(new Collection(COLLECTION_GET_METADATA));

      DataDocument document = new DataDocument("a", 1).append("b", 2).append("c", 3);
      String documentId = documentFacade.createDocument(code, document);

      Object metadataValue = documentMetadataFacade.getDocumentMetadata(code, documentId, LumeerConst.Document.CREATE_BY_USER_KEY);
      assertThat(metadataValue).isNotNull();
   }

   @Test
   public void testReadDocumentMetadata() throws Exception {
      String oldCode = collectionMetadataFacade.getCollectionCodeFromName(COLLECTION_READ_METADATA);
      if (oldCode != null) {
         dataStorage.dropCollection(oldCode);
      }
      String code = collectionFacade.createCollection(new Collection(COLLECTION_READ_METADATA));

      DataDocument document = new DataDocument("a", 1).append("b", 2).append("c", 3);
      String documentId = documentFacade.createDocument(code, document);

      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(code, documentId);

      assertThat(documentMetadata).containsKey(LumeerConst.Document.CREATE_BY_USER_KEY);
      assertThat(documentMetadata).containsKey(LumeerConst.Document.CREATE_DATE_KEY);
      assertThat(documentMetadata).hasSize(4);
   }

   @Test
   public void testPutAndUpdateAndDropDocumentMetadata() throws Exception {
      String oldCode = collectionMetadataFacade.getCollectionCodeFromName(COLLECTION_PUT_AND_UPDATE_METADATA);
      if (oldCode != null) {
         dataStorage.dropCollection(oldCode);
      }

      String code = collectionFacade.createCollection(new Collection(COLLECTION_PUT_AND_UPDATE_METADATA));

      DataDocument document = new DataDocument("a", 1).append("b", 2).append("c", 3);
      String documentId = documentFacade.createDocument(code, document);

      documentMetadataFacade.putDocumentMetadata(code, documentId, LumeerConst.Document.CREATE_BY_USER_KEY, DUMMY_META_VALUE);
      assertThat(documentMetadataFacade.getDocumentMetadata(code, documentId, LumeerConst.Document.CREATE_BY_USER_KEY).toString()).isEqualTo(DUMMY_META_VALUE);

      DataDocument metadata = new DataDocument(LumeerConst.Document.CREATE_BY_USER_KEY, DUMMY_META_UPDATE_VALUE);
      documentMetadataFacade.updateDocumentMetadata(code, documentId, metadata);
      assertThat(documentMetadataFacade.getDocumentMetadata(code, documentId, LumeerConst.Document.CREATE_BY_USER_KEY).toString()).isEqualTo(DUMMY_META_UPDATE_VALUE);

      documentMetadataFacade.dropDocumentMetadata(code, documentId, LumeerConst.Document.CREATE_BY_USER_KEY);
      assertThat(documentMetadataFacade.readDocumentMetadata(code, documentId)).doesNotContainKey(LumeerConst.Document.CREATE_BY_USER_KEY);
   }

}