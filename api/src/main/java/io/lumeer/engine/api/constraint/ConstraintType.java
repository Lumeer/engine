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
