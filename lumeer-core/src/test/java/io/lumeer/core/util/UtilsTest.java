package io.lumeer.core.util;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class UtilsTest {

   @Test
   public void isCodeSafe() {
      assertThat(Utils.isCodeSafe("abc")).isTrue();
      assertThat(Utils.isCodeSafe("abc/das")).isFalse();
   }
}