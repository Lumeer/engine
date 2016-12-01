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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Makes sure the value is of the given case.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CaseConstraintType implements ConstraintType {

   private static final String CONSTRAINT_PREFIX = "case";

   private static final String LOWER_CASE = "lower";
   private static final String UPPER_CASE = "upper";
   private static final String FIRST_UPPER_CASE = "firstUpper";
   private static final String FIRST_LOWER_CASE = "firstLower";

   private Locale locale;

   @Override
   public Set<String> getRegisteredPrefixes() {
      return Collections.singleton(CONSTRAINT_PREFIX);
   }

   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      if (config.length == 2 && CONSTRAINT_PREFIX.equals(config[0])) {
         switch (config[1]) {
            case LOWER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && value.toLowerCase(locale).equals(value),
                     value -> value.toLowerCase(locale),
                     constraintConfiguration);
            case UPPER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && value.toUpperCase(locale).equals(value),
                     value -> value.toUpperCase(locale),
                     constraintConfiguration);
            case FIRST_LOWER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && (value.substring(0, 1).toLowerCase(locale) + value.substring(1)).equals(value),
                     value -> value.substring(0, 1).toLowerCase(locale) + value.substring(1),
                     constraintConfiguration);
            case FIRST_UPPER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && (value.substring(0, 1).toUpperCase(locale) + value.substring(1)).equals(value),
                     value -> value.substring(0, 1).toUpperCase(locale) + value.substring(1),
                     constraintConfiguration);
            default:
               throw new InvalidConstraintException("Unsupported parameter value for constraint 'case': " + config[1]);
         }
      } else {
         throw new InvalidConstraintException("Invalid constraint configuration (expected case:<parameter>): " + constraintConfiguration);
      }
   }

   @Override
   public Set<String> getParameterSuggestions(final String prefix) {
      final Set<String> result = new HashSet<>();

      if (CONSTRAINT_PREFIX.equals(prefix)) {
         result.addAll(Arrays.asList(LOWER_CASE, UPPER_CASE, FIRST_LOWER_CASE, FIRST_UPPER_CASE));
      }

      return result;
   }

   @Override
   public void setLocale(final Locale locale) {
      this.locale = locale;
   }
}
