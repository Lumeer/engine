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
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConstraintManager {

   private List<Constraint> constraints;

   public ConstraintManager(final List<String> constraintConfigurations) {
      constraints = parseConstraints(constraintConfigurations);
   }

   public List<String> getConstraintConfigurations() {
      return getConstraintConfigurations(constraints);
   }

   public Constraint.ConstraintResult isValid(final String value) {
      Constraint.ConstraintResult result = Constraint.ConstraintResult.VALID;

      for (final Constraint constraint: constraints) {
         Constraint.ConstraintResult r = constraint.isValid(value);

         // can make the result only worse
         if (r.ordinal() > result.ordinal()) {
            result = r;
         }
      }

      return result;
   }

   public String fix(String value) {
      return tryToFix(value).orElse(null);
   }

   private Optional<String> tryToFix(final String value) {
      // TODO: recursive way of finding out a way through the constraints
      // to arrive to a fixed value that is accepted by all constraints

      return null;
   }

   static List<Constraint> parseConstraints(final List<String> constraintConfigurations) {
      return null;
   }

   List<String> getConstraintConfigurations(final List<Constraint> constraints) {
      return null;
   }
}
