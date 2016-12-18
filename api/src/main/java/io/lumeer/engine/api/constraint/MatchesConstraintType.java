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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
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
            return new FunctionConstraint(value -> pattern.matcher(value).matches(), constraintConfiguration);
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
