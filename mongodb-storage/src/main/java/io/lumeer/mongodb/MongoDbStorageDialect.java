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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataSort;
import io.lumeer.engine.api.data.DataStorageDialect;

import com.mongodb.client.model.Sorts;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class MongoDbStorageDialect implements DataStorageDialect {

   @Override
   public DataDocument renameAttributeQuery(final String metadataCollection, final String collection, final String oldName, final String newName) {
      return new DataDocument()
            .append("findAndModify", metadataCollection)
            .append("query", new DataDocument(LumeerConst.Collection.INTERNAL_NAME_KEY, collection))
            .append("update", new DataDocument()
                  .append("$rename", new DataDocument(
                        MongoUtils.concatParams(LumeerConst.Collection.ATTRIBUTES_KEY, oldName),
                        MongoUtils.concatParams(LumeerConst.Collection.ATTRIBUTES_KEY, newName))));
   }

   @Override
   public DataDocument addRecentlyUsedDocumentQuery(final String metadataCollection, final String collection, final String id, final int listSize) {
      return new DataDocument()
            .append("findAndModify", metadataCollection)
            .append("query", new DataDocument(LumeerConst.Collection.INTERNAL_NAME_KEY, collection))
            .append("update", new DataDocument()
                  .append("$push", new DataDocument()
                        .append(LumeerConst.Collection.RECENTLY_USED_DOCUMENTS_KEY, new DataDocument()
                              .append("$each", Collections.singletonList(id))
                              .append("$position", 0)
                              .append("$slice", listSize))));
   }

   public DataDocument[] usersOfGroupAggregate(final String organization, final String group) {
      return new DataDocument[] { new DataDocument("$match", new DataDocument(LumeerConst.UserGroup.ATTR_ORG_ID, organization)),
            new DataDocument("$unwind", "$" + LumeerConst.UserGroup.ATTR_USERS),
            new DataDocument("$match", new DataDocument(concatFields(LumeerConst.UserGroup.ATTR_USERS, LumeerConst.UserGroup.ATTR_USERS_GROUPS), group)),
            new DataDocument("$project", new DataDocument(LumeerConst.UserGroup.ATTR_USERS_USER, concatFields("$" + LumeerConst.UserGroup.ATTR_USERS, LumeerConst.UserGroup.ATTR_USERS_USER))) };
   }

   private DataFilter createFilter(final Bson filter) {
      return new MongoDbDataFilter(filter);
   }

   @Override
   public DataFilter fieldValueFilter(final String fieldName, final Object value) {
      return createFilter(eq(fieldName, value));
   }

   @Override
   public DataFilter documentFilter(final String documentFilter) {
      return createFilter(BsonDocument.parse(documentFilter));
   }

   @Override
   public DataFilter documentNestedIdFilter(final String documentId) {
      return fieldValueFilter(concatFields(LumeerConst.Document.ID, LumeerConst.Document.ID), new ObjectId(documentId));
   }

   @Override
   public DataFilter documentNestedIdFilterWithVersion(final String documentId, final int version) {
      return createFilter(eq(LumeerConst.Document.ID, and(eq(LumeerConst.Document.ID, new ObjectId(documentId)), eq(LumeerConst.Document.METADATA_VERSION_KEY, version))));
   }

   @Override
   public DataFilter documentIdFilter(final String documentId) {
      return fieldValueFilter(LumeerConst.Document.ID, new ObjectId(documentId));
   }

   @Override
   public DataFilter multipleFieldsValueFilter(final Map<String, Object> fields) {
      List<Bson> bsons = new ArrayList<>();
      fields.entrySet().forEach(e -> bsons.add(eq(e.getKey(), e.getValue())));
      return createFilter(and(bsons));
   }

   private DataSort createSort(final Bson sort) {
      return new MongoDbDataSort(sort);
   }

   @Override
   public DataSort documentSort(final String documentSort) {
      return createSort(BsonDocument.parse(documentSort));
   }

   @Override
   public DataSort documentFieldSort(final String fieldName, final int sortOrder) {
      if (sortOrder == LumeerConst.SORT_ASCENDING_ORDER) {
         return createSort(Sorts.ascending(fieldName));
      }

      if (sortOrder == LumeerConst.SORT_DESCENDING_ORDER) {
         return createSort(Sorts.descending(fieldName));
      }

      return null;
   }

   @Override
   public String concatFields(final String... fields) {
      if (fields.length == 0) {
         return "";
      }
      String field = fields[0];
      for (int i = 1; i < fields.length; i++) {
         field += "." + fields[i];
      }
      return field;
   }
}
