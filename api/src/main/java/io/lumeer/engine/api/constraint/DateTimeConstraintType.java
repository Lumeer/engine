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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

   private static final Set<String> SUGGESTIONS = new HashSet<>(Arrays.asList("yyyy/MM/dd HH:mm:ss", "HH:mm:ss", "HH:mm", "yyyy/MM/dd", "MM/dd",
         "yyyy.MM.dd G HH:mm:ss z", "EEE, MMM d, ''yy", "h:mm a", "yyyy.MMMMM.dd GGG hh:mm aaa", "EEE, d MMM yyyy HH:mm:ss Z",
         "yyMMddHHmmssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "YYYY-'W'ww-u"));
   private static final Set<String> REGISTERED = new HashSet<>(Arrays.asList("date", "dateTime", "time"));

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
            }, constraintConfiguration, Coders.getDateEncodeFunction(format), Coders.getDateDecodeFunction(format), Date.class);
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
