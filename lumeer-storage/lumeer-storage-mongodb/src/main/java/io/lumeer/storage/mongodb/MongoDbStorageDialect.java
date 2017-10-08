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

import static com.mongodb.client.model.Filters.*;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataSort;
import io.lumeer.engine.api.data.DataStorageDialect;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
            .append("query", new DataDocument(LumeerConst.Collection.CODE, collection))
            .append("update", new DataDocument()
                  .append("$rename", new DataDocument(
                        MongoUtils.concatParams(LumeerConst.Collection.ATTRIBUTES, oldName),
                        MongoUtils.concatParams(LumeerConst.Collection.ATTRIBUTES, newName))));
   }

   @Override
   public DataDocument addRecentlyUsedDocumentQuery(final String metadataCollection, final String collection, final String id, final int listSize) {
      return new DataDocument()
            .append("findAndModify", metadataCollection)
            .append("query", new DataDocument(LumeerConst.Collection.CODE, collection))
            .append("update", new DataDocument()
                  .append("$push", new DataDocument()
                        .append(LumeerConst.Collection.RECENTLY_USED_DOCUMENTS, new DataDocument()
                              .append("$each", Collections.singletonList(id))
                              .append("$position", 0)
                              .append("$slice", listSize))));
   }

   @Override
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
   public DataFilter fieldValueWildcardFilter(final String fieldName, final Object valuePart) {
      return createFilter(regex(fieldName, ".*" + valuePart + ".*", "gi"));
   }

   @Override
   public DataFilter fieldValueWildcardFilterOneSided(final String fieldName, final Object valuePart) {
      return createFilter(regex(fieldName, valuePart + ".*", "gi"));
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

   @Override
   public DataFilter combineFilters(final DataFilter... filters) {
      List<Bson> mongoDbFilters = Arrays.stream(filters)
                                        .map(DataFilter::<Bson>get)
                                        .collect(Collectors.toList());
      return createFilter(and(mongoDbFilters));
   }

   @Override
   public DataFilter collectionPermissionsRoleFilter(final String role, final String user, final List<String> groups) {
      List<Bson> groupsFilters = groups.stream()
                                       .map(g -> collectionPermissionRoleFilterHelper(LumeerConst.Security.GROUP_KEY, g, role))
                                       .collect(Collectors.toList());
      groupsFilters.add(collectionPermissionRoleFilterHelper(LumeerConst.Security.USERS_KEY, user, role));
      return createFilter(Filters.or(groupsFilters.toArray(new Bson[groupsFilters.size()])));
   }

   private Bson collectionPermissionRoleFilterHelper(final String key, final String name, final String role) {
      return Filters.elemMatch(concatFields(LumeerConst.Security.PERMISSIONS_KEY, key), Filters.and(Filters.eq(LumeerConst.Security.USERGROUP_NAME_KEY, name), Filters.in(LumeerConst.Security.USERGROUP_ROLES_KEY, role)));
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
