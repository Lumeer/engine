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
package io.lumeer.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CodeGeneratorTest {

   private static final String REGEX = "([a-z0-9])+";

   private static final String TOKEN = "token";
   private static final String TOKEN0_CODE = "3c469e9d6c5875d3";
   private static final String TOKEN1_CODE = "677578fa480df7da";
   private static final String TOKEN2_CODE = "df3e6b0bb66ceaad";

   @Test
   public void testGenerateEmptyExistingCodes() {
      String code = CodeGenerator.generate(Collections.emptySet(), TOKEN);
      assertCode(code, TOKEN0_CODE);
   }

   @Test
   public void testGenerate() {
      String code = CodeGenerator.generate(Collections.emptySet(), TOKEN);
      assertCode(code, TOKEN0_CODE);
   }

   @Test
   public void testGenerateExisting() {
      Set<String> existingCodes = new HashSet<>(Arrays.asList(TOKEN0_CODE, TOKEN1_CODE));
      String code = CodeGenerator.generate(existingCodes, TOKEN);
      assertCode(code, TOKEN2_CODE);
   }

   private static void assertCode(String actualCode, String expectedCode) {
      assertThat(actualCode).isNotNull()
                            .matches(REGEX)
                            .hasSize(CodeGenerator.LENGTH)
                            .isEqualTo(expectedCode);
   }

}
