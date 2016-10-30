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
