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
      // https://get.lumeer.io/en/w/LUMEER/LMR/view/table?q=eyJzIjpbeyJjIjoiNWQ3NTdmOWFmOGMwMDY2YzI1YWM0OWY5In1dfQe2faa9ce&c=eyJjIjoiNWQ3NTdmOWFmOGMwMDY2YzI1YWM0OWY5IiwiZCI6IjVkNzU3ZmY4ZjhjMDA2NmMyNWFjNGEwMyIsImEiOiJhMSJ937e18e18
      System.out.println(Utils.decodeQueryParam("eyJjIjoiNWQ3NTdmOWFmOGMwMDY2YzI1YWM0OWY5IiwiZCI6IjVkNzU3ZmY4ZjhjMDA2NmMyNWFjNGEwMyIsImEiOiJhMSJ937e18e18"));
      assertThat(Utils.encodeQueryParam("{\"s\":[{\"c\":\"5d24b3632ec57b390456ed06\"}]}"))
            .isEqualTo("eyJzIjpbeyJjIjoiNWQyNGIzNjMyZWM1N2IzOTA0NTZlZDA2In1dfQc809164d");
      assertThat(Utils.decodeQueryParam("eyJzIjpbeyJjIjoiNWQyNGIzNjMyZWM1N2IzOTA0NTZlZDA2In1dfQc809164d"))
            .isEqualTo("{\"s\":[{\"c\":\"5d24b3632ec57b390456ed06\"}]}");
   }
}
