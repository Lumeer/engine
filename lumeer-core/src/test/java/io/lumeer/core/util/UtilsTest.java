package io.lumeer.core.util;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

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
