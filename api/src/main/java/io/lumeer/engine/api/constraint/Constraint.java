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

import java.util.Set;

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

   /**
    * Translates the value to be stored in the database.
    *
    * @param value
    *       The value to be translated.
    * @param preferredType
    *       The requested return type, it must be one of the types returned by {@link #getEncodedTypes()}.
    *       Can be null when there is no preference.
    * @return The value with the correct type to be stored.
    */
   Object encode(final Object value, final Class preferredType);

   /**
    * Renders the object value from database to the presentation layer.
    * This can be Date conversion to the desired user format as string.
    *
    * @param value
    *       The value as read from the database.
    * @return The value in the form to be sent to the user.
    */
   Object decode(final Object value);

   /**
    * Gets a set of compatible types that can be used to store the values validated with this constraint.
    * Only constraint with overlap in these sets are compatible.
    *
    * @return Set of types that can be used to store data in the database.
    */
   Set<Class> getEncodedTypes();
}
