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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * RegEx matching constraint.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MatchesConstraintType implements ConstraintType {

   private static final String MATCHES_CONSTRAINT = "matches";

   @Override
   public Set<String> getRegisteredPrefixes() {
      return Collections.singleton(MATCHES_CONSTRAINT);
   }

   @Override
   public Constraint parseConstraint(final String constraintConfiguration) throws InvalidConstraintException {
      final String[] config = constraintConfiguration.split(":", 2);

      if (config.length == 2) {
         try {
            final Pattern pattern = Pattern.compile(config[1]);
            return new FunctionConstraint(value -> pattern.matcher(value).matches(),
                  constraintConfiguration, Coders.getToStringEncodeFunction(),
                  Coders.getAsStringDecodeFunction(), String.class);
         } catch (PatternSyntaxException pse) {
            throw new InvalidConstraintException("Invalid pattern for 'match' constraint: " + config[1], pse);
         }

      }

      throw new InvalidConstraintException("Missing pattern parameter for 'match' constraint: " + constraintConfiguration);
   }

   @Override
   public Set<String> getParameterSuggestions(final String prefix) {
      return Collections.emptySet();
   }

   @Override
   public void setLocale(final Locale locale) {
      // nop
   }
}
