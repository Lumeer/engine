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
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaDocument;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

public class MorphiaDocumentDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String DOCUMENT_ID = "59a52042d412bc346576fad7";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String USER2 = "testUser2";

   private static final String COLLECTION_ID = "59a51b83d412bc2da88b010f";
   private static final LocalDateTime CREATION_DATE = LocalDateTime.now();
   private static final String CREATED_BY = USER;
   private static final int DATA_VERSION = 1;

   private static final String UPDATED_BY = USER2;
   private static final int DATA_VERSION2 = 2;

   private MorphiaDocumentDao documentDao;

   @Before
   public void initDocumentDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      documentDao = new MorphiaDocumentDao();
      documentDao.setDatabase(database);
      documentDao.setDatastore(datastore);

      documentDao.setProject(project);
      documentDao.createDocumentsRepository(project);
   }

   private MorphiaDocument prepareDocument() {
      MorphiaDocument document = new MorphiaDocument();
      document.setCollectionId(COLLECTION_ID);
      document.setCreationDate(CREATION_DATE);
      document.setCreatedBy(CREATED_BY);
      document.setDataVersion(DATA_VERSION);
      document.setData(new DataDocument("something", "that should not be stored"));
      return document;
   }

   private MorphiaDocument createDocument() {
      MorphiaDocument document = prepareDocument();
      datastore.save(documentDao.databaseCollection(), document);
      return document;
   }

   @Test
   public void testCreateDocument() {
      MorphiaDocument document = prepareDocument();
      String id = documentDao.createDocument(document).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Document storedDocument = datastore.get(documentDao.databaseCollection(), MorphiaDocument.class, new ObjectId(id));
      assertThat(storedDocument).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(COLLECTION_ID);
      assertions.assertThat(storedDocument.getCollectionCode()).isNull();
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
      MorphiaDocument document = createDocument();
      assertThatThrownBy(() -> documentDao.createDocument(document))
            .isInstanceOf(DuplicateKeyException.class); // TODO change this to our own exception
   }

   @Test
   public void testCreateDocumentMultiple() {
      Document document = prepareDocument();
      String id = documentDao.createDocument(document).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Document document2 = new MorphiaDocument();
      String id2 = documentDao.createDocument(document2).getId();
      assertThat(id2).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id2)).isTrue();

      assertThat(id).isNotEqualTo(id2);
   }

   @Test
   public void testUpdateDocument() {
      MorphiaDocument document = createDocument();
      String id = document.getId();

      LocalDateTime updateDate = LocalDateTime.now();
      document.setDataVersion(DATA_VERSION2);
      document.setUpdatedBy(UPDATED_BY);
      document.setUpdateDate(updateDate);

      documentDao.updateDocument(document.getId(), document);

      Document storedDocument = datastore.get(documentDao.databaseCollection(), MorphiaDocument.class, new ObjectId(id));
      assertThat(storedDocument).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedDocument.getId()).isEqualTo(id);
      assertions.assertThat(storedDocument.getCollectionId()).isEqualTo(COLLECTION_ID);
      assertions.assertThat(storedDocument.getCollectionCode()).isNull();
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
      MorphiaDocument document = prepareDocument();
      assertThatThrownBy(() -> documentDao.updateDocument(DOCUMENT_ID, document))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testDeleteDocument() {
      String id = createDocument().getId();

      documentDao.deleteDocument(id);

      Document storedDocument = datastore.get(documentDao.databaseCollection(), MorphiaDocument.class, new ObjectId(id));
      assertThat(storedDocument).isNull();
   }

   @Test
   public void testDeleteDocumentNotExisting() {
      assertThatThrownBy(() -> documentDao.deleteDocument(DOCUMENT_ID))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testDeleteDocuments() {
      createDocument();
      createDocument();

      List<MorphiaDocument> documents = datastore.find(documentDao.databaseCollection(), MorphiaDocument.class).asList();
      assertThat(documents).isNotEmpty();

      documentDao.deleteDocuments(COLLECTION_ID);

      documents = datastore.find(documentDao.databaseCollection(), MorphiaDocument.class).asList();
      assertThat(documents).isEmpty();
   }

   @Test
   public void testDeleteDocumentsEmpty() {
      documentDao.deleteDocuments(COLLECTION_ID);

      List<MorphiaDocument> documents = datastore.find(documentDao.databaseCollection(), MorphiaDocument.class).asList();
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
      assertions.assertThat(document.getCollectionCode()).isNull();
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
      String id = createDocument().getId();
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
