/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.util;

import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Executes provided Groovy script. Can be used directly using the static method {@link #evaluateScript(String, Map)},
 * or can reuse a pre-parsed script (due to performance reasons).
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class GroovyExecutor {

   /**
    * Pre-parsed script.
    */
   private final Script script;

   /**
    * Pre-parses the provided script for later execution.
    *
    * @param scriptText
    *       The Groovy script text to parse.
    */
   public GroovyExecutor(final String scriptText) {
      final GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), new Binding());
      script = shell.parse(scriptText);
   }

   /**
    * Runs the pre-parsed Groovy script.
    *
    * @param variables
    *       Variables to pass to the script. Will also carry any modified variables.
    * @return The return value of the Groovy script.
    */
   @SuppressWarnings("unchecked")
   public Object run(final Map<String, Object> variables) {
      final Binding binding = script.getBinding();
      binding.getVariables().clear();
      binding.getVariables().putAll(variables);

      final Object result = script.run();
      variables.putAll(binding.getVariables());

      return result;
   }

   /**
    * Immediately parses and executes the provided Groovy script.
    *
    * @param scriptText
    *       The Groovy script to run.
    * @param vars
    *       Variables to pass to the script. Will also carry any modified variables.
    * @return The return value of the Groovy script.
    */
   public static Object evaluateScript(final String scriptText, final Map<String, Object> vars) {
      final Binding binding = new Binding(vars);
      final GroovyShell shell = new GroovyShell(GroovyExecutor.class.getClassLoader(), binding);
      final Script script = shell.parse(scriptText);

      return script.run();
   }
}
