/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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
package io.lumeer.mongodb;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.StorageConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Model
public class MongoDbStorage implements DataStorage {

   private static final String ID = "_id";
   private static final String CURSOR_KEY = "cursor";
   private static final String FIRST_BATCH_KEY = "firstBatch";

   private MongoDatabase database;
   private MongoClient mongoClient = null;

   @Inject
   @Named("dataStorageConnection")
   private StorageConnection storageConnection;

   @Inject
   @Named("dataStorageDatabase")
   private String storageDatabase;

   @PostConstruct
   public void connect() {
      if (mongoClient == null) {
         connect(storageConnection, storageDatabase);
         //connect(new StorageConnection(DB_HOST, DB_PORT, DB_USER, DB_PASSWORD), DB_NAME);
      }
   }

   @Override
   public void connect(final List<StorageConnection> connections, final String database) {
      final List<ServerAddress> addresses = new ArrayList<>();
      final List<MongoCredential> credentials = new ArrayList<>();

      connections.forEach(c -> {
         addresses.add(new ServerAddress(c.getHost(), c.getPort()));
         if (c.getUserName() != null && !c.getUserName().isEmpty()) {
            credentials.add(MongoCredential.createCredential(c.getUserName(), database, c.getPassword()));
         }
      });

      this.mongoClient = new MongoClient(addresses, credentials);
      this.database = mongoClient.getDatabase(database);
   }

   @PreDestroy
   @Override
   public void disconnect() {
      if (mongoClient != null) {
         mongoClient.close();
      }
   }

   @Override
   public List<String> getAllCollections() {
      return database.listCollectionNames().into(new ArrayList<>());
   }

   @Override
   public void createCollection(final String collectionName) {
      database.createCollection(collectionName);
   }

   @Override
   public void dropCollection(final String collectionName) {
      database.getCollection(collectionName).drop();
   }

   @Override
   public String createDocument(final String collectionName, final DataDocument dataDocument) {
      Document doc = new Document(dataDocument);
      database.getCollection(collectionName).insertOne(doc);
      return doc.containsKey(ID) ? doc.getObjectId(ID).toString() : null;
   }

