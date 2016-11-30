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
import java.util.List;
import java.util.function.Function;

/**
 * Various constraints on numbers.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NumberConstraintType implements ConstraintType {

   private static final String IS_NUMBER = "isNumber";
   private static final String IS_INTEGER = "isInteger";
   private static final String GREATER_THAN = "greaterThan";
   private static final String LESS_THAN = "lessThan";
   private static final String GREATER_OR_EQUALS = "greaterOrEquals";
   private static final String LESS_OR_EQUALS = "lessOrEquals";
   private static final String EQUALS = "equals";

   @Override
   public List<String> getRegisteredPrefixes() {
      return Arrays.asList(IS_NUMBER, IS_INTEGER, GREATER_THAN, LESS_THAN, "greaterOrEquals", "lessOrEquals", "equals");
   }

   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      switch (config[0]) {
         case IS_NUMBER:
            return new NumberConstraint((value -> {
               try {
                  Double.parseDouble(value.replaceAll(" ", ""));
                  return true;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         case IS_INTEGER:
            return new NumberConstraint((value -> {
               try {
                  Integer.parseInt(value.replaceAll(" ", ""));
                  return true;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         case LESS_THAN:
            final double ltParam = checkParameter(config, constraintConfiguration);
            return new NumberConstraint((value -> {
               try {
                  return Double.parseDouble(value.replaceAll(" ", "")) < ltParam;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         case GREATER_THAN:
            final double gtParam = checkParameter(config, constraintConfiguration);
            return new NumberConstraint((value -> {
               try {
                  return Double.parseDouble(value.replaceAll(" ", "")) > gtParam;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         case GREATER_OR_EQUALS:
            final double gteParam = checkParameter(config, constraintConfiguration);
            return new NumberConstraint((value -> {
               try {
                  return Double.parseDouble(value.replaceAll(" ", "")) >= gteParam;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         case LESS_OR_EQUALS:
            final double lteParam = checkParameter(config, constraintConfiguration);
            return new NumberConstraint((value -> {
               try {
                  return Double.parseDouble(value.replaceAll(" ", "")) <= lteParam;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         case EQUALS:
            final double eqParam = checkParameter(config, constraintConfiguration);
            return new NumberConstraint((value -> {
               try {
                  return Double.parseDouble(value.replaceAll(" ", "")) == eqParam;
               } catch (NumberFormatException nfe) {
                  return false;
               }
            }), constraintConfiguration);
         default:
            throw new InvalidConstraintException("Unable to parse constraint configuration: " + constraintConfiguration);
      }
   }

   private double checkParameter(final String[] config, final String constraintConfiguration) throws InvalidConstraintException {
      if (config.length < 2) {
         throw new InvalidConstraintException("Missing parameter in constraint configuration: " + constraintConfiguration);
      }
      final double parameter;
      try {
         parameter = Double.parseDouble(config[1]);
      } catch (NumberFormatException nfe) {
         throw new InvalidConstraintException("Invalid parameter value: " + constraintConfiguration, nfe);
      }

      return parameter;
   }

   private static class NumberConstraint implements Constraint {

      private final Function<String, Boolean> assesFunction;

      private final String configuration;

      private NumberConstraint(final Function<String, Boolean> assesFunction, final String configuration) {
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
}
