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
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.exception.DbException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class DocumentFacadeIntegrationTest extends IntegrationTestBase {

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
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Test
   public void testCreateAndDropDocument() throws Exception {
      String coll = setUpCollection(COLLECTION_CREATE_AND_DROP);

      DataDocument document = new DataDocument();
      String documentId = documentFacade.createDocument(coll, document);
      DataDocument inserted = documentFacade.readDocument(coll, documentId);
      assertThat(inserted).isNotNull();

      documentFacade.dropDocument(coll, documentId);
      assertThat(dataStorage.readDocument(coll, dataStorageDialect.documentIdFilter(documentId))).isNull();
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
      assertThat(readed.getString(attr)).isEqualTo("3");

      documentFacade.revertDocument(coll, documentId, 0);
      readed = documentFacade.readDocument(coll, documentId);
      assertThat(readed.getString(attr)).isEqualTo("0");

      documentFacade.revertDocument(coll, documentId, 1);
      readed = documentFacade.readDocument(coll, documentId);
      assertThat(readed.getString(attr)).isEqualTo("1");

      documentFacade.revertDocument(coll, documentId, 2);
      readed = documentFacade.readDocument(coll, documentId);
      assertThat(readed.getString(attr)).isEqualTo("2");

      documentFacade.revertDocument(coll, documentId, 3);
      readed = documentFacade.readDocument(coll, documentId);
      assertThat(readed.getString(attr)).isEqualTo("3");
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

      assertThat(readed).doesNotContainKey("a");
      assertThat(readed).doesNotContainKey("b");
      assertThat(readed).doesNotContainKey("c");

      assertThat(readed).containsKey("d");
      assertThat(readed).containsKey("e");
      assertThat(readed).containsKey("f");
   }

   @Test
   public void testReadAndUpdateDocument() throws Exception {
      String coll = setUpCollection(COLLECTION_READ_AND_UPDATE);

      DataDocument document = new DataDocument(DUMMY_KEY1, DUMMY_VALUE1);
      String documentId = documentFacade.createDocument(coll, document);
      DataDocument inserted = documentFacade.readDocument(coll, documentId);
      assertThat(inserted).isNotNull();
      assertThat(inserted.getId()).isEqualTo(documentId);

      inserted.entrySet().removeIf(entry -> entry.getKey().startsWith(LumeerConst.Document.METADATA_PREFIX));

      String changed = DUMMY_VALUE1 + "_changed";
      inserted.put(DUMMY_KEY1, changed);
      documentFacade.updateDocument(coll, inserted);
      DataDocument updated = dataStorage.readDocument(coll, dataStorageDialect.documentIdFilter(documentId));
      assertThat(updated).isNotNull();
      assertThat(updated.getString(DUMMY_KEY1)).isEqualTo(changed);
   }

   @Test
   public void testGetAttributes() throws Exception {
      String coll = setUpCollection(COLLECTION_GETATTRS_AND_DROPATTR);

      DataDocument document = new DataDocument("a", 1)
            .append("b", 2)
            .append("c", new DataDocument("cc", 1)
                  .append("dd", 2))
            .append("d", new DataDocument("dd", new DataDocument("ddd", new DataDocument("dddd", new DataDocument("ddddd", 1)
                  .append("ddddd2", 2)))));

      String docId = dataStorage.createDocument(coll, document);

      Set<String> attrs = documentFacade.getDocumentAttributes(coll, docId);
      assertThat(attrs).containsOnly("_id", "a", "b", "c", "d", "c.cc", "c.dd", "d.dd", "d.dd.ddd", "d.dd.ddd.dddd", "d.dd.ddd.dddd.ddddd", "d.dd.ddd.dddd.ddddd2");

      documentFacade.dropAttribute(coll, docId, "b");
      documentFacade.dropAttribute(coll, docId, "c.dd");
      documentFacade.dropAttribute(coll, docId, "d.dd.ddd.dddd.ddddd2");

      DataDocument update = new DataDocument(ID_KEY, docId);
      update.put("f", 2);
      documentFacade.updateDocument(coll, update);

      attrs = documentFacade.getDocumentAttributes(coll, docId);

      assertThat(attrs).containsOnly("_id", "a", "f", "c", "d", "c.cc", "d.dd", "d.dd.ddd", "d.dd.ddd.dddd", "d.dd.ddd.dddd.ddddd");
   }

   private String setUpCollection(final String collectionName) throws DbException {
      String collectionCode = collectionMetadataFacade.getCollectionCodeFromName(collectionName);
      if(collectionCode != null) {
         dataStorage.dropCollection(collectionCode);
      }
      return collectionFacade.createCollection(new Collection(collectionName));
   }

}