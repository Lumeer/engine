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
package io.lumeer.storage.mongodb;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.*;

import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheProvider;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataSort;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageStats;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.storage.mongodb.codecs.BigDecimalCodec;
import io.lumeer.storage.mongodb.codecs.RoleCodec;
import io.lumeer.storage.mongodb.codecs.ZonedDateTimeCodec;
import io.lumeer.storage.mongodb.codecs.providers.AttributeCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.CompanyContactCodedProvider;
import io.lumeer.storage.mongodb.codecs.providers.GroupCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.LinkInstanceCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.LinkTypeCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.PaymentCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.PermissionCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.PermissionsCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.QueryCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.UserCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.UserLoginEventCodecProvider;
import io.lumeer.storage.mongodb.codecs.providers.ViewCodecProvider;

import com.mongodb.BasicDBObject;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoNamespace;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class MongoDbStorage implements DataStorage {

   private static final String CURSOR_KEY = "cursor";
   private static final String FIRST_BATCH_KEY = "firstBatch";
   private static final String COLLECTION_CACHE = "collections";

   private static final String DOCUMENT_ID = "_id";

   private MongoDatabase database;
   private MongoClient mongoClient = null;

   private final Morphia morphia;
   private AdvancedDatastore datastore;

   private long cacheLastUpdated = 0L;
   private CacheProvider cacheProvider;
   private AtomicReference<Cache<List<String>>> collectionCacheRef = new AtomicReference<>();

   public MongoDbStorage(Morphia morphia) {
      this.morphia = morphia;
   }

   @Override
   public void setCacheProvider(final CacheProvider cacheProvider) {
      this.cacheProvider = cacheProvider;
   }

   private List<String> getCollectionCache() {
      if (cacheProvider != null) {
         Cache<List<String>> cache = collectionCacheRef.get();
         if (cache == null) {
            cache = cacheProvider.getCache(COLLECTION_CACHE);         // create and initialize actual instance
            if (collectionCacheRef.compareAndSet(null, cache)) // CAS succeeded
            {
               return cache.get();
            } else                                                      // CAS failed: other thread set an object
            {
               return collectionCacheRef.get().get();
            }
         } else {
            return cache.get();
         }
      } else {
         return null;
      }
   }

   private void setCollectionCache(final List<String> collections) {
      Cache<List<String>> cache = collectionCacheRef.get();
      if (cache != null) {
         cache.set(collections);
      }
   }

   @Override
   public void connect(final List<StorageConnection> connections, final String database, final Boolean useSsl) {
      final List<ServerAddress> addresses = new ArrayList<>();
      final List<MongoCredential> credentials = new ArrayList<>();

      connections.forEach(c -> {
         addresses.add(new ServerAddress(c.getHost(), c.getPort()));
         if (c.getUserName() != null && !c.getUserName().isEmpty()) {
            credentials.add(MongoCredential.createScramSha1Credential(c.getUserName(), database, c.getPassword()));
         }
      });

      final MongoClientOptions.Builder optionsBuilder = (new MongoClientOptions.Builder()).connectTimeout(30000);

      if (useSsl) {
         optionsBuilder.sslEnabled(true).socketFactory(NaiveTrustManager.getSocketFactory()).sslInvalidHostNameAllowed(true);
      }

      final CodecRegistry defaultRegistry = MongoClient.getDefaultCodecRegistry();
      final CodecRegistry codecRegistry = CodecRegistries.fromCodecs(new BigDecimalCodec(), new RoleCodec(), new ZonedDateTimeCodec());
      final CodecRegistry providersRegistry = CodecRegistries.fromProviders(
            new PermissionsCodecProvider(), new PermissionCodecProvider(), new QueryCodecProvider(), new ViewCodecProvider(),
            new AttributeCodecProvider(), new LinkInstanceCodecProvider(), new LinkTypeCodecProvider(), new UserCodecProvider(),
            new GroupCodecProvider(), new PaymentCodecProvider(), new CompanyContactCodedProvider(), new UserLoginEventCodecProvider()
      );
      final CodecRegistry registry = CodecRegistries.fromRegistries(defaultRegistry, codecRegistry, providersRegistry);

      this.mongoClient = new MongoClient(addresses, credentials, optionsBuilder.codecRegistry(registry).build());
      this.database = mongoClient.getDatabase(database);
      this.datastore = (AdvancedDatastore) morphia.createDatastore(this.mongoClient, database);
   }

   @Override
   public void disconnect() {
      if (mongoClient != null) {
         mongoClient.close();
      }
   }

   @Override
   @SuppressWarnings("unchecked")
   public List<String> getAllCollections() {
      if (getCollectionCache() != null) {
         collectionCacheRef.get().lock(COLLECTION_CACHE);
         try {
            if (getCollectionCache() == null || cacheLastUpdated + 5000 < System.currentTimeMillis()) {
               setCollectionCache(database.listCollectionNames().into(new ArrayList<>()));
               cacheLastUpdated = System.currentTimeMillis();
            }
         } finally {
            collectionCacheRef.get().unlock(COLLECTION_CACHE);
         }

         return getCollectionCache();
      } else {
         return database.listCollectionNames().into(new ArrayList<>());
      }
   }

   @Override
   public void createCollection(final String collectionName) {
      if (getCollectionCache() != null) {
         collectionCacheRef.get().lock(COLLECTION_CACHE);
         try {
            final List<String> collections = getCollectionCache();

            if (collections != null) {
               collections.add(collectionName);
            } else {
               setCollectionCache(new ArrayList<>(Collections.singletonList(collectionName)));
            }

            database.createCollection(collectionName);
         } finally {
            collectionCacheRef.get().unlock(COLLECTION_CACHE);
         }
      } else {
         database.createCollection(collectionName);
      }
   }

   @Override
   public void dropCollection(final String collectionName) {
      if (getCollectionCache() != null) {
         collectionCacheRef.get().lock(COLLECTION_CACHE);
         try {
            final List<String> collections = getCollectionCache();

            if (collections != null) {
               collections.remove(collectionName);
            }

            database.getCollection(collectionName).drop();
         } finally {
            collectionCacheRef.get().unlock(COLLECTION_CACHE);
         }
      } else {
         database.getCollection(collectionName).drop();
      }
   }

   @Override
   public void renameCollection(final String oldCollectionName, final String newCollectionName) {
      if (getCollectionCache() != null) {
         collectionCacheRef.get().lock(COLLECTION_CACHE);
         try {
            final List<String> collections = getCollectionCache();

            if (collections != null) {
               collections.remove(oldCollectionName);
               collections.add(newCollectionName);
            }

            if (hasCollection(oldCollectionName)) {
               database.getCollection(oldCollectionName).renameCollection(new MongoNamespace(database.getName(), newCollectionName));
            }
         } finally {
            collectionCacheRef.get().unlock(COLLECTION_CACHE);
         }
      } else {
         if (hasCollection(oldCollectionName)) {
            database.getCollection(oldCollectionName).renameCollection(new MongoNamespace(database.getName(), newCollectionName));
         }
      }
   }

   @Override
   public boolean hasCollection(final String collectionName) {
      return getAllCollections().contains(collectionName);
   }

   @Override
   public boolean collectionHasDocument(final String collectionName, final DataFilter filter) {
      return database.getCollection(collectionName).find(filter.<Bson>get()).limit(1).iterator().hasNext();
   }

   @Override
   public String createDocument(final String collectionName, final DataDocument dataDocument) {
      Document doc = new Document(dataDocument);

      if (getCollectionCache() != null) {
         collectionCacheRef.get().lock(COLLECTION_CACHE);
         try {
            final List<String> collections = getCollectionCache();

            if (collections != null) {
               collections.add(collectionName);
            } else {
               setCollectionCache(new ArrayList<>(Collections.singletonList(collectionName)));
            }

            database.getCollection(collectionName).insertOne(doc);
         } finally {
            collectionCacheRef.get().unlock(COLLECTION_CACHE);
         }
      } else {
         database.getCollection(collectionName).insertOne(doc);
      }
      return doc.containsKey(DOCUMENT_ID) ? doc.getObjectId(DOCUMENT_ID).toString() : null;
   }

   @Override
   public List<String> createDocuments(final String collectionName, final List<DataDocument> dataDocuments) {
      List<Document> documents = dataDocuments.stream()
                                              .map(MongoUtils::dataDocumentToDocument)
                                              .collect(Collectors.toList());

      if (getCollectionCache() != null) {
         collectionCacheRef.get().lock(COLLECTION_CACHE);
         try {
            final List<String> collections = getCollectionCache();

            if (collections != null) {
               collections.add(collectionName);
            } else {
               setCollectionCache(new ArrayList<>(Collections.singletonList(collectionName)));
            }

            database.getCollection(collectionName).insertMany(documents, new InsertManyOptions().ordered(false));
         } finally {
            collectionCacheRef.get().unlock(COLLECTION_CACHE);
         }
      } else {
         database.getCollection(collectionName).insertMany(documents, new InsertManyOptions().ordered(false));
      }

      return documents.stream()
                      .filter(d -> d.containsKey(DOCUMENT_ID))
                      .map(d -> d.getObjectId(DOCUMENT_ID).toString())
                      .collect(Collectors.toList());
   }

   @Override
   public void createOldDocument(final String collectionName, final DataDocument dataDocument, final String documentId, final int version) throws UnsuccessfulOperationException {
      Document doc = new Document(dataDocument);
      doc.put(DOCUMENT_ID, new BasicDBObject(DOCUMENT_ID, new ObjectId(documentId)));
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
   public DataDocument readDocumentIncludeAttrs(final String collectionName, final DataFilter filter, final List<String> attributes) {
      Document document = database.getCollection(collectionName).find(filter.<Bson>get()).projection(Projections.include(attributes)).limit(1).first();
      return document != null ? MongoUtils.convertDocument(document) : null;
   }

   @Override
   public DataDocument readDocument(final String collectionName, final DataFilter filter) {
      Document document = database.getCollection(collectionName).find(filter.<Bson>get()).limit(1).first();

      return document != null ? MongoUtils.convertDocument(document) : null;
   }

   @Override
   public void updateDocument(final String collectionName, final DataDocument updatedDocument, final DataFilter filter) {
      DataDocument toUpdate = new DataDocument(updatedDocument);
      if (toUpdate.containsKey(DOCUMENT_ID)) {
         toUpdate.remove(DOCUMENT_ID);
      }
      BasicDBObject updateBson = new BasicDBObject("$set", new BasicDBObject(toUpdate));
      database.getCollection(collectionName).updateOne(filter.<Bson>get(), updateBson, new UpdateOptions().upsert(true));
   }

   @Override
   public void replaceDocument(final String collectionName, final DataDocument replaceDocument, final DataFilter filter) {
      DataDocument toReplace = new DataDocument(replaceDocument);
      if (toReplace.containsKey(DOCUMENT_ID)) {
         toReplace.remove(DOCUMENT_ID);
      }
      Document replaceDoc = new Document(toReplace);
      database.getCollection(collectionName).replaceOne(filter.<Bson>get(), replaceDoc, new UpdateOptions().upsert(true));
   }

   @Override
   public void dropDocument(final String collectionName, final DataFilter filter) {
      database.getCollection(collectionName).deleteOne(filter.<Bson>get());
   }

   @Override
   public long documentCount(final String collectionName) {
      return database.getCollection(collectionName).count();
   }

   @Override
   public void dropManyDocuments(final String collectionName, final DataFilter filter) {
      database.getCollection(collectionName).deleteMany(filter.<Bson>get());
   }

   @Override
   public void renameAttribute(final String collectionName, final String oldName, final String newName) {
      database.getCollection(collectionName).updateMany(BsonDocument.parse("{}"), rename(oldName, newName));
   }

   @Override
   public void dropAttribute(final String collectionName, final DataFilter filter, final String attributeName) {
      database.getCollection(collectionName).updateOne(filter.<Bson>get(), unset(attributeName));
   }

   @Override
   public <T> void addItemToArray(final String collectionName, final DataFilter filter, final String attributeName, final T item) {
      database.getCollection(collectionName).updateOne(filter.<Bson>get(), addToSet(attributeName, MongoUtils.isDataDocument(item) ? new Document((DataDocument) item) : item));
   }

   @Override
   public <T> void addItemsToArray(final String collectionName, final DataFilter filter, final String attributeName, final List<T> items) {
      if (items.isEmpty()) {
         return;
      }
      if (MongoUtils.isDataDocument(items.get(0))) {
         List<Document> docs = new ArrayList<>();
         items.forEach((i) -> docs.add(new Document((DataDocument) i)));
         addItemsToArrayInternal(collectionName, filter, attributeName, docs);
         return;
      }
      addItemsToArrayInternal(collectionName, filter, attributeName, items);
   }

   private <T> void addItemsToArrayInternal(final String collectionName, final DataFilter filter, final String attributeName, final List<T> items) {
      database.getCollection(collectionName).updateOne(filter.<Bson>get(), addEachToSet(attributeName, items));
   }

   @Override
   public <T> void removeItemFromArray(final String collectionName, final DataFilter filter, final String attributeName, final T item) {
      database.getCollection(collectionName).updateMany(filter.<Bson>get(), pull(attributeName, MongoUtils.isDataDocument(item) ? new Document((DataDocument) item) : item));
   }

   @Override
   public <T> void removeItemsFromArray(final String collectionName, final DataFilter filter, final String attributeName, final List<T> items) {
      if (items.isEmpty()) {
         return;
      }
      if (MongoUtils.isDataDocument(items.get(0))) {
         List<Document> docs = new ArrayList<>();
         items.forEach((i) -> docs.add(new Document((DataDocument) i)));
         removeItemsFromArrayInternal(collectionName, filter, attributeName, docs);
         return;
      }
      removeItemsFromArrayInternal(collectionName, filter, attributeName, items);
   }

   private <T> void removeItemsFromArrayInternal(final String collectionName, final DataFilter filter, final String attributeName, final List<T> items) {
      database.getCollection(collectionName).updateMany(filter.<Bson>get(), pullAll(attributeName, items));
   }

   @Override
   public Set<String> getAttributeValues(final String collectionName, final String attributeName) {
      // skip non existing values
      Bson match = match(exists(attributeName));
      // define grouping by out attributeName
      Bson group = group("$" + attributeName, Collections.emptyList());
      // sorting by id, descending, from the newest entry to oldest one
      Bson sort = sort(descending(DOCUMENT_ID));
      // limit...
      Bson limit = limit(100);
      // this projection adds attribute with desired name, and hides _id attribute
      Bson project = project(new Document(attributeName, "$_id").append(DOCUMENT_ID, 0));

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
            result.add(MongoUtils.convertDocument(d));
         });
      }

      return result;
   }

   @Override
   public List<DataDocument> search(final String collectionName, final DataFilter filter, final List<String> attributes) {
      return search(collectionName, filter, null, attributes, 0, 0);
   }

   @Override
   public List<DataDocument> search(final String collectionName, final DataFilter filter, final DataSort sort, final int skip, final int limit) {
      return search(collectionName, filter, sort, null, skip, limit);
   }

   @Override
   public List<DataDocument> search(String collectionName, DataFilter filter, final DataSort sort, List<String> attributes, final int skip, int limit) {
      MongoCollection<Document> collection = database.getCollection(collectionName);
      FindIterable<Document> documents = filter != null ? collection.find(filter.<Bson>get()) : collection.find();
      if (sort != null) {
         documents = documents.sort(sort.<Bson>get());
      }
      if (attributes != null && !attributes.isEmpty()) {
         documents.projection(Projections.fields(Projections.include(attributes)));
      }
      if (skip > 0) {
         documents = documents.skip(skip);
      }
      if (limit > 0) {
         documents = documents.limit(limit);
      }

      return MongoUtils.convertIterableToList(documents);
   }

   @Override
   public long count(final String collectionName, final DataFilter filter) {
      MongoCollection<Document> collection = database.getCollection(collectionName);

      return filter != null ? collection.count(filter.<Bson>get()) : collection.count();
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
         return Collections.emptyList();
      }

      final List<DataDocument> result = new LinkedList<>();
      final List<Document> documents = new LinkedList<>();
      for (final DataDocument d : stages) {
         documents.add(MongoUtils.dataDocumentToDocument(d));
      }

      AggregateIterable<Document> resultDocuments = database.getCollection(collectionName).aggregate(documents);
      resultDocuments.into(new LinkedList<>()).forEach(d -> {
         result.add(MongoUtils.convertDocument(d));
      });

      return result;
   }

   @Override
   public void incrementAttributeValueBy(final String collectionName, final DataFilter filter, final String attributeName, final int incBy) {
      database.getCollection(collectionName).updateOne(filter.<Bson>get(), inc(attributeName, incBy));
   }

   @Override
   public synchronized int getNextSequenceNo(final String collectionName, final String indexAttribute, final String index) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Document doc = database.getCollection(collectionName).findOneAndUpdate(eq(indexAttribute, index), inc("seq", 1),
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

      final Document doc = database.getCollection(collectionName).findOneAndUpdate(eq(indexAttribute, index), set("seq", 0),
            options);

      if (doc == null) {
         Document newSeq = new Document();
         newSeq.put(indexAttribute, index);
         newSeq.put("seq", 0);
         database.getCollection(collectionName).insertOne(newSeq);
      }
   }

   @Override
   public void createIndex(final String collectionName, final DataDocument indexAttributes, boolean unique) {
      database.getCollection(collectionName).createIndex(MongoUtils.dataDocumentToDocument(indexAttributes), new IndexOptions().unique(unique));
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
      if (getCollectionCache() != null) {
         collectionCacheRef.get().remove(COLLECTION_CACHE);
      }
   }

   @Override
   public DataStorageStats getDbStats() {
      final Document dbStats = database.runCommand(Document.parse("{ dbStats: 1, scale: 1 }"));
      final DataStorageStats dss = new DataStorageStats();

      dss.setDatabaseName(dbStats.getString("db"));
      dss.setCollections(dbStats.getInteger("collections"));
      dss.setDocuments(dbStats.getInteger("objects"));
      dss.setDataSize(dbStats.getDouble("dataSize").longValue());
      dss.setStorageSize(dbStats.getDouble("storageSize").longValue());
      dss.setIndexes(dbStats.getInteger("indexes"));
      dss.setIndexSize(dbStats.getDouble("indexSize").longValue());

      return dss;
   }

   @Override
   public DataStorageStats getCollectionStats(final String collectionName) {
      final Document collStats = database.runCommand(Document.parse("{ collStats: \"" + collectionName + "\", scale: 1, verbose: false }"));
      final DataStorageStats dss = new DataStorageStats();

      final String ns = collStats.getString("ns");

      dss.setDatabaseName(ns.substring(0, ns.indexOf(".")));
      dss.setCollectionName(ns.substring(ns.indexOf(".") + 1));
      dss.setDocuments(collStats.getInteger("count"));
      dss.setDataSize(collStats.getInteger("size"));
      dss.setStorageSize(collStats.getInteger("storageSize"));
      dss.setIndexes(collStats.getInteger("nindexes"));
      dss.setIndexSize(collStats.getInteger("totalIndexSize"));

      return dss;
   }

   public MongoDatabase getDatabase() {
      return database;
   }

   public AdvancedDatastore getDataStore() {
      return datastore;
   }
}
