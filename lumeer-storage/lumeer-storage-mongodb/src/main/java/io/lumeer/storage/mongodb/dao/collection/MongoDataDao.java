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
package io.lumeer.storage.mongodb.dao.collection;

import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.mongodb.MongoUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoDataDao extends CollectionScopedDao implements DataDao {

   private static final String ID = "_id";
   private static final String PREFIX = "data_c-";

   @Override
   public void createDataRepository(final String collectionId) {
      database.createCollection(dataCollectionName(collectionId));
      createFulltextIndexOnAllFields(collectionId);
   }

   private void createFulltextIndexOnAllFields(final String collectionId) {
      dataCollection(collectionId).createIndex(Indexes.text("$**"));
   }

   @Override
   public void deleteDataRepository(final String collectionId) {
      dataCollection(collectionId).drop();
   }

   @Override
   public DataDocument createData(final String collectionId, final String documentId, final DataDocument data) {
      Document document = new Document(data).append(ID, new ObjectId(documentId));
      dataCollection(collectionId).insertOne(document);
      data.setId(documentId);
      return data;
   }

   @Override
   public DataDocument updateData(final String collectionId, final String documentId, final DataDocument data) {
      Document document = new Document(data);
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      Document updatedDocument = dataCollection(collectionId).findOneAndReplace(idFilter(documentId), document, options);
      if (updatedDocument == null) {
         throw new StorageException("Document '" + documentId + "' has not been updated (replaced).");
      }
      return MongoUtils.convertDocument(updatedDocument);
   }

   @Override
   public DataDocument patchData(final String collectionId, final String documentId, final DataDocument data) {
      Document updateDocument = new Document("$set", new Document(data));
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      Document patchedDocument = dataCollection(collectionId).findOneAndUpdate(idFilter(documentId), updateDocument, options);
      if (patchedDocument == null) {
         throw new StorageException("Document '" + documentId + "' has not been patched (partially updated).");
      }
      return MongoUtils.convertDocument(patchedDocument);
   }

   @Override
   public void deleteData(final String collectionId, final String documentId) {
      dataCollection(collectionId).deleteOne(idFilter(documentId));
   }

   @Override
   public DataDocument getData(final String collectionId, final String documentId) {
      MongoCursor<Document> mongoCursor = dataCollection(collectionId).find(idFilter(documentId)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.DOCUMENT);
      }
      return MongoUtils.convertDocument(mongoCursor.next());
   }

   @Override
   public List<DataDocument> getData(final String collectionId, final SearchQuery query) {
      MongoIterable<Document> mongoIterable = dataCollection(collectionId).find(createFilter(query));
      return MongoUtils.convertIterableToList(mongoIterable);
   }

   @Override
   public long getDataCount(final String collectionId, final SearchQuery query) {
      return dataCollection(collectionId).count(createFilter(query));
   }

   private Bson createFilter(SearchQuery query) {
      return query.isFulltextQuery() ? Filters.text(query.getFulltext()) : new BsonDocument();
   }

   MongoCollection<Document> dataCollection(String collectionId) {
      return database.getCollection(dataCollectionName(collectionId));
   }

   String dataCollectionName(String collectionId) {
      return PREFIX + collectionId;
   }

   private static Bson idFilter(String id) {
      return Filters.eq(ID, new ObjectId(id));
   }
}
