/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.model.common.MorphiaEntity.ID;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaCollection;
import io.lumeer.storage.mongodb.model.MorphiaDocument;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MorphiaDocumentDao extends ProjectScopedDao implements DocumentDao {

   private static final String PREFIX = "documents_p-";

   @Override
   public void createDocumentsRepository(final Project project) {
      database.createCollection(databaseCollection(project));
      datastore.ensureIndexes(databaseCollection(project), MorphiaDocument.class);
   }

   @Override
   public void deleteDocumentsRepository(final Project project) {
      database.getCollection(databaseCollection(project)).drop();
   }

   @Override
   public Document createDocument(final Document document) {
      MorphiaDocument morphiaDocument = new MorphiaDocument(document);
      datastore.insert(databaseCollection(), morphiaDocument);
      return morphiaDocument;
   }

   @Override
   public List<Document> createDocuments(final List<Document> documents) {
      List<org.bson.Document> bsonDocuments = documents.stream().map(doc -> new MorphiaDocument(doc).toBsonDocument()).collect(Collectors.toList());
      database.getCollection(databaseCollection()).insertMany(bsonDocuments);

      for (int i = 0; i < documents.size(); i++) {
         documents.get(i).setId(bsonDocuments.get(i).getObjectId(ID).toHexString());
      }
      return documents;
   }

   @Override
   public Document updateDocument(final String id, final Document document) {
      MorphiaDocument morphiaDocument = new MorphiaDocument(document);
      morphiaDocument.setId(id);
      datastore.save(databaseCollection(), morphiaDocument);
      return morphiaDocument;
   }

   @Override
   public void deleteDocument(final String id) {
      WriteResult writeResult = datastore.delete(databaseCollection(), MorphiaDocument.class, new ObjectId(id));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public void deleteDocuments(final String collectionId) {
      Query<MorphiaDocument> query = datastore.createQuery(databaseCollection(), MorphiaDocument.class)
                                              .field(MorphiaDocument.COLLECTION_ID).equal(collectionId);
      datastore.delete(query);
   }

   @Override
   public Document getDocumentById(final String id) {
      Document document = datastore.createQuery(databaseCollection(), MorphiaDocument.class)
                                   .field(ID).equal(new ObjectId(id))
                                   .get();
      if (document == null) {
         throw new ResourceNotFoundException(ResourceType.DOCUMENT);
      }
      return document;
   }

   @Override
   public List<Document> getDocumentsByIds(final String... ids) {
      List<ObjectId> objectIds = Arrays.stream(ids).map(ObjectId::new).collect(Collectors.toList());
      List<MorphiaDocument> documents = datastore.createQuery(databaseCollection(), MorphiaDocument.class)
                                                 .field(ID).in(objectIds)
                                                 .asList();
      return new ArrayList<>(documents);
   }

   private String databaseCollection(Project project) {
      return PREFIX + project.getId();
   }

   String databaseCollection() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollection(getProject().get());
   }

}
