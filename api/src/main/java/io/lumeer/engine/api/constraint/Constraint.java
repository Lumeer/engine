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

/**
 * A configured instance of a certain {@link ConstraintType} that verifies the concrete value.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Constraint {

   /**
    * Result of the check of a value. The value can be either completely ok, or we know how to make it ok, or it is wrong.
    */
   enum ConstraintResult {
      VALID, FIXABLE, INVALID
   }

   /**
    * Checks whether the value is valid according to this constraint.
    *
    * @param value
    *       The value to check.
    * @return Validation result.
    */
   ConstraintResult isValid(final String value);

   /**
    * Tries to fix the value (supposing {@link #isValid(String)} returned {@link ConstraintResult#FIXABLE}).
    *
    * @param value
    *       The value to fix.
    * @return The fixed value.
    */
   String fix(final String value);

   /**
    * Returns the string representation of the configuration of this constraint.
    *
    * @return The string representation of the configuration of this constraint.
    */
   String getConfigurationString();
}
