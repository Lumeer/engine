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
package io.lumeer.engine.api.constraint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Constraint based on any {@link java.util.function.Function} provided in constructor.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FunctionConstraint implements Constraint {

   private final Function<String, Boolean> assesFunction;

   private final String configuration;

   private final BiFunction<Object, Class, Object> encodeFunction;

   private final Function<Object, Object> decodeFunction;

   private final Set<Class> encodedTypes;

   /**
    * Gets a new instance of constraint that is defined by the provided function.
    *
    * @param assesFunction
    *       Function that returns true for valid values and false otherwise.
    * @param configuration
    *       Original constraint configuration to be able to throw user friendly exceptions.
    * @param encodeFunction
    *       Function to encode input data types to the form needed for database.
    * @param decodeFunction
    *       Function to decode database data types to user data type.
    * @param encodedTypes
    *       Allowed database data types.
    */
   protected FunctionConstraint(final Function<String, Boolean> assesFunction, final String configuration, final BiFunction<Object, Class, Object> encodeFunction, final Function<Object, Object> decodeFunction, final Class... encodedTypes) {
      this.assesFunction = assesFunction;
      this.configuration = configuration;
      this.encodeFunction = encodeFunction;
      this.decodeFunction = decodeFunction;
      this.encodedTypes = new HashSet<>(Arrays.asList(encodedTypes));
   }

   @Override
   public ConstraintResult isValid(final String value) {
      return assesFunction.apply(value) ? ConstraintResult.VALID : ConstraintResult.INVALID;
   }

   @Override
   public String fix(final String value) {
      return isValid(value) == ConstraintResult.VALID ? value : null;
   }

   @Override
   public String getConfigurationString() {
      return configuration;
   }

   @Override
   public Object encode(final Object value, final Class preferredType) {
      return encodeFunction.apply(value, preferredType);
   }

   @Override
   public Object decode(final Object value) {
      return decodeFunction.apply(value);
   }

   @Override
   public Set<Class> getEncodedTypes() {
      return encodedTypes;
   }
}
