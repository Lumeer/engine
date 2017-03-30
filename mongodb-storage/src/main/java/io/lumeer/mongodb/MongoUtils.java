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
package io.lumeer.mongodb;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import com.mongodb.MongoClient;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="kubedo8@gmail.com">Jakub Rodák</a>
 */
public class MongoUtils {

   private MongoUtils() {

   }

   public static String convertBsonToJson(Bson object) {
      return object.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry()).toJson();
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
      Object docId = document.get(LumeerConst.Document.ID);
      if (docId instanceof ObjectId) { // classic document
         document.replace(LumeerConst.Document.ID, docId.toString());
      } else if (docId instanceof Document) { // shadow document
         Document d = (Document) docId;
         if (d.containsKey(LumeerConst.Document.ID)) {
            d.replace(LumeerConst.Document.ID, d.getObjectId(LumeerConst.Document.ID).toString());
         }
         DataDocument doc = new DataDocument(d);
         document.replace(LumeerConst.Document.ID, doc);
      }
   }

   public static boolean isDataDocument(Object obj) {
      return obj != null && obj instanceof DataDocument;
   }

   public static boolean isDocument(Object obj) {
      return obj != null && obj instanceof Document;
   }

   public static boolean isList(Object obj) {
      return obj != null && obj instanceof List;
   }

   public static String concatParams(String... args) {
      if (args.length == 0) {
         return "";
      }
      String returnParam = args[0];
      for (int i = 1; i < args.length; i++) {
         returnParam += "." + args[i];
      }
      return returnParam;
   }

   // returns string "parent.child"
   public static String nestedAttributeName(String parent, String child) {
      return parent + "." + child;
   }

}
