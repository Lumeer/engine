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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
                     constraintConfiguration, Coders.getToStringEncodeFunction(), Coders.getAsStringDecodeFunction(), String.class);
            case UPPER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && value.toUpperCase(locale).equals(value),
                     value -> value.toUpperCase(locale),
                     constraintConfiguration, Coders.getToStringEncodeFunction(), Coders.getAsStringDecodeFunction(), String.class);
            case FIRST_LOWER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && (value.substring(0, 1).toLowerCase(locale) + value.substring(1)).equals(value),
                     value -> value.substring(0, 1).toLowerCase(locale) + value.substring(1),
                     constraintConfiguration, Coders.getToStringEncodeFunction(), Coders.getAsStringDecodeFunction(), String.class);
            case FIRST_UPPER_CASE:
               return new FixingFunctionConstraint(
                     value -> value != null && (value.substring(0, 1).toUpperCase(locale) + value.substring(1)).equals(value),
                     value -> value.substring(0, 1).toUpperCase(locale) + value.substring(1),
                     constraintConfiguration, Coders.getToStringEncodeFunction(), Coders.getAsStringDecodeFunction(), String.class);
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

   @Override
   public Set<Constraint> suggestConstraints(final List<String> values) {
      final Set<Constraint> constraints = new HashSet<>();

      if (values.stream().filter(s -> s.toLowerCase(locale).equals(s)).count() == values.size()) {
         try {
            constraints.add(parseConstraint(CONSTRAINT_PREFIX + ":" + LOWER_CASE));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }
      }

      if (values.stream().filter(s -> s.toUpperCase(locale).equals(s)).count() == values.size()) {
         try {
            constraints.add(parseConstraint(CONSTRAINT_PREFIX + ":" + UPPER_CASE));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }
      }

      if (values.stream().filter(s -> s.substring(0, 1).toLowerCase(locale).equals(s.substring(0, 1))).count() == values.size()) {
         try {
            constraints.add(parseConstraint(CONSTRAINT_PREFIX + ":" + FIRST_LOWER_CASE));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }
      }

      if (values.stream().filter(s -> s.substring(0, 1).toUpperCase(locale).equals(s.substring(0, 1))).count() == values.size()) {
         try {
            constraints.add(parseConstraint(CONSTRAINT_PREFIX + ":" + FIRST_UPPER_CASE));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }
      }

      return constraints;
   }
}
