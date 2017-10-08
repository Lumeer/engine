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

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Constraint based on asses and fixing {@link Function}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FixingFunctionConstraint extends FunctionConstraint {

   private Function<String, String> fixFunction;

   /**
    * Gets a new instance of constraint that is defined by the provided function.
    *
    * @param assesFunction
    *       Function that returns true for valid values and false otherwise.
    * @param fixFunction
    *       Function that can fix the value. Return null if and only if the value cannot be fixed.
    * @param configuration
    *       Original constraint configuration to be able to throw user friendly exceptions.
    * @param encodeFunction
    *       Function to encode input data types to the form needed for database.
    * @param decodeFunction
    *       Function to decode database data types to user data type.
    * @param encodedTypes
    *       Allowed database data types.
    */
   protected FixingFunctionConstraint(final Function<String, Boolean> assesFunction, final Function<String, String> fixFunction, final String configuration, final BiFunction<Object, Class, Object> encodeFunction, final Function<Object, Object> decodeFunction, final Class... encodedTypes) {
      super(assesFunction, configuration, encodeFunction, decodeFunction, encodedTypes);

      this.fixFunction = fixFunction;
   }

   @Override
   public ConstraintResult isValid(final String value) {
      ConstraintResult result = super.isValid(value);

      if (ConstraintResult.VALID == result) {
         return ConstraintResult.VALID;
      }

      if (fixFunction.apply(value) != null) {
         return ConstraintResult.FIXABLE;
      }

      return ConstraintResult.INVALID;
   }

   @Override
   public String fix(final String value) {
      String result = super.fix(value);

      return result != null ? result : fixFunction.apply(value);
   }
}
