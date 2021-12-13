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
import io.lumeer.api.model.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public class Attribute implements HealthChecking, Updatable<Attribute> {

   public static final List<Character> ILLEGAL_CHARS = List.of('.');

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String CONSTRAINT = "constraint";
   public static final String LOCK = "lock";
   public static final String FUNCTION = "function";
   public static final String DESCRIPTION = "description";
   public static final String USAGE_COUNT = "usageCount";

   private String id;
   private String name;
   private String description;
   private Constraint constraint;
   private AttributeLock lock;
   private Function function;
   private Integer usageCount;

   public Attribute(final String id) {
      this.id = id;
      this.name = id;
      this.usageCount = 0;
   }

   @JsonCreator
   public Attribute(
         @JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(CONSTRAINT) final Constraint constraint,
         @JsonProperty(LOCK) final AttributeLock lock,
         @JsonProperty(FUNCTION) final Function function,
         @JsonProperty(USAGE_COUNT) final Integer usageCount) {
      this.name = name;
      this.id = id;
      this.description = description;
      this.constraint = constraint;
      this.lock = lock;
      this.function = function;
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

   public Constraint getConstraint() {
      return constraint;
   }

   public void setConstraint(final Constraint constraint) {
      this.constraint = constraint;
   }

   public Function getFunction() {
      return function;
   }

   public void setFunction(final Function function) {
      this.function = function;
   }

   @JsonIgnore
   public boolean isFunctionDefined() {
      return getFunction() != null && getFunction().getJs() != null && !getFunction().getJs().isEmpty();
   }

   public Integer getUsageCount() {
      return usageCount != null ? usageCount : 0;
   }

   public void setUsageCount(final Integer usageCount) {
      this.usageCount = usageCount;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public AttributeLock getLock() {
      return lock;
   }

   public void setLock(final AttributeLock lock) {
      this.lock = lock;
   }

   public Attribute copy() {
      return new Attribute(getId(), getName(), getDescription(), getConstraint(), getLock(), getFunction(), getUsageCount());
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
      return "Attribute{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", constraint=" + constraint +
            ", function=" + function +
            ", usageCount=" + usageCount +
            '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("name", name, MAX_STRING_LENGTH);
      checkStringLength("description", description, MAX_LONG_STRING_LENGTH);
      checkIllegalCharacters("name", name, ILLEGAL_CHARS);
   }

   @Override
   public void patch(final Attribute resource, final Set<RoleType> roles) {
      if (roles.contains(RoleType.AttributeEdit)) {
         setName(resource.getName());
         setDescription(resource.getDescription());
         setConstraint(resource.getConstraint());
         setLock(resource.getLock());
      }
      if (roles.contains(RoleType.TechConfig)) {
         setFunction(resource.getFunction());
      }
   }

   public void patchCreation(final Set<RoleType> roles) {
      if (!roles.contains(RoleType.TechConfig)) {
         setFunction(null);
      }
      setUsageCount(0);
   }
}
