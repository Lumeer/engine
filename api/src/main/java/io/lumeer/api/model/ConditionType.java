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
package io.lumeer.api.model;

import java.util.Arrays;
import java.util.List;

public enum ConditionType {

   EQUALS,
   NOT_EQUALS,
   LOWER_THAN,
   LOWER_THAN_EQUALS,
   GREATER_THAN,
   GREATER_THAN_EQUALS;

   private static final List<String> EQ_VARIANTS = Arrays.asList("=", "==", "eq", "equals");
   private static final List<String> NEQ_VARIANTS = Arrays.asList("!=", "!==", "<>", "ne", "neq", "nequals");
   private static final List<String> LT_VARIANTS = Arrays.asList("<", "lt");
   private static final List<String> LTE_VARIANTS = Arrays.asList("<=", "lte");
   private static final List<String> GT_VARIANTS = Arrays.asList(">", "gt");
   private static final List<String> GTE_VARIANTS = Arrays.asList(">=", "gte");

   public static ConditionType fromString(String condition) {
      if (EQ_VARIANTS.contains(condition)) {
         return EQUALS;
      } else if (NEQ_VARIANTS.contains(condition)) {
         return NOT_EQUALS;
      } else if (LT_VARIANTS.contains(condition)) {
         return LOWER_THAN;
      } else if (LTE_VARIANTS.contains(condition)) {
         return LOWER_THAN_EQUALS;
      } else if (GT_VARIANTS.contains(condition)) {
         return GREATER_THAN;
      } else if (GTE_VARIANTS.contains(condition)) {
         return GREATER_THAN_EQUALS;
      }
      return null;
   }

}
