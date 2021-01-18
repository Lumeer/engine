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
package io.lumeer.api.model;

public enum ConditionType {

   EQUALS("eq"),
   NOT_EQUALS("neq"),
   LOWER_THAN("lt"),
   LOWER_THAN_EQUALS("lte"),
   GREATER_THAN("gt"),
   GREATER_THAN_EQUALS("gte"),
   IN("in"),
   HAS_SOME("hasSome"),
   HAS_ALL("hasAll"),
   HAS_NONE_OF("nin"),
   BETWEEN("between"),
   NOT_BETWEEN("notBetween"),
   CONTAINS("contains"),
   NOT_CONTAINS("notContains"),
   STARTS_WITH("startsWith"),
   ENDS_WITH("endsWith"),
   IS_EMPTY("empty"),
   NOT_EMPTY("notEmpty"),
   ENABLED("enabled"),
   DISABLED("disabled");

   private final String value;

   ConditionType(String value){
      this.value = value;
   }

   public String getValue() {
      return value;
   }

   public static ConditionType fromString(String condition) {
      return ConditionType.valueOf(condition);
   }

}
