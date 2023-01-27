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

public class FunctionRow {

   private String id;
   private String resourceId;
   private FunctionResourceType type;
   private String attributeId;
   private String dependentCollectionId;
   private String dependentLinkTypeId;
   private String dependentAttributeId;

   public static FunctionRow createForCollection(final String resourceId, final String attributeId, final String dependentCollectionId, final String dependentLinkTypeId, final String dependentAttributeId){
      return new FunctionRow(null, resourceId, FunctionResourceType.COLLECTION, attributeId, dependentCollectionId, dependentLinkTypeId, dependentAttributeId);
   }

   public static FunctionRow createForLink(final String resourceId, final String attributeId, final String dependentCollectionId, final String dependentLinkTypeId, final String dependentAttributeId){
      return new FunctionRow(null, resourceId, FunctionResourceType.LINK, attributeId, dependentCollectionId, dependentLinkTypeId, dependentAttributeId);
   }

   public FunctionRow(final String id, final String resourceId, final FunctionResourceType type, final String attributeId, final String dependentCollectionId, final String dependentLinkTypeId, final String dependentAttributeId) {
      this.id = id;
      this.resourceId = resourceId;
      this.type = type;
      this.attributeId = attributeId;
      this.dependentCollectionId = dependentCollectionId;
      this.dependentLinkTypeId = dependentLinkTypeId;
      this.dependentAttributeId = dependentAttributeId;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getResourceId() {
      return resourceId;
   }

   public FunctionResourceType getType() {
      return type;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public String getDependentCollectionId() {
      return dependentCollectionId;
   }

   public String getDependentLinkTypeId() {
      return dependentLinkTypeId;
   }

   public String getDependentAttributeId() {
      return dependentAttributeId;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof FunctionRow)) {
         return false;
      }
      final FunctionRow that = (FunctionRow) o;
      return Objects.equals(getResourceId(), that.getResourceId()) &&
            getType() == that.getType() &&
            Objects.equals(getAttributeId(), that.getAttributeId()) &&
            Objects.equals(getDependentCollectionId(), that.getDependentCollectionId()) &&
            Objects.equals(getDependentLinkTypeId(), that.getDependentLinkTypeId()) &&
            Objects.equals(getDependentAttributeId(), that.getDependentAttributeId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getResourceId(), getType(), getAttributeId(), getDependentCollectionId(), getDependentLinkTypeId(), getDependentAttributeId());
   }

   @Override
   public String toString() {
      return "FunctionRow2{" +
            "resourceId='" + resourceId + '\'' +
            ", type=" + type +
            ", attributeId='" + attributeId + '\'' +
            ", dependentCollectionId='" + dependentCollectionId + '\'' +
            ", dependentLinkTypeId='" + dependentLinkTypeId + '\'' +
            ", dependentAttributeId='" + dependentAttributeId + '\'' +
            '}';
   }
}
