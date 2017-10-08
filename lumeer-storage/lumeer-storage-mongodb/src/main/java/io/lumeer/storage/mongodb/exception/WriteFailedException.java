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
package io.lumeer.storage.mongodb.exception;

import io.lumeer.storage.api.exception.StorageException;

import com.mongodb.WriteResult;

public class WriteFailedException extends StorageException {

   private final WriteResult writeResult;

   public WriteFailedException(final WriteResult writeResult) {
      super(writeResult.toString());

      this.writeResult = writeResult;
   }

   public WriteResult getWriteResult() {
      return writeResult;
   }
}
