/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.api.constraint;

import java.util.function.Function;

/**
 * Constraint based on asses and fixing {@link Function}>
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
    */
   protected FixingFunctionConstraint(final Function<String, Boolean> assesFunction, final Function<String, String> fixFunction, final String configuration) {
      super(assesFunction, configuration);

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
