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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ListConstraintType implements ConstraintType {

   private static final String ONE_OF_TYPE = "oneOf";
   private static final String TAGS_TYPE = "tags";

   private Locale locale = Locale.getDefault();

   @Override
   public Set<String> getRegisteredPrefixes() {
      return new HashSet<>(Arrays.asList(ONE_OF_TYPE, TAGS_TYPE));
   }

   private BiFunction<Object, Class, Object> getEncodeFunction(final boolean oneOf) {
      return (o, t) -> {
         if (oneOf) {
            if (t == null || t == String.class) {
               return o.toString();
            }
         } else {
            if (t == null || String[].class.isAssignableFrom(t)) {
               return Arrays.asList(o.toString().split(",")).stream()
                            .map(String::trim)
                            .collect(Collectors.toList());
            }
         }

         return null;
      };
   }

   private Function<Object, Object> getDecodeFunction() {
      return o -> {
         if (o instanceof String[]) {
            final String tags =  Arrays.toString((String[]) o);

            if ("null".equals(tags)) {
               return "";
            }

            return tags.substring(1, tags.length() - 1); // trim [ and ]
         } else if (o instanceof List) {
            final String tags = o.toString();

            return tags.substring(1, tags.length() - 1); // trim [ and ]
         }

         return o.toString();
      };
   }


   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      if (config.length == 2) {
         final Set<String> options = new HashSet<>();
         final Set<String> optionsLowerCase = new HashSet<>();

         Arrays.asList(config[1].split(",")).stream().map(String::trim).forEach(o -> {
            options.add(o);
            optionsLowerCase.add(o.toLowerCase(locale));
         });

         switch (config[0]) {
            case ONE_OF_TYPE:
               return new FixingFunctionConstraint(value -> options.contains(value), value -> {
                  // find the option with correct case
                  if (optionsLowerCase.contains(value.toLowerCase(locale))) {
                     for (final String opt : options) {
                        if (opt.toLowerCase(locale).equals(value.toLowerCase(locale))) {
                           return opt;
                        }
                     }
                  }

                  return null;
               }, constraintConfiguration, getEncodeFunction(true), getDecodeFunction(), String.class);
            case TAGS_TYPE:
               return new FixingFunctionConstraint(
                     value -> options.containsAll(Arrays.asList(value.split(",")).stream().map(String::trim).collect(Collectors.toSet())),
                     value -> {
                        // filter all tags and find their matches with correct case
                        final Set<String> tags = Arrays.asList(value.split(",")).stream().map(String::trim).collect(Collectors.toSet());
                        final Set<String> tagsLowerCase = tags.stream().map(s -> s.toLowerCase(locale)).collect(Collectors.toSet());
                        final Set<String> result = new HashSet<>();

                        if (optionsLowerCase.containsAll(tagsLowerCase)) {
                           for (final String tag : tags) {
                              boolean found = false;

                              for (final String opt : options) {
                                 if (opt.toLowerCase(locale).equals(tag.toLowerCase(locale))) {
                                    found = true;
                                    result.add(opt);
                                    break;
                                 }
                              }

                              if (!found) {
                                 return null;
                              }
                           }
                        }

                        return result.stream().collect(Collectors.joining(", "));
                     },
                     constraintConfiguration, getEncodeFunction(false), getDecodeFunction(), String[].class);
            default:
               throw new InvalidConstraintException("Unsupported parameter value for constraint: " + config[1]);
         }
      } else {
         throw new InvalidConstraintException("Invalid constraint configuration (expected case:<parameter>): " + constraintConfiguration);
      }
   }

   @Override
   public Set<String> getParameterSuggestions(final String prefix) {
      return null;
   }

   @Override
   public void setLocale(final Locale locale) {
      this.locale = locale;
   }

   @Override
   public Set<Constraint> suggestConstraints(final List<String> values) {
      final Set<Constraint> constraints = new HashSet<>();
      final Set<String> distinct = values.stream().collect(Collectors.toSet());

      if (distinct.size() < 10) {
         try {
            constraints.add(parseConstraint(ONE_OF_TYPE + ":" + String.join(",", distinct)));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }
         try {
            constraints.add(parseConstraint(TAGS_TYPE + ":" + String.join(",", distinct)));
         } catch (InvalidConstraintException ice) {
            // we just don't suggest the wrong constraint
         }
      }

      return constraints;
   }
}
