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

package io.lumeer.storage.mongodb.util;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.RoleOld;
import io.lumeer.storage.api.filter.SearchAttributeFilter;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.codecs.PermissionCodec;
import io.lumeer.storage.mongodb.codecs.PermissionsCodec;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MongoFilters {

   private static final String ID = "_id";
   private static final String CODE = "code";
   private static final String NAME = "name";
   private static final String PERMISSIONS = "permissions";

   public static Bson idFilter(String id) {
      return Filters.eq(ID, new ObjectId(id));
   }

   public static Bson idsFilter(java.util.Collection<String> ids) {
      if (ids == null) {
         return null;
      }
      List<ObjectId> objectIds = ids.stream().filter(key -> key != null && ObjectId.isValid(key)).map(ObjectId::new).collect(Collectors.toList());
      if (!objectIds.isEmpty()) {
         return Filters.in(ID, objectIds);
      }
      return null;
   }

   public static Bson codeFilter(String code) {
      return Filters.eq(CODE, code);
   }

   public static Bson nameFilter(String name) {
      return Filters.eq(NAME, name);
   }

   public static Bson permissionsFilter(DatabaseQuery databaseQuery) {
      final List<Bson> filters = databaseQuery.getGroups().stream()
                                              .map(MongoFilters::groupPermissionsFilter)
                                              .collect(Collectors.toList());

      filters.addAll(databaseQuery.getUsers().stream()
                                  .map(MongoFilters::userPermissionsFilter)
                                  .collect(Collectors.toList()));

      return Filters.or(filters);
   }

   private static Bson userPermissionsFilter(String user) {
      return entityPermissionsFilter(PermissionsCodec.USER_ROLES, user);
   }

   private static Bson groupPermissionsFilter(String group) {
      return entityPermissionsFilter(PermissionsCodec.GROUP_ROLES, group);
   }

   private static Bson entityPermissionsFilter(String entityField, String entityName) {
      return Filters.elemMatch(PERMISSIONS + "." + entityField, Filters.and(
            entityNameFilter(entityName),
            entityRolesFilter(RoleOld.READ, RoleOld.MANAGE)
      ));
   }

   private static Bson entityNameFilter(String name) {
      return Filters.eq(PermissionCodec.ID, name);
   }

   private static Bson entityRolesFilter(RoleOld... roles) {
      List<String> rolesStrings = Arrays.stream(roles).map(RoleOld::toString).collect(Collectors.toList());
      return Filters.in(PermissionCodec.ROLES, rolesStrings);
   }

   public static Bson createFilterForFulltexts(java.util.Collection<Attribute> attributes, Set<String> fulltexts) {
      List<Bson> filters = fulltexts.stream().map(fulltext -> createFilterForFulltext(attributes, fulltext))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());

      return filters.size() > 0 ? Filters.and(filters) : null;
   }

   private static Bson createFilterForFulltext(java.util.Collection<Attribute> attributes, String fulltext) {
      List<Attribute> fulltextAttrs = attributes.stream()
                                                .filter(attr -> attr.getName().toLowerCase().contains(fulltext.toLowerCase()))
                                                .collect(Collectors.toList());

      List<Bson> attrFilters = attributes.stream()
                                         .map(attr -> Filters.regex(attr.getId(), Pattern.compile(fulltext, Pattern.CASE_INSENSITIVE)))
                                         .collect(Collectors.toList());

      Bson contentFilter = !attrFilters.isEmpty() ? Filters.or(attrFilters) : null;

      if (fulltextAttrs.size() > 0) { // we search by presence of the matching attributes
         Bson attrNamesFilter = Filters.or(fulltextAttrs.stream().map(attr -> Filters.exists(attr.getId())).collect(Collectors.toList()));
         if (contentFilter != null) {
            return Filters.or(contentFilter, attrNamesFilter);
         }
         return attrNamesFilter;
      }

      return contentFilter;
   }

   public static Bson attributeFilter(SearchAttributeFilter filter) {
      if (filter == null || filter.getConditionType() == null) {
         return null;
      }
      switch (filter.getConditionType()) {
         case EQUALS:
            return Filters.eq(filter.getAttributeId(), filter.getValue());
         case NOT_EQUALS:
            return Filters.ne(filter.getAttributeId(), filter.getValue());
         case LOWER_THAN:
            return Filters.lt(filter.getAttributeId(), filter.getValue());
         case LOWER_THAN_EQUALS:
            return Filters.lte(filter.getAttributeId(), filter.getValue());
         case GREATER_THAN:
            return Filters.gt(filter.getAttributeId(), filter.getValue());
         case GREATER_THAN_EQUALS:
            return Filters.gte(filter.getAttributeId(), filter.getValue());
      }
      return null;
   }

}
