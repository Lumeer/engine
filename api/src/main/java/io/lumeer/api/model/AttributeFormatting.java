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

public class AttributeFormatting {

   private final List<AttributeFormattingGroup> groups;

   @JsonCreator
   public AttributeFormatting(@JsonProperty("groups") final List<AttributeFormattingGroup> groups) {
      this.groups = groups;
   }

   public List<AttributeFormattingGroup> getGroups() {
      return groups;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeFormatting)) {
         return false;
      }
      final AttributeFormatting that = (AttributeFormatting) o;
      return Objects.equals(groups, that.groups);
   }

   @Override
   public int hashCode() {
      return Objects.hash(groups);
   }

   @Override
   public String toString() {
      return "AttributeFormatting{" +
            "groups=" + groups +
            '}';
   }
}
