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
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;

import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@SessionScoped
public class MongoDbStorage implements DataStorage {

   private static final String CURSOR_KEY = "cursor";
   private static final String FIRST_BATCH_KEY = "firstBatch";

   private MongoDatabase database;
   private MongoClient mongoClient = null;
   private List<String> collectionCache = null;
   private long cacheLastUpdated = 0L;

   @Inject
   @Named("dataStorageConnection")
   private StorageConnection storageConnection;

   @Inject
   @Named("dataStorageDatabase")
   private String storageDatabase;

   @Inject
   private Logger log;

   @PostConstruct
   public void connect() {
      if (mongoClient == null) {
         connect(storageConnection, storageDatabase);
      }
   }

   @Override
   public void connect(final List<StorageConnection> connections, final String database) {
      final List<ServerAddress> addresses = new ArrayList<>();
      final List<MongoCredential> credentials = new ArrayList<>();

      connections.forEach(c -> {
         addresses.add(new ServerAddress(c.getHost(), c.getPort()));
         if (c.getUserName() != null && !c.getUserName().isEmpty()) {
            credentials.add(MongoCredential.createScramSha1Credential(c.getUserName(), database, c.getPassword()));
         }
      });

      this.mongoClient = new MongoClient(addresses, credentials, (new MongoClientOptions.Builder()).connectTimeout(30000).build());
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
      if (collectionCache == null || cacheLastUpdated + 5000 < System.currentTimeMillis()) {
         collectionCache = database.listCollectionNames().into(new ArrayList<>());
         cacheLastUpdated = System.currentTimeMillis();
      }
      return collectionCache;
   }

   @Override
   public void createCollection(final String collectionName) {
      if (collectionCache != null) {
         collectionCache.add(collectionName);
      }
      database.createCollection(collectionName);
   }

   @Override
   public void dropCollection(final String collectionName) {
      if (collectionCache != null) {
         collectionCache.remove(collectionName);
      }
      database.getCollection(collectionName).drop();
   }

   @Override
   public boolean hasCollection(final String collectionName) {
      return getAllCollections().contains(collectionName);
   }

