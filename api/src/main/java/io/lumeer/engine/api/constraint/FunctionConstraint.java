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
 * Constraint based on any {@link java.util.function.Function} provided in constructor.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FunctionConstraint implements Constraint {

   private final Function<String, Boolean> assesFunction;

   private final String configuration;

   /**
    * Gets a new instance of constraint that is defined by the provided function.
    *
    * @param assesFunction
    *       Function that returns true for valid values and false otherwise.
    * @param configuration
    *       Original constraint configuration to be able to throw user friendly exceptions.
    */
   protected FunctionConstraint(final Function<String, Boolean> assesFunction, final String configuration) {
      this.assesFunction = assesFunction;
      this.configuration = configuration;
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

}
