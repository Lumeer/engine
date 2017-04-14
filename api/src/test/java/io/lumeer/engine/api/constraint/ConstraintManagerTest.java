package io.lumeer.engine.api.constraint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConstraintManagerTest {

   final static Locale l = Locale.forLanguageTag("cs_CZ");

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
   public void testCaseConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("case:lower");

      final String fixable = "Ahoj";
      final String ok = "ahoj";

      assertThat(cm.isValid(fixable)).isEqualTo(Constraint.ConstraintResult.FIXABLE);
      assertThat(cm.isValid(ok)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(fixable)).isEqualTo(ok);

      final Object encoded = cm.encode(cm.fix(fixable));
      assertThat(encoded).isInstanceOf(String.class);
      assertThat(encoded).isEqualTo(ok);
   }

   @Test
   public void testDateTimeConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("date:yyyy-MM-dd'T'HH:mm:ss.SSSZ");

      final Date d = new Date(1234567890);
      final String valid = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", l).format(d);

      assertThat(cm.isValid(valid)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(valid)).isEqualTo(valid);

      final Object encoded = cm.encode(cm.fix(valid));
      assertThat(encoded).isInstanceOf(Date.class);
      assertThat(encoded).isEqualTo(d);
      assertThat(cm.decode(encoded)).isEqualTo(valid);
   }

   @Test
   public void testListTagsConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("tags:java,mongodb,jee,angular,lumeer");

      final List<String> l = Arrays.asList("mongodb", "lumeer");
      final String fixable = "MongoDb, Lumeer";
      final String valid = "lumeer, mongodb";

      assertThat(cm.isValid(fixable)).isEqualTo(Constraint.ConstraintResult.FIXABLE);
      assertThat(cm.isValid(valid)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(fixable)).isEqualTo(valid);

      final Object encoded = cm.encode(cm.fix(fixable));
      assertThat(encoded).isInstanceOf(List.class);
      assertThat((List) encoded).containsAll(l);
      assertThat(cm.decode(encoded)).isEqualTo(valid);
   }

   @Test
   public void testListOneOfConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("oneOf:java,mongodb,jee,angular,lumeer");

      final List<String> l = Arrays.asList("lumeer");
      final String fixable = "Lumeer";
      final String valid = "lumeer";
      final String invalid = "lumeer, mongodb";

      assertThat(cm.isValid(invalid)).isEqualTo(Constraint.ConstraintResult.INVALID);
      assertThat(cm.isValid(fixable)).isEqualTo(Constraint.ConstraintResult.FIXABLE);
      assertThat(cm.isValid(valid)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(fixable)).isEqualTo(valid);

      final Object encoded = cm.encode(cm.fix(fixable));
      assertThat(encoded).isInstanceOf(String.class);
      assertThat(encoded).isEqualTo(valid);
      assertThat(cm.decode(encoded)).isEqualTo(valid);
   }

   @Test
   public void testMatchesfConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("matches:^abc.*");

      final String valid = "abcdef";

      assertThat(cm.isValid("ABCdef")).isEqualTo(Constraint.ConstraintResult.INVALID);
      assertThat(cm.isValid("aabcd")).isEqualTo(Constraint.ConstraintResult.INVALID);
      assertThat(cm.isValid("abc")).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.isValid(valid)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(valid)).isEqualTo(valid);

      final Object encoded = cm.encode(cm.fix(valid));
      assertThat(encoded).isInstanceOf(String.class);
      assertThat(encoded).isEqualTo(valid);
      assertThat(cm.decode(encoded)).isEqualTo(valid);
   }

   @Test
   public void testNumberMonetaryConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("isMonetary");

      final NumberFormat nf = NumberFormat.getNumberInstance(l);
      ((DecimalFormat) nf).setParseBigDecimal(true);
      final String valid = nf.format(0.02);

      assertThat(cm.isValid(valid)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(valid)).isEqualTo(valid);

      final Object encoded = cm.encode(cm.fix(valid));
      assertThat(encoded).isInstanceOf(BigDecimal.class);
      assertThat(encoded).isEqualTo(new BigDecimal(valid));
      assertThat(cm.decode(encoded)).isEqualTo(valid);
   }

   @Test
   public void testNumberConstraint() throws Exception {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);
      cm.registerConstraint("isNumber");

      final NumberFormat nf = NumberFormat.getNumberInstance(l);
      final String validDouble = nf.format(0.02);
      final String validLong = nf.format(123);
      final BigDecimal bd = new BigDecimal(Double.MAX_VALUE).multiply(new BigDecimal(Double.MAX_VALUE));
      final String validBigDecimal = bd.toString();

      assertThat(cm.isValid(validDouble)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.isValid(validLong)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.isValid(validBigDecimal)).isEqualTo(Constraint.ConstraintResult.VALID);
      assertThat(cm.fix(validDouble)).isEqualTo(validDouble);
      assertThat(cm.fix(validLong)).isEqualTo(validLong);
      assertThat(cm.fix(validBigDecimal)).isEqualTo(validBigDecimal);

      Object encoded = cm.encode(cm.fix(validDouble));
      assertThat(encoded).isInstanceOf(Double.class);
      assertThat(encoded).isEqualTo(0.02);
      assertThat(cm.decode(encoded)).isEqualTo(validDouble);

      encoded = cm.encode(cm.fix(validLong));
      assertThat(encoded).isInstanceOf(Long.class);
      assertThat(encoded).isEqualTo(123L);
      assertThat(cm.decode(encoded)).isEqualTo(validLong);

      encoded = cm.encode(cm.fix(validBigDecimal));
      assertThat(encoded).isInstanceOf(BigDecimal.class);
      assertThat(encoded).isEqualTo(bd);
      assertThat(cm.decode(encoded)).isEqualTo(validBigDecimal);
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