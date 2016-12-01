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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

   /**
    * Number format respecting given locale.
    */
   private NumberFormat numberFormat = NumberFormat.getNumberInstance();
   private NumberFormat integerNumberFormat = NumberFormat.getNumberInstance();

   public NumberConstraintType() {
      integerNumberFormat.setParseIntegerOnly(true);
   }

   @Override
   public Set<String> getRegisteredPrefixes() {
      final Set<String> result = new HashSet<>();
      result.addAll(Arrays.asList(IS_NUMBER, IS_INTEGER, GREATER_THAN, LESS_THAN, "greaterOrEquals", "lessOrEquals", "equals"));

      return result;
   }

   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      switch (config[0]) {
         case IS_NUMBER:
            return new FunctionConstraint((value -> {
               try {
                  numberFormat.parse(value.replaceAll(" ", ""));
                  return true;
               } catch (ParseException pe) {
                  return false;
               }
            }), constraintConfiguration);
         case IS_INTEGER:
            return new FunctionConstraint((value -> {
               try {
                  integerNumberFormat.parse(value.replaceAll(" ", "")).intValue();
                  return true;
               } catch (ParseException pe) {
                  return false;
               }
            }), constraintConfiguration);
         case LESS_THAN:
            final double ltParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint((value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() < ltParam;
               } catch (ParseException pe) {
                  return false;
               }
            }), constraintConfiguration);
         case GREATER_THAN:
            final double gtParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint((value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() > gtParam;
               } catch (ParseException pe) {
                  return false;
               }
            }), constraintConfiguration);
         case GREATER_OR_EQUALS:
            final double gteParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint((value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() >= gteParam;
               } catch (ParseException pe) {
                  return false;
               }
            }), constraintConfiguration);
         case LESS_OR_EQUALS:
            final double lteParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint((value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() <= lteParam;
               } catch (ParseException pe) {
                  return false;
               }
            }), constraintConfiguration);
         case EQUALS:
            final double eqParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint((value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() == eqParam;
               } catch (ParseException pe) {
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

   @Override
   public Set<String> getParameterSuggestions(final String prefix) {
      return Collections.emptySet();
   }

   @Override
   public void setLocale(final Locale locale) {
      numberFormat = NumberFormat.getInstance(locale);
   }
}
