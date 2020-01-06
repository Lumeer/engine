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
package io.lumeer.api.model.function;

import java.util.Objects;

public class FunctionParameter {

   private final FunctionResourceType type;
   private final String resourceId;
   private final String attributeId;

   public FunctionParameter(final FunctionResourceType type, final String resourceId, final String attributeId) {
      this.type = type;
      this.resourceId = resourceId;
      this.attributeId = attributeId;
   }

   public FunctionResourceType getType() {
      return type;
   }

   public String getResourceId() {
      return resourceId;
   }

   public String getAttributeId() {
      return attributeId;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final FunctionParameter that = (FunctionParameter) o;
      return type == that.type &&
            Objects.equals(resourceId, that.resourceId) &&
            Objects.equals(attributeId, that.attributeId);
   }

   @Override
   public int hashCode() {
      return Objects.hash(type, resourceId, attributeId);
   }

   @Override
   public String toString() {
      return "FunctionParameter{" +
            "type=" + type +
            ", resourceId='" + resourceId + '\'' +
            ", attributeId='" + attributeId + '\'' +
            '}';
   }
}
