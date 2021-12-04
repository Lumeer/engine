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
package io.lumeer.core.util.s3;

import java.util.Objects;

public class S3ObjectItem {
   private final String key;
   private final Long size;

   public S3ObjectItem(final String key, final Long size) {
      this.key = key;
      this.size = size;
   }

   public String getKey() {
      return key;
   }

   public Long getSize() {
      return size;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final S3ObjectItem that = (S3ObjectItem) o;
      return Objects.equals(key, that.key) && Objects.equals(size, that.size);
   }

   @Override
   public int hashCode() {
      return Objects.hash(key, size);
   }

   @Override
   public String toString() {
      return "S3ObjectItem{" +
            "key='" + key + '\'' +
            ", size=" + size +
            '}';
   }
}
