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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.api.data.DataDocument;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class GroovyExecutorTest {

   @Test
   public void testDirectExecution() {
      final Map<String, Object> vars = new HashMap<>();
      vars.put("i", 4);
      vars.put("name", "Pepa");

      final Object result = GroovyExecutor.evaluateScript("name = \"Hello $name\"; return name + ': ' + (++i)", vars);

      assertThat(result).isEqualTo("Hello Pepa: 5");
      assertThat(vars).hasSize(2);
      assertThat(vars).containsKey("name");
      assertThat(vars.get("name").toString()).isEqualTo("Hello Pepa");
      assertThat(vars).containsEntry("i", 5);
   }

   @Test
   public void testPreCompiledScript() {
      final Map<String, Object> vars = new HashMap<>();
      vars.put("i", 4);
      vars.put("name", "Pepa");

      final GroovyExecutor ge = new GroovyExecutor("def brandNew = 4.5d; name = \"Hello $name\"; return name + ': ' + (++i)");

      // multiple executions continue to add to variables
      ge.run(vars);
      Object result = ge.run(vars);

      assertThat(result).isEqualTo("Hello Hello Pepa: 6");
      assertThat(vars).hasSize(2);
      assertThat(vars).containsKey("name");
      assertThat(vars.get("name").toString()).isEqualTo("Hello Hello Pepa");
      assertThat(vars).containsEntry("i", 6);

      //now we switch variables completely and it still works
      final Map<String, Object> vars2 = new HashMap<>();
      vars2.put("i", 42);
      vars2.put("name", "Liberát");

      result = ge.run(vars2);

      assertThat(result).isEqualTo("Hello Liberát: 43");
      assertThat(vars2).hasSize(2);
      assertThat(vars2).containsKey("name");
      assertThat(vars2.get("name").toString()).isEqualTo("Hello Liberát");
      assertThat(vars2).containsEntry("i", 43);
   }

   @Test
   public void passingDataDocumentTest() {
      final Map<String, Object> vars = new HashMap<>();
      vars.put("dd", new DataDocument());

      // verifies that we can pass our own objects in and out
      GroovyExecutor.evaluateScript("dd.put('myDbl', 3.14157d)", vars);

      assertThat(((DataDocument) vars.get("dd")).getDouble("myDbl")).isEqualTo(3.14157d);
   }
}