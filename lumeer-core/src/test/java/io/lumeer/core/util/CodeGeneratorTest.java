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
