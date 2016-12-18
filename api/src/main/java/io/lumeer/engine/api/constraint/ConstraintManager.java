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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds a list of constraints that can be obtained from a list of string configurations.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConstraintManager {

   /**
    * Locale that will be passed to all constraints.
    */
   private Locale locale;

   /**
    * Configured constraints.
    */
   private List<Constraint> constraints;

   /**
    * Registry of constraint types.
    */
   private Map<String, ConstraintType> registry = new HashMap<>();

   /**
    * List of all constraint type classes.
    */
   private static final ConstraintType[] CONSTRAINT_CLASSES = {
         new NumberConstraintType(), new CaseConstraintType(), new ListConstraintType(), new MatchesConstraintType(), new DateTimeConstraintType()
   };

   /**
    * Initializes an empty constraint manager.
    *
    * @throws InvalidConstraintException
    *       When there are multiple constraints asking to be registered with the same configuration prefix.
    */
   public ConstraintManager() throws InvalidConstraintException {
      final List<String> collisions = new ArrayList<>();

      for (final ConstraintType type : CONSTRAINT_CLASSES) {
         type.getRegisteredPrefixes().forEach(prefix -> {
            if (registry.containsKey(prefix)) {
               collisions.add(prefix);
            } else {
               registry.put(prefix, type);
            }
         });
      }

      if (collisions.size() > 0) {
         throw new InvalidConstraintException("Multiple constraint types found for the following configuration prefixes: " + String.join(", ", collisions));
      }
   }

   /**
    * Creates a new manager with the constraints whose configurations are provided.
    *
    * @param constraintConfigurations
    *       Configurations of constraints.
    * @throws InvalidConstraintException
    *       When it was not possible to parse constraint configuration.
    */
   public ConstraintManager(final List<String> constraintConfigurations) throws InvalidConstraintException {
      this();
      constraints = parseConstraints(constraintConfigurations);
   }

   /**
    * Registers another constraint.
    *
    * @param constraint
    *       Constraint to be registered.
    */
   public void registerConstraint(final Constraint constraint) {
      constraints.add(constraint);
   }

   /**
    * Registers another constraint based on provided constraint configuration.
    *
    * @param constraintConfiguration
    *       The constraint configuration.
    * @throws InvalidConstraintException
    *       When it was not possible to parse constraint configuration.
    */
   public void registerConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      constraints.addAll(parseConstraints(Collections.singletonList(constraintConfiguration)));
   }

   /**
    * Gets list of configurations of current constraints.
    *
    * @return The list of configurations of current constraints.
    */
   public List<String> getConstraintConfigurations() {
      return getConstraintConfigurations(constraints);
   }

   /**
    * Gets constraint configurations based on the provided constraints.
    *
    * @param constraints
    *       Constraints to get configurations for.
    * @return The constraint configurations.
    */
   public List<String> getConstraintConfigurations(final List<Constraint> constraints) {
      if (constraints == null) {
         return Collections.emptyList();
      }

      return constraints.stream().map(Constraint::getConfigurationString).collect(Collectors.toList());
   }

   /**
    * Validates the given value with all constraints.
    *
    * @param value
    *       The value to validate.
    * @return Validation result.
    */
   public Constraint.ConstraintResult isValid(final String value) {
      Constraint.ConstraintResult result = Constraint.ConstraintResult.VALID;

      for (final Constraint constraint : constraints) {
         Constraint.ConstraintResult r = constraint.isValid(value);

         // can make the result only worse
         if (r.ordinal() > result.ordinal()) {
            result = r;
         }
      }

      return result;
   }

   /**
    * Tries to fix the value so that all constraints return {@link io.lumeer.engine.api.constraint.Constraint.ConstraintResult#VALID}.
    *
    * @param value
    *       The value to fix.
    * @return The fixed value or null when it was not possible to fix the value so that all constraint are met.
    */
   public String fix(String value) {
      return tryToFix(new HashSet<>(), value);
   }

   /**
    * Internal helper that tries to fix the value.
    *
    * @param used
    *       Constraints that were already tried to fix the value.
    * @param value
    *       The value to fix.
    * @return The fixed value or null when it was not possible to fix the value so that all constraint are met.
    */
   private String tryToFix(final Set<Constraint> used, final String value) {
      for (final Constraint c : constraints) {
         if (!used.contains(c)) {
            Constraint.ConstraintResult r = c.isValid(value);
            if (r == Constraint.ConstraintResult.INVALID) { // no way of moving forward
               return null;
            } else if (r == Constraint.ConstraintResult.FIXABLE) { // apply the fix
               final String fixed = c.fix(value);

               used.add(c);

               if (fixed != null) {
                  return tryToFix(used, c.fix(value)); // try the next round
               } else {
                  return null;
               }
            }
         }
      }

      return value;
   }

   /**
    * Parses constraint configurations.
    *
    * @param constraintConfigurations
    *       Constraint configurations to parse.
    * @return Newly created constraints.
    * @throws InvalidConstraintException
    *       When it was not possible to parse constraint configuration.
    */
   public List<Constraint> parseConstraints(final List<String> constraintConfigurations) throws InvalidConstraintException {
      final List<Constraint> constraints = new ArrayList<>();
      final List<String> invalidConfigurations = new ArrayList<>();

      constraintConfigurations.forEach(configuration -> {
         final String[] config = configuration.split(":");

         if (!registry.containsKey(config[0])) {
            invalidConfigurations.add(configuration);
         } else {
            try {
               constraints.add(registry.get(config[0]).parseConstraint(configuration));
            } catch (InvalidConstraintException e) {
               invalidConfigurations.add(configuration);
            }
         }
      });

      if (invalidConfigurations.size() > 0) {
         throw new InvalidConstraintException("The following constraint configurations are not recognized: " + String.join(", ", invalidConfigurations));
      }

      return constraints;
   }

   /**
    * Gets the set of possible constrain prefixes (e.g. lessThan, case, matches...).
    *
    * @return The set of possible constrain prefixes.
    */
   public Set<String> getRegisteredPrefixes() {
      return registry.keySet();
   }

   /**
    * Get the set of possible parameter values for the given constraint prefix (e.g. for prefix case,
    * it can be lower, upper).
    *
    * @param prefix
    *       The prefix to return possible parameters for.
    * @return The set of possible parameter values for the given constraint prefix.
    */
   public Set<String> getConstraintParameterSuggestions(final String prefix) {
      final ConstraintType constraintType = registry.get(prefix);
      return constraintType == null ? Collections.emptySet() : constraintType.getParameterSuggestions(prefix);
   }

   /**
    * Sets the user's locale.
    *
    * @return The user's locale.
    */
   public Locale getLocale() {
      return locale;
   }

   /**
    * Gets the currently used locale.
    *
    * @param locale
    *       The currently used locale.
    */
   public void setLocale(final Locale locale) {
      this.locale = locale;
      Arrays.asList(CONSTRAINT_CLASSES).forEach(ct -> ct.setLocale(locale));
   }
}
