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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
               }, constraintConfiguration, Coders.getTagsEncodeFunction(true), Coders.getTagsDecodeFunction(), String.class);
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
                     constraintConfiguration, Coders.getTagsEncodeFunction(false), Coders.getTagsDecodeFunction(), String[].class);
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
