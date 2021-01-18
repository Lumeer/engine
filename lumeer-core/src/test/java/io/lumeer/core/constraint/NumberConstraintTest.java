package io.lumeer.core.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

public class NumberConstraintTest {

   private final Object emptyConfig = null;
   private final Object decimalsConfig = Collections.singletonMap("decimals", 3);

   @Test
   public void testIsEqual() {
      var constraint = new NumberConstraint(emptyConfig);

      assertThat(constraint.createDataValue(" 1").isEqual(constraint.createDataValue(1))).isTrue();
      assertThat(constraint.createDataValue("1").isEqual(constraint.createDataValue(1.0f))).isTrue();
      assertThat(constraint.createDataValue(new BigDecimal(1)).isEqual(constraint.createDataValue(1.0f))).isTrue();
      assertThat(constraint.createDataValue(new BigDecimal(1)).isEqual(constraint.createDataValue(1.1f))).isFalse();
      assertThat(constraint.createDataValue(new BigDecimal("30.32")).isEqual(constraint.createDataValue("30.3200"))).isTrue();
      assertThat(constraint.createDataValue(null).isEqual(constraint.createDataValue(""))).isTrue();
      assertThat(constraint.createDataValue(null).isEqual(constraint.createDataValue(null))).isTrue();
      assertThat(constraint.createDataValue(null).isEqual(constraint.createDataValue("1"))).isFalse();
      assertThat(constraint.createDataValue("  1333  ").isEqual(constraint.createDataValue(null))).isFalse();
      assertThat(constraint.createDataValue("1000000").isEqual(constraint.createDataValue(1e6))).isTrue();
      assertThat(constraint.createDataValue("1000000").isEqual(constraint.createDataValue("1e6"))).isTrue();
      assertThat(constraint.createDataValue("0.0001").isEqual(constraint.createDataValue(10e-5))).isTrue();

      var decimalsConstraint = new NumberConstraint(decimalsConfig);
      assertThat(decimalsConstraint.createDataValue("1.111").isEqual(decimalsConstraint.createDataValue(1.111234))).isTrue();
      assertThat(decimalsConstraint.createDataValue("1.111").isEqual(decimalsConstraint.createDataValue(1.111634))).isFalse();

      var zeroDecimalsConstraint = new NumberConstraint(Collections.singletonMap("decimals", 0));
      assertThat(zeroDecimalsConstraint.createDataValue("1.234").isEqual(zeroDecimalsConstraint.createDataValue(1.2031))).isTrue();
      assertThat(zeroDecimalsConstraint.createDataValue("1.567").isEqual(zeroDecimalsConstraint.createDataValue(2.2031))).isTrue();
   }

   @Test
   public void testIsNotEqual() {
      var constraint = new NumberConstraint(emptyConfig);

      assertThat(constraint.createDataValue("30").isNotEqual(constraint.createDataValue(30))).isFalse();
      assertThat(constraint.createDataValue(new BigDecimal(1)).isNotEqual(constraint.createDataValue(1.1f))).isTrue();
      assertThat(constraint.createDataValue(null).isNotEqual(constraint.createDataValue(null))).isFalse();
      assertThat(constraint.createDataValue(null).isNotEqual(constraint.createDataValue("1"))).isTrue();
      assertThat(constraint.createDataValue("1333").isNotEqual(constraint.createDataValue(null))).isTrue();
      assertThat(constraint.createDataValue(new BigDecimal("1330.32")).isNotEqual(constraint.createDataValue("1330.3200"))).isFalse();
      assertThat(constraint.createDataValue("3").isNotEqual(constraint.createDataValue(3.000f))).isFalse();
   }

   @Test
   public void testGreaterThan() {
      var constraint = new NumberConstraint(emptyConfig);

      assertThat(constraint.createDataValue("30").greaterThan(constraint.createDataValue(30))).isFalse();
      assertThat(constraint.createDataValue("30").greaterThanEquals(constraint.createDataValue(30))).isTrue();
      assertThat(constraint.createDataValue(new BigDecimal("1.1")).greaterThan(constraint.createDataValue(1f))).isTrue();
      assertThat(constraint.createDataValue(null).greaterThan(constraint.createDataValue(null))).isFalse();
      assertThat(constraint.createDataValue(null).greaterThan(constraint.createDataValue("1"))).isFalse();
      assertThat(constraint.createDataValue("1333").greaterThan(constraint.createDataValue(null))).isTrue();
      assertThat(constraint.createDataValue("3").greaterThan(constraint.createDataValue("25"))).isFalse();
      assertThat(constraint.createDataValue("3").greaterThanEquals(constraint.createDataValue("25"))).isFalse();
      assertThat(constraint.createDataValue("1333").greaterThanEquals(constraint.createDataValue(null))).isTrue();
      assertThat(constraint.createDataValue("1333").greaterThanEquals(constraint.createDataValue("1299.999"))).isTrue();
   }

   @Test
   public void testLowerThan() {
      var constraint = new NumberConstraint(emptyConfig);

      assertThat(constraint.createDataValue("3").lowerThan(constraint.createDataValue(3))).isFalse();
      assertThat(constraint.createDataValue("30").lowerThanEquals(constraint.createDataValue(30))).isTrue();
      assertThat(constraint.createDataValue(new BigDecimal("10")).lowerThan(constraint.createDataValue(10.1f))).isTrue();
      assertThat(constraint.createDataValue(null).lowerThan(constraint.createDataValue(null))).isFalse();
      assertThat(constraint.createDataValue(null).lowerThan(constraint.createDataValue("1"))).isTrue();
      assertThat(constraint.createDataValue("1333").lowerThan(constraint.createDataValue(null))).isFalse();
      assertThat(constraint.createDataValue("3").lowerThan(constraint.createDataValue("25"))).isTrue();
      assertThat(constraint.createDataValue("3").lowerThanEquals(constraint.createDataValue("25"))).isTrue();
   }

   @Test
   public void testIsEmpty() {
      var constraint = new NumberConstraint(emptyConfig);

      assertThat(constraint.createDataValue("3").isEmpty()).isFalse();
      assertThat(constraint.createDataValue("  ").isEmpty()).isTrue();
      assertThat(constraint.createDataValue("").isEmpty()).isTrue();
      assertThat(constraint.createDataValue(null).isEmpty()).isTrue();
      assertThat(constraint.createDataValue("xx").isEmpty()).isTrue();
   }

   @Test
   public void testIsNotEmpty() {
      var constraint = new NumberConstraint(emptyConfig);

      assertThat(constraint.createDataValue("3").isNotEmpty()).isTrue();
      assertThat(constraint.createDataValue(30).isNotEmpty()).isTrue();
      assertThat(constraint.createDataValue("  ").isNotEmpty()).isFalse();
      assertThat(constraint.createDataValue("").isNotEmpty()).isFalse();
      assertThat(constraint.createDataValue(null).isNotEmpty()).isFalse();
      assertThat(constraint.createDataValue("xx").isNotEmpty()).isFalse();
   }

}
