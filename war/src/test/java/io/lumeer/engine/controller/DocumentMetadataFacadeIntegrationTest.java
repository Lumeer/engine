/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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