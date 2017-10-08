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
