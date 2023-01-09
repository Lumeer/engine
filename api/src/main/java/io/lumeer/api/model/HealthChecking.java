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

import io.lumeer.api.exception.InsaneObjectException;

import java.util.List;

/**
 * Classes with this interface can automatically check their correctness (attribute lengths, content etc.)
 */
public interface HealthChecking {

   long MAX_STRING_LENGTH = 512;
   long MAX_LONG_STRING_LENGTH = 10240;
   long MAX_MESSAGE_SIZE = 10L * 1024 * 1024;

   /**
    * Checks object health.
    *
    * @throws InsaneObjectException When thee object is not healthy.
    */
   void checkHealth() throws InsaneObjectException;

   default void checkStringLength(final String name, final String value, final long maxLength) throws InsaneObjectException {
      if (value != null && value.length() > maxLength) {
         throw new InsaneObjectException(String.format("Value of %s is longer than %d.", name, maxLength));
      }
   }

   default void checkIllegalCharacters(final String name, final String value, final List<Character> invalidChars) throws InsaneObjectException {
      if (value != null) {
         for (char letter : value.toCharArray()) {
            if (invalidChars.contains(letter)) {
               throw new InsaneObjectException(String.format("Value of %s contains illegal character %c.", name, letter));
            }
         }
      }
   }
}
