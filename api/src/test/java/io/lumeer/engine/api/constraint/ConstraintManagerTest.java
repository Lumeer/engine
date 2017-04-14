package io.lumeer.engine.api.constraint;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConstraintManagerTest {

   private static CaseConstraintType cct = new CaseConstraintType();
   private static DateTimeConstraintType dct = new DateTimeConstraintType();
   private static ListConstraintType lct = new ListConstraintType();
   private static MatchesConstraintType mct = new MatchesConstraintType();
   private static NumberConstraintType nct = new NumberConstraintType();
   final static Locale l = Locale.forLanguageTag("cs_CZ");

   @BeforeClass
   public static void initLocale() {
      cct.setLocale(l);
      dct.setLocale(l);
      lct.setLocale(l);
      mct.setLocale(l);
      nct.setLocale(l);
   }

   @Test
   public void testTypesHandling() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      assertThat(cm.encode("2.34")).isInstanceOf(Double.class);
      assertThat(cm.encode("2")).isInstanceOf(Long.class);
      assertThat(cm.encode("a2")).isInstanceOf(String.class);

      final String longNumber = new BigDecimal(Double.MAX_VALUE).multiply(new BigDecimal(Double.MAX_VALUE)).toString();

      assertThat(cm.encode(longNumber)).isInstanceOf(BigDecimal.class);

      assertThat(longNumber).isEqualTo(cm.encode(longNumber).toString());
   }

   @Test
   public void testCompatibleConstraints() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("case:lower"); // since now we can do only strings

      assertThatThrownBy(() -> {
         cm.registerConstraint("date:yyyy/MM/dd HH:mm:ss");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");

      assertThatThrownBy(() -> {
         cm.registerConstraint("tags:a,b,c,d");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");

      assertThatThrownBy(() -> {
         cm.registerConstraint("isNumber");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");

      cm.registerConstraint("oneOf:a,b,c,d");
      cm.registerConstraint("matches:^abc.*");

      final ConstraintManager cm2 = new ConstraintManager();
      cm2.setLocale(l);
      cm2.registerConstraint("date:yyyy/MM/dd HH:mm:ss");

      assertThatThrownBy(() -> {
         cm2.registerConstraint("tags:a,b,c,d");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");

      assertThatThrownBy(() -> {
         cm2.registerConstraint("isNumber");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");

      assertThatThrownBy(() -> {
         cm2.registerConstraint("oneOf:a,b,c,d");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");

      assertThatThrownBy(() -> {
         cm2.registerConstraint("matches:^abc.*");
      }).isInstanceOf(InvalidConstraintException.class).hasMessageContaining("data types");
   }
}