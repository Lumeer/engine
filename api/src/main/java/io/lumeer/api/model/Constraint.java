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

import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@Immutable
public class Constraint {

   public static final String TYPE = "type";
   public static final String CONFIG = "config";

   private ConstraintType type;
   private Object config;

   @JsonCreator
   public Constraint(
         @JsonProperty(TYPE) final ConstraintType type,
         @JsonProperty(CONFIG) final Object config) {
      this.type = type;
      this.config = config;
   }

   public ConstraintType getType() {
      return type;
   }

   public Object getConfig() {
      return config;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Constraint that = (Constraint) o;
      return type == that.type &&
            Objects.equals(config, that.config);
   }

   @Override
   public int hashCode() {
      return Objects.hash(type, config);
   }

   @Override
   public String toString() {
      return "Constraint{" +
            "type=" + type +
            ", config=" + config +
            '}';
   }
}
