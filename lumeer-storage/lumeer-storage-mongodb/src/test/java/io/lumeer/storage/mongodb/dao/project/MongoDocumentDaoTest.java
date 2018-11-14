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
package io.lumeer.storage.mongodb.dao.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class MongoDocumentDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String DOCUMENT_ID = "59a52042d412bc346576fad7";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String USER2 = "testUser2";

   private static final String COLLECTION_ID = "59a51b83d412bc2da88b010f";
   private static final ZonedDateTime CREATION_DATE = ZonedDateTime.now().withNano(0);
   private static final String CREATED_BY = USER;
   private static final int DATA_VERSION = 1;

   private static final String UPDATED_BY = USER2;
   private static final int DATA_VERSION2 = 2;

   private MongoDocumentDao documentDao;

   @Before
   public void initDocumentDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      documentDao = new MongoDocumentDao();
      documentDao.setDatabase(database);

      documentDao.setProject(project);
      documentDao.createDocumentsRepository(project);
   }

   private Document prepareDocument() {
      Document document = new Document(COLLECTION_ID, CREATION_DATE, null, CREATED_BY, null, DATA_VERSION, new DataDocument());
      document.setData(new DataDocument("something", "that should not be stored"));
      return document;
   }

   private Document createDocument() {
      Document document = prepareDocument();
      documentDao.databaseCollection().insertOne(document);
      return document;
   }

   @Test
   public void testCreateDocument() {
      Document document = prepareDocument();
      String id = documentDao.createDocument(document).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Document storedDocument = documentDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedDocument).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(COLLECTION_ID);
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(CREATED_BY);
      assertions.assertThat(storedDocument.getCreationDate()).isEqualTo(CREATION_DATE);
      assertions.assertThat(storedDocument.getUpdatedBy()).isNull();
      assertions.assertThat(storedDocument.getUpdateDate()).isNull();
      assertions.assertThat(storedDocument.getDataVersion()).isEqualTo(DATA_VERSION);
      assertions.assertThat(storedDocument.getData()).isNull();
      assertions.assertAll();
   }

   @Test
   public void testCreateDocumentExisting() {
      Document document = createDocument();
      assertThatThrownBy(() -> documentDao.createDocument(document))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testCreateDocumentMultiple() {
      Document document = prepareDocument();
      String id = documentDao.createDocument(document).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Document document2 = prepareDocument();
      String id2 = documentDao.createDocument(document2).getId();
      assertThat(id2).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id2)).isTrue();

      assertThat(id).isNotEqualTo(id2);
   }

   @Test
   public void testUpdateDocument() {
      Document document = createDocument();
      String id = document.getId();

      ZonedDateTime updateDate = ZonedDateTime.now().withNano(0);
      document.setDataVersion(DATA_VERSION2);
      document.setUpdatedBy(UPDATED_BY);
      document.setUpdateDate(updateDate);

      documentDao.updateDocument(document.getId(), document);

      Document storedDocument = documentDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedDocument).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(COLLECTION_ID);
      assertions.assertThat(storedDocument.getCreatedBy()).isEqualTo(CREATED_BY);
      assertions.assertThat(storedDocument.getCreationDate()).isEqualTo(CREATION_DATE);
      assertions.assertThat(storedDocument.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertions.assertThat(storedDocument.getUpdateDate()).isEqualTo(updateDate);
      assertions.assertThat(storedDocument.getDataVersion()).isEqualTo(DATA_VERSION2);
      assertions.assertAll();
   }

   @Test
   @Ignore("Stored anyway with the current implementation")
   public void testUpdateDocumentNotExisting() {
      Document document = prepareDocument();
      assertThatThrownBy(() -> documentDao.updateDocument(DOCUMENT_ID, document))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteDocument() {
      String id = createDocument().getId();

      documentDao.deleteDocument(id);

      Document storedDocument = documentDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedDocument).isNull();
   }

   @Test
   public void testDeleteDocumentNotExisting() {
      assertThatThrownBy(() -> documentDao.deleteDocument(DOCUMENT_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteDocuments() {
      createDocument();
      createDocument();

      List<Document> documents = documentDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(documents).isNotEmpty();

      documentDao.deleteDocuments(COLLECTION_ID);

      documents = documentDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(documents).isEmpty();
   }

   @Test
   public void testDeleteDocumentsEmpty() {
      documentDao.deleteDocuments(COLLECTION_ID);

      List<Document> documents = documentDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(documents).isEmpty();
   }

   @Test
   public void testGetDocumentById() {
      String id = createDocument().getId();

      Document document = documentDao.getDocumentById(id);
      assertThat(document).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(document.getId()).isEqualTo(id);
      assertions.assertThat(document.getCollectionId()).isEqualTo(COLLECTION_ID);
      assertions.assertThat(document.getCreatedBy()).isEqualTo(CREATED_BY);
      assertions.assertThat(document.getCreationDate()).isEqualTo(CREATION_DATE);
      assertions.assertThat(document.getUpdatedBy()).isNull();
      assertions.assertThat(document.getUpdateDate()).isNull();
      assertions.assertThat(document.getDataVersion()).isEqualTo(DATA_VERSION);
      assertions.assertAll();
   }

   @Test
   public void testGetDocumentByIdNotExisting() {
      assertThatThrownBy(() -> documentDao.getDocumentById(DOCUMENT_ID))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasFieldOrPropertyWithValue("resourceType", ResourceType.DOCUMENT);
   }

   @Test
   public void testGetDocumentsByIds() {
      createDocument();
      String id2 = createDocument().getId();
      String id3 = createDocument().getId();

      List<Document> documents = documentDao.getDocumentsByIds(id2, id3);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3);
   }

   @Test
   public void testGetDocumentsByIdsEmpty() {
      List<Document> documents = documentDao.getDocumentsByIds();
      assertThat(documents).isEmpty();
   }

   @Test
   public void testGetDocumentsByIdsNotExisting() {
      List<Document> documents = documentDao.getDocumentsByIds(DOCUMENT_ID);
      assertThat(documents).isEmpty();
   }
}