   @Override
   public void createOldDocument(final String collectionName, final DataDocument dataDocument, final String documentId, final int version) throws Exception {
      Document doc = new Document(dataDocument);
      doc.put(ID, new BasicDBObject(ID, new ObjectId(documentId)).append(
            LumeerConst.METADATA_VERSION_KEY, version));
      try {
         database.getCollection(collectionName).insertOne(doc);
      } catch (MongoWriteException e) {
         if (e.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY)) {
            throw new Exception(e.getMessage(), e.getCause());
         } else {
            throw e;
         }
      }
   }

   @Override
   public DataDocument readDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject(ID, new ObjectId(documentId));
      Document document = database.getCollection(collectionName).find(filter).first();

      if (document == null) {
         return null;
      }

      // converts id to string
      String documentIdString = document.getObjectId(ID).toString();
      document.replace(ID, documentIdString);

      DataDocument readed = new DataDocument(document);
      convertNestedDocuments(readed);

      return readed;
   }

   @Override
   public DataDocument readOldDocument(final String collectionName, final String documentId, final int version) {
      BasicDBObject filter = new BasicDBObject(ID, new BasicDBObject(ID, new ObjectId(documentId)).append(
            LumeerConst.METADATA_VERSION_KEY, version));
      Document document = database.getCollection(collectionName).find(filter).first();

      if (document == null) {
         return null;
      }
      DataDocument readed = new DataDocument(document);
      convertNestedDocuments(readed);

      return readed;
   }

   @Override
   public void updateDocument(final String collectionName, final DataDocument updatedDocument, final String documentId, final int targetVersion) {
      if (updatedDocument.containsKey(ID)) {
         updatedDocument.remove(ID);
      }
      BasicDBObject filter = new BasicDBObject(ID, new ObjectId(documentId));
      if (targetVersion >= 0) {
         filter.append(LumeerConst.METADATA_VERSION_KEY, updatedDocument.getInteger(LumeerConst.METADATA_VERSION_KEY));
      }
      BasicDBObject updateBson = new BasicDBObject("$set", new BasicDBObject(updatedDocument));
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   @Override
   public void dropDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject(ID, new ObjectId(documentId));
      database.getCollection(collectionName).deleteOne(filter);
   }

   @Override
   public void dropOldDocument(final String collectionName, final String documentId, final int version) {
      BasicDBObject filter = new BasicDBObject(ID, new BasicDBObject(ID, new ObjectId(documentId)).append(
            LumeerConst.METADATA_VERSION_KEY, version));
      database.getCollection(collectionName).deleteOne(filter);
   }

   @Override
   public void dropManyDocuments(final String collectionName, final String filter) {
      database.getCollection(collectionName).deleteMany(BsonDocument.parse(filter));
   }

   @Override
   public void renameAttribute(final String collectionName, final String oldName, final String newName) {
      database.getCollection(collectionName).updateMany(BsonDocument.parse("{}"), Updates.rename(oldName, newName));
   }

   @Override
   public void removeAttribute(final String collectionName, final String documentId, final String attributeName) {
      BasicDBObject filter = new BasicDBObject(ID, new ObjectId(documentId));
      BasicDBObject updateBson = new BasicDBObject("$unset", new BasicDBObject(attributeName, 1));
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   @Override
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) {
      // skip non existing values
      final Document match = new Document("$match", new Document(attributeName, new Document("$exists", true)));
      // define grouping by out attributeName
      final Document group = new Document("$group", new Document(ID, "$" + attributeName));
      // sorting by id, descending, from the newest entry to oldest one
      final Document sort = new Document("$sort", new Document(ID, -1));
      // limit...
      final Document limit = new Document("$limit", 100);
      // this projection adds attribute with desired name, and hides _id attribute
      final Document project = new Document("$project", new Document(attributeName, "$_id").append("_id", 0));

      AggregateIterable<Document> aggregate = database.getCollection(collectionName).aggregate(Arrays.asList(match, group, sort, limit, project));
      Set<String> attributeValues = new HashSet<>();
      for (Document doc : aggregate) {
         // there is only one column with name "attributeName"
         attributeValues.add(doc.get(attributeName).toString());
      }
      return attributeValues;
   }

   @SuppressWarnings("unchecked")
   @Override
   public List<DataDocument> search(final String query) {
      final List<DataDocument> result = new ArrayList<>();

      Document cursor = (Document) database.runCommand(BsonDocument.parse(query)).get(CURSOR_KEY);

      ((ArrayList<Document>) cursor.get(FIRST_BATCH_KEY)).forEach(d -> {
         d.replace(ID, d.getObjectId(ID).toString());
         DataDocument raw = new DataDocument(d);
         convertNestedDocuments(raw);
         result.add(raw);
      });

      return result;
   }

   @Override
   public List<DataDocument> search(final String collectionName, final String filter, final String sort, final int skip, final int limit) {
      final List<DataDocument> result = new ArrayList<>();

      MongoCollection<Document> collection = database.getCollection(collectionName);
      FindIterable<Document> documents = filter != null ? collection.find(BsonDocument.parse(filter)) : collection.find();
      documents.sort(sort != null ? BsonDocument.parse(sort) : null)
               .skip(skip)
               .limit(limit)
               .into(new ArrayList<>())
               .forEach(d -> {
                  d.replace(ID, d.getObjectId(ID).toString());
                  DataDocument raw = new DataDocument(d);
                  convertNestedDocuments(raw);
                  result.add(raw);
               });

      return result;
   }

   @Override
   public void incerementAttributeValueBy(final String collectionName, final String documentId, final String attributeName, final int incBy) {
      BasicDBObject filter = new BasicDBObject(ID, new ObjectId(documentId));
      BasicDBObject updateBson = new BasicDBObject("$inc", new BasicDBObject(new Document(attributeName, incBy)));
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   private void convertNestedDocuments(DataDocument dataDcument) {
      for (String key : dataDcument.keySet()) {
         Object value = dataDcument.get(key);
         if (value instanceof Document) {
            DataDocument converted = new DataDocument((Document) value);
            dataDcument.replace(key, converted);
            convertNestedDocuments(converted);
         }
      }
   }

}