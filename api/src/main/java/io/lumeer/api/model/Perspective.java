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

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Perspective {
   Search("search"),
   Table("table"),
   Calendar("calendar"),
   Chart("chart"),
   Detail("detail"),
   Kanban("kanban"),
   Map("map"),
   Pivot("pivot"),
   GanttChart("ganttChart"),
   Workflow("workflow"),
   Form("form");

   private final String value;

   Perspective(String value) {
      this.value = value;
   }

   @JsonValue
   public String getValue() {
      return value;
   }

   public static Perspective fromString(String perspective) {
      if (perspective == null) {
         return null;
      }
      try {
         return Arrays.stream(values()).filter(role -> role.toString().equalsIgnoreCase(perspective)).findFirst().orElse(null);
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   @Override
   public String toString() {
      return value;
   }
}
