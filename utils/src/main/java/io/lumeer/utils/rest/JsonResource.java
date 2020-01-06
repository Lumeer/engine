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
package io.lumeer.utils.rest;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonResource implements Comparable<JsonResource> {

   private static final int DEFAULT_ORDER = 99_999;

   private final int order;

   private final File path;

   public JsonResource(final String fileName) {
      this.path = new File(fileName);

      int o = DEFAULT_ORDER;
      final Matcher m = Pattern.compile("[0-9]+").matcher(fileName);
      if (m.find()) {
         try {
            o = Integer.parseInt(m.group());
         } catch (NumberFormatException nfe) {
         }
      }

      this.order = o;
   }

   public int getOrder() {
      return order;
   }

   public File getPath() {
      return path;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final JsonResource that = (JsonResource) o;

      if (order != that.order) {
         return false;
      }
      return path != null ? path.equals(that.path) : that.path == null;
   }

   @Override
   public int hashCode() {
      int result = order;
      result = 31 * result + (path != null ? path.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "JsonResource{" +
            "order=" + order +
            ", path=" + path +
            '}';
   }

   @Override
   public int compareTo(final JsonResource o) {
      return this.order - o.order;
   }
}
