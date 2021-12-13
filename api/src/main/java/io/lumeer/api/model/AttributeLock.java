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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class AttributeLock {

   private final List<AttributeLockExceptionGroup> exceptionGroups;
   private final boolean locked;

   @JsonCreator
   public AttributeLock(@JsonProperty("exceptionGroups") final List<AttributeLockExceptionGroup> exceptionGroups,
         @JsonProperty("locked") final boolean locked) {
      this.exceptionGroups = exceptionGroups;
      this.locked = locked;
   }

   public List<AttributeLockExceptionGroup> getExceptionGroups() {
      return exceptionGroups;
   }

   public boolean getLocked() {
      return locked;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeLock)) {
         return false;
      }
      final AttributeLock that = (AttributeLock) o;
      return Objects.equals(exceptionGroups, that.exceptionGroups) && Objects.equals(locked, that.locked);
   }

   @Override
   public int hashCode() {
      return Objects.hash(exceptionGroups, locked);
   }

   @Override
   public String toString() {
      return "AttributeLock{" +
            "exceptionGroups=" + exceptionGroups +
            ", locked=" + locked +
            '}';
   }
}
