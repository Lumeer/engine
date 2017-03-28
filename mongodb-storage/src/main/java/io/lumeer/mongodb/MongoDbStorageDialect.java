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
import static io.lumeer.mongodb.MongoUtils.convertBsonToJson;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorageDialect;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class MongoDbStorageDialect implements DataStorageDialect {

   @Override
   public DataDocument updateCollectionAttributeCountQuery(final String metadataCollectionName, final String attributeName) {
      return new DataDocument(); // TODO based on new query in CollectionMetadataFacade
   }

   @Override
   public String linkingFromTablesColNameFilter(final String collectionName, final String role) {
      Bson filterRaw = role == null || role.isEmpty()
            ? eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName)
            : and(eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return convertBsonToJson(filterRaw);
   }

   @Override
   public String linkingFromTablesFilter(final String firstCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String collParam = linkDirection == LumeerConst.Linking.LinkDirection.FROM ? LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION : LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION;
      Bson filterRaw = role == null || role.isEmpty()
            ? eq(collParam, firstCollectionName)
            : and(eq(collParam, firstCollectionName),
            eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return convertBsonToJson(filterRaw);
   }

   @Override
   public String linkingFromToTablesFilter(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
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
      return convertBsonToJson(filterRaw);
   }

   @Override
   public String linkingFromToDocumentFilter(final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      Bson filterRaw = linkDirection == LumeerConst.Linking.LinkDirection.FROM
            ? and(eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId),
            eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, toId))
            : and(eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, toId),
            eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, fromId)
      );
      return convertBsonToJson(filterRaw);
   }

   @Override
   public String linkingFromDocumentFilter(final String fromId, final LumeerConst.Linking.LinkDirection linkDirection) {
      Bson filterRaw = linkDirection == LumeerConst.Linking.LinkDirection.FROM
            ? eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId)
            : eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, fromId);
      return convertBsonToJson(filterRaw);
   }

   @Override
   public String fieldValueFilter(final String fieldName, final Object value) {
      return convertBsonToJson(eq(fieldName, value));
   }

   @Override
   public String documentNestedIdFilter(final String documentId) {
      return fieldValueFilter(concatFields(LumeerConst.Document.ID, LumeerConst.Document.ID), new ObjectId(documentId));
   }

   @Override
   public String documentNestedIdFilterWithVersion(final String documentId, final int version) {
      return convertBsonToJson(eq(LumeerConst.Document.ID, and(eq(LumeerConst.Document.ID, new ObjectId(documentId)), eq(LumeerConst.Document.METADATA_VERSION_KEY, version))));
   }

   @Override
   public String documentIdFilter(final String documentId) {
      return fieldValueFilter(LumeerConst.Document.ID, new ObjectId(documentId));
   }

   @Override
   public String multipleFieldsValueFilter(final Map<String, Object> fields) {
      List<Bson> bsons = new ArrayList<>();
      fields.entrySet().forEach(e -> bsons.add(eq(e.getKey(), e.getValue())));
      return convertBsonToJson(and(bsons));
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
