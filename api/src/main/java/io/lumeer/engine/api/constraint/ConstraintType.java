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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Defines a type of constraint that makes sure user data are in the required format.
 * The constraint is typically specified in the form of &lt;constraint type&gt;:&lt;constraint parameter(s)&gt;.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface ConstraintType {

   /**
    * Get configuration prefixes recognized by this constraint type.
    *
    * @return Configuration prefixes recognized by this constraint type.
    */
   Set<String> getRegisteredPrefixes();

   /**
    * Parses constraint configuration.
    *
    * @param constraintConfiguration
    *       Constraint configuration.
    * @return Parsed constraint.
    * @throws InvalidConstraintException
    *       When the configuration was not parseable.
    */
   Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException;

   /**
    * Get the set of possible parameter values for the given constraint prefix (e.g. for prefix case,
    * it can be lower, upper).
    *
    * @param prefix
    *       The prefix to return possible parameters for.
    * @return The set of possible parameter values for the given constraint prefix.
    */
   Set<String> getParameterSuggestions(final String prefix);

   /**
    * Sets locale that needs to be respected by all constraints of this type.
    *
    * @param locale
    *       Locale to set.
    */
   void setLocale(final Locale locale);

   /**
    * Tries to provide constraint of its own type that are valid for all input values.
    *
    * @param values
    *       Values to figure constraints for.
    * @return The suggested constraints.
    */
   default Set<Constraint> suggestConstraints(final List<String> values) {
      return Collections.emptySet();
   }
}
