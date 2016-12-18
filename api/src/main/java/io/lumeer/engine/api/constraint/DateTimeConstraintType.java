/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows use of any date time format pattern as specified by {@link SimpleDateFormat}.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DateTimeConstraintType implements ConstraintType {

   final static private Set<String> SUGGESTIONS = new HashSet<>(Arrays.asList("yyyy/MM/dd HH:mm:ss", "HH:mm:ss", "HH:mm", "yyyy/MM/dd", "MM/dd",
         "yyyy.MM.dd G HH:mm:ss z", "EEE, MMM d, ''yy", "h:mm a", "yyyy.MMMMM.dd GGG hh:mm aaa", "EEE, d MMM yyyy HH:mm:ss Z",
         "yyMMddHHmmssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "YYYY-'W'ww-u"));
   final static private Set<String> REGISTERED = new HashSet<>(Arrays.asList("date", "dateTime", "time"));

   private Locale locale;

   @Override
   public Set<String> getRegisteredPrefixes() {
      return REGISTERED;
   }

   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      if (config.length == 2) {
         try {
            final DateFormat format = new SimpleDateFormat(config[1], locale);

            return new FunctionConstraint(value -> {
               try {
                  format.parse(value.trim());
                  return true;
               } catch (ParseException pe) {
                  return false;
               }
            }, constraintConfiguration);
         } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidConstraintException("Invalid pattern for '" + config[0] + "' constraint: " + config[1], e);
         }

      }

      throw new InvalidConstraintException("Missing pattern parameter for '" + config[0] + "' constraint: " + constraintConfiguration);
   }

   @Override
   public Set<String> getParameterSuggestions(final String prefix) {
      if (prefix == null || prefix.isEmpty()) {
         return SUGGESTIONS;
      }

      return SUGGESTIONS.stream().filter(s -> s.toLowerCase(locale).startsWith(prefix.toLowerCase(locale))).collect(Collectors.toSet());
   }

   @Override
   public void setLocale(final Locale locale) {
      this.locale = locale;
   }

   @Override
   public Set<Constraint> suggestConstraints(final List<String> values) {
      final Set<Constraint> constraints = new HashSet<>();

      SUGGESTIONS.forEach(suggestion -> {
         try {
            final Constraint c = parseConstraint("date:" + suggestion);

            long passed = values.stream().filter(val -> c.isValid(val) == Constraint.ConstraintResult.VALID).count();

            if (passed == values.size()) {
               constraints.add(c);
            }
         } catch (InvalidConstraintException ice) {
            // so we skip it, who cares...
         }
      });

      return constraints;
   }
}
