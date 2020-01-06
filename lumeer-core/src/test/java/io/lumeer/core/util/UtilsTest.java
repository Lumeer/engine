package io.lumeer.core.util;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UtilsTest {

   @Test
   public void isCodeSafe() {
      assertThat(Utils.isCodeSafe("abc")).isTrue();
      assertThat(Utils.isCodeSafe("abc/das")).isFalse();
   }
}
