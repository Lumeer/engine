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

import static com.mongodb.client.model.Filters.*;

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
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoDbStorageDialect implements DataStorageDialect {

   private static final String DOCUMENT_ID = "_id";

   private DataFilter createFilter(final Bson filter) {
      return new MongoDbDataFilter(filter);
   }

   @Override
   public DataFilter fieldValueFilter(final String fieldName, final Object value) {
      return createFilter(eq(fieldName, value));
   }

   @Override
   public DataFilter documentIdFilter(final String documentId) {
      return fieldValueFilter(DOCUMENT_ID, new ObjectId(documentId));
   }

   @Override
   public DataFilter multipleFieldsValueFilter(final Map<String, Object> fields) {
      List<Bson> bsons = new ArrayList<>();
      fields.entrySet().forEach(e -> bsons.add(eq(e.getKey(), e.getValue())));
      return createFilter(and(bsons));
   }

   @Override
   public DataFilter combineFilters(final DataFilter... filters) {
      List<Bson> mongoDbFilters = Arrays.stream(filters)
                                        .map(DataFilter::<Bson>get)
                                        .collect(Collectors.toList());
      return createFilter(and(mongoDbFilters));
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
      if (sortOrder == 1) {
         return createSort(Sorts.ascending(fieldName));
      }

      if (sortOrder == -1) {
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
