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
package io.lumeer.core.task.executor.operation.data;

import java.util.Arrays;
import java.util.Objects;

public class FileAttachmentData {

   final byte[] data;
   final String fileName;
   final boolean overwriteExisting;

   public FileAttachmentData(final byte[] data, final String fileName, final boolean overwriteExisting) {
      this.data = data;
      this.fileName = fileName;
      this.overwriteExisting = overwriteExisting;
   }

   public byte[] getData() {
      return data;
   }

   public String getFileName() {
      return fileName;
   }

   public boolean isOverwriteExisting() {
      return overwriteExisting;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final FileAttachmentData that = (FileAttachmentData) o;
      return overwriteExisting == that.overwriteExisting && Arrays.equals(data, that.data) && Objects.equals(fileName, that.fileName);
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(fileName, overwriteExisting);
      result = 31 * result + Arrays.hashCode(data);
      return result;
   }

   @Override
   public String toString() {
      return "FileAttachmentData{" +
            "data size=" + (data != null ? data.length : 0) +
            ", fileName='" + fileName + '\'' +
            ", overwriteExisting=" + overwriteExisting +
            '}';
   }
}
