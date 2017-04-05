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
                              .append("$each", Arrays.asList(id))
                              .append("$position", 0)
                              .append("$slice", listSize))));
   }

   private DataFilter createFilter(final Bson filter) {
      return new MongoDbDataFilter(filter);
   }

   @Override
   public DataFilter linkingFromTablesColNameFilter(final String collectionName, final String role) {
      Bson filterRaw = role == null || role.isEmpty()
            ? eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName)
            : and(eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return createFilter(filterRaw);
   }

   @Override
   public DataFilter linkingFromTablesFilter(final String firstCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String collParam = linkDirection == LumeerConst.Linking.LinkDirection.FROM ? LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION : LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION;
      Bson filterRaw = role == null || role.isEmpty()
            ? eq(collParam, firstCollectionName)
            : and(eq(collParam, firstCollectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return createFilter(filterRaw);
   }

   @Override
   public DataFilter linkingFromToTablesFilter(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String fromCollectionName;
      String toCollectionName;
      if (linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         fromCollectionName = firstCollectionName;
         toCollectionName = secondCollectionName;
      } else {
         fromCollectionName = secondCollectionName;
         toCollectionName = firstCollectionName;
      }
      Bson filterRaw = role == null
            ? and(eq(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName))
            : and(eq(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return createFilter(filterRaw);
   }

   @Override
   public DataFilter linkingFromToDocumentFilter(final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      Bson filterRaw = linkDirection == LumeerConst.Linking.LinkDirection.FROM
            ? and(eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId),
            eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, toId))
            : and(eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, toId),
            eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, fromId)
      );
      return createFilter(filterRaw);
   }

   @Override
   public DataFilter linkingFromDocumentFilter(final String fromId, final LumeerConst.Linking.LinkDirection linkDirection) {
      Bson filterRaw = linkDirection == LumeerConst.Linking.LinkDirection.FROM
            ? eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId)
            : eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, fromId);
      return createFilter(filterRaw);
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
