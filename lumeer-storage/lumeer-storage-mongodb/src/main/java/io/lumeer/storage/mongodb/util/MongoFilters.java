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

package io.lumeer.storage.mongodb.util;

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.codecs.PermissionCodec;
import io.lumeer.storage.mongodb.codecs.PermissionsCodec;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MongoFilters {

   private static final String ID = "_id";
   private static final String CODE = "code";
   private static final String PERMISSIONS = "permissions";

   public static Bson idFilter(String id) {
      return Filters.eq(ID, new ObjectId(id));
   }

   public static Bson paymentIdFilter(final String paymentId) {
      return Filters.eq(Payment.PAYMENT_ID, paymentId);
   }

   public static Bson paymentStateFilter(final int stateId) {
      return Filters.eq(Payment.STATE, stateId);
   }

   public static Bson paymentValidUntilFilter(final Date date) {
      return Filters.gte(Payment.VALID_UNTIL, date);
   }

   public static Bson paymentStartFilter(final Date date) {
      return Filters.lte(Payment.START, date);
   }

   public static Bson companyOrganizationIdFilter(final String organizationId) {
      return Filters.eq(CompanyContact.ORGANIZATION_ID, organizationId);
   }

   public static Bson codeFilter(String code) {
      return Filters.eq(CODE, code);
   }

   public static Bson permissionsFilter(DatabaseQuery databaseQuery) {
      List<Bson> filters = databaseQuery.getGroups().stream()
                                        .map(MongoFilters::groupPermissionsFilter)
                                        .collect(Collectors.toList());
      filters.add(userPermissionsFilter(databaseQuery.getUser()));

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
            entityRolesFilter(Role.READ)
      ));
   }

   private static Bson entityNameFilter(String name) {
      return Filters.eq(PermissionCodec.ID, name);
   }

   private static Bson entityRolesFilter(Role role) {
      return Filters.in(PermissionCodec.ROLES, role.toString());
   }

   public static Bson suggestionsFilter(SuggestionQuery query) {
      return Filters.and(
            Filters.text(query.getText()),
            MongoFilters.permissionsFilter(query)
      );
   }

}
