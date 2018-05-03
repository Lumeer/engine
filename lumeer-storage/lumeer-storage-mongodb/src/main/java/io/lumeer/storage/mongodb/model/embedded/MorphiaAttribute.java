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
package io.lumeer.storage.mongodb.model.embedded;

import io.lumeer.api.model.Attribute;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Embedded
public class MorphiaAttribute implements Attribute {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String CONSTRAINTS = "constraints";
   public static final String USAGE_COUNT = "usageCount";

   @Property(ID)
   private String id;

   @Property(NAME)
   private String name;

   @Property(CONSTRAINTS)
   private Set<String> constraints;

   @Property(USAGE_COUNT)
   private Integer usageCount;

   public MorphiaAttribute() {
   }

   public MorphiaAttribute(Attribute attribute) {
      this.id = attribute.getId();
      this.name = attribute.getName();
      this.constraints = new HashSet<>(attribute.getConstraints());
      this.usageCount = attribute.getUsageCount();
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public void setName(final String name) {
      this.name = name;
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void setId(final String id) {
      this.id = id;
   }

   @Override
   public Set<String> getConstraints() {
      return constraints;
   }

   @Override
   public Integer getUsageCount() {
      return usageCount != null ? usageCount : 0;
   }

   @Override
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
      return "MongoAttribute{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", constraints=" + constraints +
            ", usageCount=" + usageCount +
            '}';
   }

   public static MorphiaAttribute convert(Attribute attribute) {
      return attribute instanceof MorphiaAttribute ? (MorphiaAttribute) attribute : new MorphiaAttribute(attribute);
   }

   public static Set<MorphiaAttribute> convert(Set<Attribute> attributes) {
      return attributes.stream()
                       .map(MorphiaAttribute::convert)
                       .collect(Collectors.toSet());
   }
}
