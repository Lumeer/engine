/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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

import io.lumeer.engine.api.data.DataDocument;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class MongoUtils {

   private static final String DOCUMENT_ID = "_id";

   private MongoUtils() {

   }

   public static DataDocument convertDocument(Document document) {
      MongoUtils.replaceId(document);
      DataDocument dataDocument = new DataDocument(document);
      MongoUtils.convertNestedAndListDocuments(dataDocument);
      return dataDocument;
   }

   public static List<DataDocument> convertIterableToList(MongoIterable<Document> documents) {
      final List<DataDocument> result = new ArrayList<>();
      MongoCursor<Document> it = documents.iterator();
      while (it.hasNext()) {
         result.add(MongoUtils.convertDocument(it.next()));
      }
      it.close();

      return result;
   }

   public static String convertBsonToJson(Bson object) {
      return object.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()).toJson();
   }

   public static void convertNestedAndListDocuments(DataDocument dataDocument) {
      for (String key : dataDocument.keySet()) {
         Object value = dataDocument.get(key);
         if (isDocument(value)) {
            DataDocument converted = new DataDocument((Document) value);
            dataDocument.replace(key, converted);
            convertNestedAndListDocuments(converted);
         } else if (isList(value)) {
            List l = (List) value;
            if (!l.isEmpty() && isDocument(l.get(0))) {
               ArrayList<DataDocument> docs = new ArrayList<>(l.size());
               dataDocument.replace(key, docs);
               for (Object o : l) {
                  if (!isDocument(o)) {
                     continue;
                  }
                  DataDocument d = new DataDocument((Document) o);
                  docs.add(d);
                  convertNestedAndListDocuments(d);
               }
            }
         } else if (value instanceof Decimal128 d) {
            dataDocument.replace(key, d.bigDecimalValue());
         }
      }
   }

   /**
    * Converts {@link DataDocument} recursively to {@link Document}.
    *
    * @param dataDocument
    *       {@link DataDocument} to convert.
    * @return Converted {@link Document}.
    */
   public static Document dataDocumentToDocument(final DataDocument dataDocument) {
      for (final String key : dataDocument.keySet()) {
         if (isDataDocument(dataDocument.get(key))) {
            dataDocument.replace(key, dataDocumentToDocument((DataDocument) dataDocument.get(key)));
         }
      }

      return new Document(dataDocument);
   }

   public static void replaceId(final Document document) {
      Object docId = document.get(DOCUMENT_ID);
      if (docId instanceof ObjectId) { // classic document
         document.replace(DOCUMENT_ID, docId.toString());
      } else if (docId instanceof Document d) { // shadow document
         if (d.containsKey(DOCUMENT_ID)) {
            d.replace(DOCUMENT_ID, d.getObjectId(DOCUMENT_ID).toString());
         }
         DataDocument doc = new DataDocument(d);
         document.replace(DOCUMENT_ID, doc);
      }
   }

   public static boolean isDataDocument(Object obj) {
      return obj instanceof DataDocument;
   }

   public static boolean isDocument(Object obj) {
      return obj instanceof Document;
   }

   public static boolean isList(Object obj) {
      return obj instanceof List;
   }

   public static String concatParams(String... args) {
      if (args.length == 0) {
         return "";
      }
      StringBuilder returnParam = new StringBuilder(args[0]);
      for (int i = 1; i < args.length; i++) {
         returnParam.append(".").append(args[i]);
      }
      return returnParam.toString();
   }

}
