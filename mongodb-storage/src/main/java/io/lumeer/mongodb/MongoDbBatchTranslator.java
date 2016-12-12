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
package io.lumeer.mongodb;

import io.lumeer.engine.api.batch.Batch;
import io.lumeer.engine.api.batch.MergeBatch;
import io.lumeer.engine.api.batch.SplitBatch;

import com.mongodb.client.MongoDatabase;
import org.bson.BsonString;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MongoDbBatchTranslator {

   public static void translateBatch(final MongoDatabase database, final Batch batch) {
      if (batch instanceof MergeBatch) {
         internalTranslateBatch(database, (MergeBatch) batch);
      } else if (batch instanceof SplitBatch) {
         internalTranslateBatch(database, (SplitBatch) batch);
      }
   }

   private static void internalTranslateBatch(final MongoDatabase database, final MergeBatch batch) {
      final Document doc = new Document();

      if (batch.getMergeType() == MergeBatch.MergeType.JOIN) {
         final List<BsonString> fields = new ArrayList<>();
         batch.getAttributes().forEach(attr -> {
            if (fields.size() > 0) {
               fields.add(new BsonString(batch.getJoin()));
            }
            fields.add(new BsonString("$" + attr));
         });

         //final String fieldsStr = batch.getAttributes().stream().map(attr -> "$" + attr).collect(Collectors.joining(batch.getJoin()));

      } else if (batch.getMergeType() == MergeBatch.MergeType.SUM) {
         /*database.getCollection(batch.getCollectionName()).aggregate(Arrays.asList(
               new Document().append("$addFields",
                     new Document().append(batch.getResultAttribute(),
                           new Document("$sum", batch.getAttributes()))),
               new Document().append("$out", batch.getCollectionName())));*/
      } else {
         final List<Document> fields = new ArrayList<>();
      }

   }

   private static void internalTranslateBatch(final MongoDatabase database, final SplitBatch batch) {
   }

}