   @Override
   public boolean collectionHasDocument(final String collectionName, final String documentId) {
      return database.getCollection(collectionName).find(new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId))).limit(1).iterator().hasNext();
   }

   @Override
   public String createDocument(final String collectionName, final DataDocument dataDocument) {
      Document doc = new Document(dataDocument);
      database.getCollection(collectionName).insertOne(doc);
      return doc.containsKey(LumeerConst.Document.ID) ? doc.getObjectId(LumeerConst.Document.ID).toString() : null;
   }

   @Override
   public void createOldDocument(final String collectionName, final DataDocument dataDocument, final String documentId, final int version) throws UnsuccessfulOperationException {
      Document doc = new Document(dataDocument);
      doc.put(LumeerConst.Document.ID, new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId)).append(LumeerConst.METADATA_VERSION_KEY, version));
      try {
         database.getCollection(collectionName).insertOne(doc);
      } catch (MongoWriteException e) {
         if (e.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY)) {
            throw new UnsuccessfulOperationException(e.getMessage(), e.getCause());
         } else {
            throw e;
         }
      }
   }

   @Override
   public DataDocument readDocumentIncludeAttrs(final String collectionName, final String documentId, final List<String> attributes) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      Bson projection = Projections.include(attributes);
      Document document = database.getCollection(collectionName).find(filter).projection(projection).first();

      if (document == null) {
         return null;
      }

      // converts id to string
      MongoUtils.replaceId(document);
      DataDocument readed = new DataDocument(document);
      MongoUtils.convertNestedAndListDocuments(readed);

      return readed;
   }

   @Override
   public DataDocument readDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      Document document = database.getCollection(collectionName).find(filter).first();

      if (document == null) {
         return null;
      }

      // converts id to string
      MongoUtils.replaceId(document);
      DataDocument readed = new DataDocument(document);
      MongoUtils.convertNestedAndListDocuments(readed);

      return readed;
   }

   @Override
   public DataDocument readOldDocument(final String collectionName, final String documentId, final int version) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId)).append(
            LumeerConst.METADATA_VERSION_KEY, version));
      Document document = database.getCollection(collectionName).find(filter).first();

      if (document == null) {
         return null;
      }
      MongoUtils.replaceId(document);
      DataDocument readed = new DataDocument(document);
      MongoUtils.convertNestedAndListDocuments(readed);

      return readed;
   }

   @Override
   public void updateDocument(final String collectionName, final DataDocument updatedDocument, final String documentId, final int targetVersion) {
      if (updatedDocument.containsKey(LumeerConst.Document.ID)) {
         updatedDocument.remove(LumeerConst.Document.ID);
      }
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      if (targetVersion >= 0) {
         filter.append(LumeerConst.METADATA_VERSION_KEY, updatedDocument.getInteger(LumeerConst.METADATA_VERSION_KEY));
      }
      BasicDBObject updateBson = new BasicDBObject("$set", new BasicDBObject(updatedDocument));
      updatedDocument.put(LumeerConst.Document.ID, documentId);
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   @Override
   public void dropDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      database.getCollection(collectionName).deleteOne(filter);
   }

   @Override
   public long documentCount(final String collectionName) {
      return database.getCollection(collectionName).count();
   }

   @Override
   public void dropOldDocument(final String collectionName, final String documentId, final int version) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId)).append(
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
   public void dropAttribute(final String collectionName, final String documentId, final String attributeName) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      BasicDBObject updateBson = new BasicDBObject("$unset", new BasicDBObject(attributeName, 1));
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   public <T> void addItemToArray(final String collectionName, final String documentId, final String attributeName, final T item) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      database.getCollection(collectionName).updateOne(filter, Updates.push(attributeName, MongoUtils.isDataDocument(item) ? new Document((DataDocument) item) : item));
   }

   public <T> void addItemsToArray(final String collectionName, final String documentId, final String attributeName, final List<T> items) {
      if (items.isEmpty()) {
         return;
      }
      if (MongoUtils.isDataDocument(items.get(0))) {
         List<Document> docs = new ArrayList<>();
         items.forEach((i) -> docs.add(new Document((DataDocument) i)));
         addItemsToArrayInternal(collectionName, documentId, attributeName, docs);
         return;
      }
      addItemsToArrayInternal(collectionName, documentId, attributeName, items);
   }

   private <T> void addItemsToArrayInternal(final String collectionName, final String documentId, final String attributeName, final List<T> items) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      database.getCollection(collectionName).updateOne(filter, Updates.pushEach(attributeName, items));
   }

   public <T> void removeItemFromArray(final String collectionName, final String documentId, final String attributeName, final T item) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      database.getCollection(collectionName).updateOne(filter, Updates.pull(attributeName, MongoUtils.isDataDocument(item) ? new Document((DataDocument) item) : item));
   }

   public <T> void removeItemsFromArray(final String collectionName, final String documentId, final String attributeName, final List<T> items) {
      if (items.isEmpty()) {
         return;
      }
      if (MongoUtils.isDataDocument(items.get(0))) {
         List<Document> docs = new ArrayList<>();
         items.forEach((i) -> docs.add(new Document((DataDocument) i)));
         removeItemsFromArrayInternal(collectionName, documentId, attributeName, docs);
         return;
      }
      removeItemsFromArrayInternal(collectionName, documentId, attributeName, items);
   }

   private <T> void removeItemsFromArrayInternal(final String collectionName, final String documentId, final String attributeName, final List<T> items) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      database.getCollection(collectionName).updateOne(filter, Updates.pullAll(attributeName, items));
   }

   @Override
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) {
      // skip non existing values
      final Document match = new Document("$match", new Document(attributeName, new Document("$exists", true)));
      // define grouping by out attributeName
      final Document group = new Document("$group", new Document(LumeerConst.Document.ID, "$" + attributeName));
      // sorting by id, descending, from the newest entry to oldest one
      final Document sort = new Document("$sort", new Document(LumeerConst.Document.ID, -1));
      // limit...
      final Document limit = new Document("$limit", 100);
      // this projection adds attribute with desired name, and hides _id attribute
      final Document project = new Document("$project", new Document(attributeName, "$_id").append(LumeerConst.Document.ID, 0));

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
   public List<DataDocument> run(final String command) {
      return run(BsonDocument.parse(command));
   }

   @Override
   public List<DataDocument> run(final DataDocument command) {
      return run(MongoUtils.dataDocumentToDocument(command));
   }

   private List<DataDocument> run(final Bson command) {
      final List<DataDocument> result = new ArrayList<>();

      Document cursor = (Document) database.runCommand(command).get(CURSOR_KEY);

      if (cursor != null) {
         ((ArrayList<Document>) cursor.get(FIRST_BATCH_KEY)).forEach(d -> {
            MongoUtils.replaceId(d);
            DataDocument raw = new DataDocument(d);
            MongoUtils.convertNestedAndListDocuments(raw);
            result.add(raw);
         });
      }

      return result;
   }


   @Override
   public List<DataDocument> search(final String collectionName, final String filter, final String sort, final int skip, final int limit) {
      final List<DataDocument> result = new ArrayList<>();

      MongoCollection<Document> collection = database.getCollection(collectionName);
      FindIterable<Document> documents = filter != null ? collection.find(BsonDocument.parse(filter)) : collection.find();
      if (sort != null && !sort.isEmpty()) {
         documents = documents.sort(BsonDocument.parse(sort));
      }
      if (skip > 0) {
         documents = documents.skip(skip);
      }
      if (limit > 0) {
         documents = documents.limit(limit);
      }

      documents.into(new ArrayList<>()).forEach(d -> {
         MongoUtils.replaceId(d);
         DataDocument raw = new DataDocument(d);
         MongoUtils.convertNestedAndListDocuments(raw);
         result.add(raw);
      });

      return result;
   }

   @Override
   public long count(final String collectionName, final String filter) {
      MongoCollection<Document> collection = database.getCollection(collectionName);

      return filter != null ? collection.count(BsonDocument.parse(filter)) : collection.count();
   }

   @Override
   public List<DataDocument> query(final Query query) {
      List<DataDocument> result = new LinkedList<>();
      List<DataDocument> stages = new LinkedList<>();

      if (query.getFilters().size() > 0) {
         final DataDocument filters = new DataDocument();
         filters.put("$match", query.getFilters());
         stages.add(filters);
      }

      if (query.getGrouping().size() > 0) {
         final DataDocument grouping = new DataDocument();
         grouping.put("$group", query.getGrouping());
         stages.add(grouping);
      }

      if (query.getProjections().size() > 0) {
         final DataDocument projections = new DataDocument();
         projections.put("$project", query.getProjections());
         stages.add(projections);
      }

      if (query.getSorting().size() > 0) {
         final DataDocument sorts = new DataDocument();
         sorts.put("$sort", query.getSorting());
         stages.add(sorts);
      }

      if (query.getSkip() != null && query.getSkip() > 0) {
         final DataDocument skip = new DataDocument();
         skip.put("$skip", query.getSkip());
         stages.add(skip);
      }

      if (query.getLimit() != null && query.getLimit() > 0) {
         final DataDocument limit = new DataDocument();
         limit.put("$limit", query.getLimit());
         stages.add(limit);
      }

      if (query.getOutput() != null && !query.getOutput().isEmpty()) {
         final DataDocument output = new DataDocument();
         output.put("$out", query.getOutput());
         stages.add(output);
      }

      query.getCollections().forEach(collection -> {
         result.addAll(aggregate(collection, stages.toArray(new DataDocument[stages.size()])));
      });

      return result;
   }

   @Override
   public List<DataDocument> aggregate(final String collectionName, final DataDocument... stages) {
      if (stages == null || stages.length == 0) {
         return Collections.EMPTY_LIST;
      }

      final List<DataDocument> result = new LinkedList<>();
      final List<Document> documents = new LinkedList<>();
      for (final DataDocument d : stages) {
         documents.add(MongoUtils.dataDocumentToDocument(d));
      }

      AggregateIterable<Document> resultDocuments = database.getCollection(collectionName).aggregate(documents);
      resultDocuments.into(new LinkedList<>()).forEach(d -> {
         if (d.get(LumeerConst.Document.ID) instanceof Document) {
            d.replace(LumeerConst.Document.ID, ((Document) d.get(LumeerConst.Document.ID)).toJson());
         } else {
            d.replace(LumeerConst.Document.ID, d.getObjectId(LumeerConst.Document.ID).toString());
         }
         DataDocument raw = new DataDocument(d);
         MongoUtils.convertNestedAndListDocuments(raw);
         result.add(raw);
      });

      return result;
   }

   @Override
   public void incrementAttributeValueBy(final String collectionName, final String documentId, final String attributeName, final int incBy) {
      BasicDBObject filter = new BasicDBObject(LumeerConst.Document.ID, new ObjectId(documentId));
      BasicDBObject updateBson = new BasicDBObject("$inc", new BasicDBObject(new Document(attributeName, incBy)));
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   @Override
   public synchronized int getNextSequenceNo(final String collectionName, final String indexAttribute, final String index) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Document doc = database.getCollection(collectionName).findOneAndUpdate(Filters.eq(indexAttribute, index), Updates.inc("seq", 1),
            options);

      if (doc == null) { // the sequence did not exist
         resetSequence(collectionName, indexAttribute, index);
         return 0;
      } else {
         return doc.getInteger("seq");
      }
   }

   @Override
   public synchronized void resetSequence(final String collectionName, final String indexAttribute, final String index) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Document doc = database.getCollection(collectionName).findOneAndUpdate(Filters.eq(indexAttribute, index), Updates.set("seq", 0),
            options);

      if (doc == null) {
         Document newSeq = new Document();
         newSeq.put(indexAttribute, index);
         newSeq.put("seq", 0);
         database.getCollection(collectionName).insertOne(newSeq);
      }
   }

   @Override
   public void createIndex(final String collectionName, final Map<String, String> indexAttributes) {
      final StringBuilder indexJson = new StringBuilder();
      indexAttributes.forEach((k, v) -> {
         if (indexJson.length() > 0) {
            indexJson.append(", ");
         }
         indexJson.append(k);
         indexJson.append(":");
         indexJson.append(v);
      });
      database.getCollection(collectionName).createIndex(BsonDocument.parse("{" + indexJson.toString() + "}"));
   }

   @Override
   public List<DataDocument> listIndexes(final String collectionName) {
      final List<DataDocument> result = new ArrayList<>();

      ((Iterable<Document>) database.getCollection(collectionName).listIndexes()).forEach(d -> result.add(new DataDocument(d)));

      return result;
   }

   @Override
   public void dropIndex(final String collectionName, final String indexName) {
      database.getCollection(collectionName).dropIndex(indexName);
   }

   @Override
   public void invalidateCaches() {
      collectionCache = null;
   }
}