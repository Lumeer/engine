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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Model
public class MongoDbStorage implements DataStorage {

   private MongoDatabase database;

   @PostConstruct
   public void connect() {
      MongoClient mongo = new MongoClient("localhost", 27017); // default connection
      database = mongo.getDatabase("lumeer");
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
   public String createDocument(final String collectionName, final DataDocument document) {
      Document doc = new Document();
      doc.putAll(document);
      database.getCollection(collectionName).insertOne(doc);
      return doc.getObjectId("_id").toString();
   }

   @Override
   public DataDocument readDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject("_id", new ObjectId(documentId));
      Document document = database.getCollection(collectionName).find(filter).first();

      if (document == null) {
         return null;
      }

      DataDocument dataDocument = new DataDocument();
      dataDocument.putAll(document);
      dataDocument.remove("_id");
      return dataDocument;
   }

   @Override
   public void updateDocument(final String collectionName, final DataDocument updatedDocument, final String documentId) {
      BasicDBObject filter = new BasicDBObject("_id", new ObjectId(documentId));
      BasicDBObject updateBson = new BasicDBObject("$set",new BasicDBObject(updatedDocument) );
      database.getCollection(collectionName).updateOne(filter, updateBson);
   }

   @Override
   public void dropDocument(final String collectionName, final String documentId) {
      BasicDBObject filter = new BasicDBObject("_id", new ObjectId(documentId));
      database.getCollection(collectionName).deleteOne(filter);
   }

   @Override
   public List<DataDocument> search(final String query, final int page, final int limit) {
      // TODO
      return null;
   }

   @Override
   public void renameAttribute(final String collectionName, final String oldName, final String newName) {
      database.getCollection(collectionName).updateMany(BsonDocument.parse("{}"), Updates.rename(oldName, newName));
   }
}