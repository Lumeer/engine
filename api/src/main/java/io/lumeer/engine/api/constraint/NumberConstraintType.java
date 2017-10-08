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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

/**
 * Various constraints on numbers.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class NumberConstraintType implements ConstraintType {

   private static final String IS_NUMBER = "isNumber";
   private static final String IS_INTEGER = "isInteger";
   private static final String IS_MONETARY = "isMonetary";
   private static final String GREATER_THAN = "greaterThan";
   private static final String LESS_THAN = "lessThan";
   private static final String GREATER_OR_EQUALS = "greaterOrEquals";
   private static final String LESS_OR_EQUALS = "lessOrEquals";
   private static final String EQUALS = "equals";

   /**
    * Number format respecting given locale.
    */
   private NumberFormat numberFormat = NumberFormat.getNumberInstance();
   private NumberFormat bigNumberFormat = NumberFormat.getNumberInstance();
   private NumberFormat integerNumberFormat = NumberFormat.getIntegerInstance();
   private NumberFormat currencyNumberFormat = NumberFormat.getNumberInstance();

   public NumberConstraintType() {
      ((DecimalFormat) currencyNumberFormat).setParseBigDecimal(true);
      ((DecimalFormat) bigNumberFormat).setParseBigDecimal(true);
   }

   @Override
   public Set<String> getRegisteredPrefixes() {
      final Set<String> result = new HashSet<>();
      result.addAll(Arrays.asList(IS_NUMBER, IS_INTEGER, IS_MONETARY, GREATER_THAN, LESS_THAN, GREATER_OR_EQUALS, LESS_OR_EQUALS, EQUALS));

      return result;
   }

   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      switch (config[0]) {
         case IS_NUMBER:
            return new FunctionConstraint(value -> {
               try {
                  numberFormat.parse(value.replaceAll(" ", ""));
                  return true;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(numberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
         case IS_INTEGER:
            return new FunctionConstraint(value -> {
               try {
                  integerNumberFormat.parse(value.replaceAll(" ", "")).intValue();
                  return true;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(integerNumberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
         case IS_MONETARY:
            return new FunctionConstraint(value -> {
               try {
                  currencyNumberFormat.parse(value.replaceAll(" ", ""));
                  return true;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(currencyNumberFormat, null), Coders.getIdentityDecodeFunction(), Number.class);
         case LESS_THAN:
            final double ltParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint(value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() < ltParam;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(numberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
         case GREATER_THAN:
            final double gtParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint(value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() > gtParam;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(numberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
         case GREATER_OR_EQUALS:
            final double gteParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint(value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() >= gteParam;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(numberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
         case LESS_OR_EQUALS:
            final double lteParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint(value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() <= lteParam;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(numberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
         case EQUALS:
            final double eqParam = checkParameter(config, constraintConfiguration);
            return new FunctionConstraint(value -> {
               try {
                  return numberFormat.parse(value.replaceAll(" ", "")).doubleValue() == eqParam;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration, Coders.getNumberEncodeFunction(numberFormat, bigNumberFormat), Coders.getIdentityDecodeFunction(), Number.class);
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
      integerNumberFormat = NumberFormat.getIntegerInstance(locale);
      currencyNumberFormat = NumberFormat.getNumberInstance(locale);
      ((DecimalFormat) currencyNumberFormat).setParseBigDecimal(true);
      bigNumberFormat = NumberFormat.getNumberInstance(locale);
      ((DecimalFormat) bigNumberFormat).setParseBigDecimal(true);
   }

   @Override
   public Set<Constraint> suggestConstraints(final List<String> values) {
      final Set<Constraint> constraints = new HashSet<>();
      final LongAdder numbers = new LongAdder();
      final LongAdder integers = new LongAdder();
      final Map<String, Double> stats = new HashMap<>();
      stats.put("min", Double.MAX_VALUE);
      stats.put("max", -Double.MAX_VALUE);

      values.forEach(s -> {
         try {
            double d = numberFormat.parse(s.replaceAll(" ", "")).doubleValue();
            numbers.increment();
            stats.put("min", Math.min(d, stats.get("min")));
            stats.put("max", Math.min(d, stats.get("max")));
         } catch (ParseException pe) {
            // nps
         }

         try {
            integerNumberFormat.parse(s.replaceAll(" ", "")).intValue();
            integers.increment();
         } catch (ParseException pe) {
            // nps
         }
      });

      if (numbers.intValue() == values.size()) {
         try {
            constraints.add(parseConstraint(IS_NUMBER));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }

         try {
            constraints.add(parseConstraint(IS_MONETARY));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }

         if (integers.intValue() == values.size()) {
            try {
               constraints.add(parseConstraint(IS_INTEGER));
            } catch (InvalidConstraintException ice) {
               // we just don't suggest the wrong constraint
            }
         }

         try {
            constraints.add(parseConstraint(LESS_OR_EQUALS + ":" + (stats.get("max"))));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }

         try {
            constraints.add(parseConstraint(GREATER_OR_EQUALS + ":" + (stats.get("min"))));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }

         if ((double) stats.get("min") == stats.get("max")) {
            try {
               constraints.add(parseConstraint(EQUALS + ":" + (stats.get("min"))));
            } catch (InvalidConstraintException ice) {
               // we just don't suggest the wrong constraint
            }
         }
      }

      return constraints;
   }
}
