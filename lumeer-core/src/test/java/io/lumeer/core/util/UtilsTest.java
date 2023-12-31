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

import org.junit.jupiter.api.Test;

public class UtilsTest {

   @Test
   public void isCodeSafe() {
      assertThat(Utils.isCodeSafe("abc")).isTrue();
      assertThat(Utils.isCodeSafe("abc/das")).isFalse();
   }

   @Test
   public void testQueryParamEncoding() {
      assertThat(Utils.encodeQueryParam("{\"s\":[{\"c\":\"5d24b3632ec57b390456ed06\"}]}"))
            .isEqualTo("eyJzIjpbeyJjIjoiNWQyNGIzNjMyZWM1N2IzOTA0NTZlZDA2In1dfQc809164d");
      assertThat(Utils.decodeQueryParam("eyJzIjpbeyJjIjoiNWQyNGIzNjMyZWM1N2IzOTA0NTZlZDA2In1dfQc809164d"))
            .isEqualTo("{\"s\":[{\"c\":\"5d24b3632ec57b390456ed06\"}]}");
   }
}
