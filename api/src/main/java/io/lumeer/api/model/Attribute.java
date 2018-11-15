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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Attribute {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String CONSTRAINTS = "constraints";
   public static final String USAGE_COUNT = "usageCount";

   private String id;
   private String name;
   private Set<String> constraints;
   private Integer usageCount;

   public Attribute(final String id) {
      this.id = id;
      this.name = id;
      this.constraints = Collections.emptySet();
      this.usageCount = 0;
   }

   public Attribute(Attribute attribute) {
      this.id = attribute.getId();
      this.name = attribute.getName();
      this.constraints = new HashSet<>(attribute.getConstraints());
      this.usageCount = attribute.getUsageCount();
   }

   @JsonCreator
   public Attribute(
         @JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(CONSTRAINTS) final Set<String> constraints,
         @JsonProperty(USAGE_COUNT) final Integer usageCount) {
      this.name = name;
      this.id = id;
      this.constraints = constraints;
      this.usageCount = usageCount;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public Set<String> getConstraints() {
      return constraints;
   }

   public Integer getUsageCount() {
      return usageCount != null ? usageCount : 0;
   }

   public void setUsageCount(final Integer usageCount) {
      this.usageCount = usageCount;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Attribute)) {
         return false;
      }

      final Attribute that = (Attribute) o;

      return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
   }

   @Override
   public int hashCode() {
      return getId() != null ? getId().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "JsonAttribute{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", constraints=" + constraints +
            ", usageCount=" + usageCount +
            '}';
   }

}
