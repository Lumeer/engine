package io.lumeer.core.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;

import com.mongodb.client.model.geojson.NamedCoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class ConstraintManagerTest {

   final static Locale l = Locale.forLanguageTag("en_US");

   @Test
   public void testTypesHandling() {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      assertThat(cm.encode("2.34")).isInstanceOf(BigDecimal.class);
      assertThat(cm.encode("2,34e3")).isInstanceOf(BigDecimal.class).isEqualTo(new BigDecimal("2.34E+3"));
      assertThat(cm.encode("-2.34e-3")).isInstanceOf(BigDecimal.class).isEqualTo(new BigDecimal("-0.00234"));;
      assertThat(cm.encode("2019-01-20")).isInstanceOf(String.class);
      assertThat(cm.encode("2")).isInstanceOf(Long.class);
      assertThat(cm.encode("a2")).isInstanceOf(String.class);

      final String longNumber = new BigDecimal(Double.MAX_VALUE).multiply(new BigDecimal(Double.MAX_VALUE)).toString();
      assertThat(cm.encode(longNumber)).isInstanceOf(String.class);
      assertThat(longNumber).isEqualTo(cm.encode(longNumber).toString());
   }

   @Test
   public void testDateTimeConstraint() {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      final Date d = new Date(1234567890);
      final ZonedDateTime dt = ZonedDateTime.from(d.toInstant().atZone(ZoneId.of("Asia/Kolkata")));
      final String valid1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", l).format(dt);
      final String valid2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSO", l).format(dt);
      final String valid3 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSx", l).format(dt);
      final String valid4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", l).format(dt);

      Object encoded = cm.encode(valid1, new Constraint(ConstraintType.DateTime, null));
      assertThat(encoded).isInstanceOf(Date.class);
      assertThat(((Date) encoded).toInstant()).isEqualTo(d.toInstant());

      encoded = cm.encode(valid2, new Constraint(ConstraintType.DateTime, null));
      assertThat(((Date) encoded).toInstant()).isEqualTo(d.toInstant());

      encoded = cm.encode(valid3, new Constraint(ConstraintType.DateTime, null));
      assertThat(encoded).isEqualTo(d);

      encoded = cm.encode(valid4, new Constraint(ConstraintType.DateTime, null));
      assertThat(encoded).isEqualTo(d);
   }

   @Test
   public void testNumberConstraint() {
      final Constraint numberConstraint = new Constraint(ConstraintType.Number, null);
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      final NumberFormat nf = NumberFormat.getNumberInstance(l);
      final String validDouble = nf.format(0.02);
      final String validLong = nf.format(123);
      final BigDecimal bdv = new BigDecimal("123454.434563456345");
      final String validBigDecimal = bdv.toString();
      final BigDecimal bd = new BigDecimal(Double.MAX_VALUE).multiply(new BigDecimal(Double.MAX_VALUE));
      final String unstorableBigDecimal = bd.toString(); // does not fit into Decimal128

      Object encoded = cm.encode(validDouble, numberConstraint);
      assertThat(encoded).isInstanceOf(BigDecimal.class);
      assertThat(encoded).isEqualTo(new BigDecimal("0.02"));

      encoded = cm.encode(validLong, numberConstraint);
      assertThat(encoded).isInstanceOf(Long.class);
      assertThat(encoded).isEqualTo(123L);

      encoded = cm.encode(validBigDecimal, numberConstraint);
      assertThat(encoded).isInstanceOf(BigDecimal.class);
      assertThat(encoded).isEqualTo(bdv);

      encoded = cm.encode(unstorableBigDecimal, numberConstraint);
      assertThat(encoded).isInstanceOf(String.class);
      assertThat(encoded).isEqualTo(unstorableBigDecimal);
   }

   @Test
   public void testBooleanConstraint() {
      final Constraint booleanConstraint = new Constraint(ConstraintType.Boolean, null);
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      Object encoded = cm.encode("true", booleanConstraint);
      assertThat(encoded).isEqualTo(Boolean.TRUE);

      encoded = cm.encode("yes", booleanConstraint);
      assertThat(encoded).isEqualTo("yes");

      encoded = cm.encode("   fAlse    ", booleanConstraint);
      assertThat(encoded).isEqualTo(Boolean.FALSE);
   }

   @Test
   public void testPercentageConstraint() {
      final Constraint percentageConstraint = new Constraint(ConstraintType.Percentage, null);
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      Object encoded = cm.encode("12%", percentageConstraint);
      assertThat(encoded).isEqualTo(new BigDecimal("0.12"));

      encoded = cm.encode("12 %", percentageConstraint);
      assertThat(encoded).isEqualTo(new BigDecimal("0.12"));

      encoded = cm.encode("1,2e1 %", percentageConstraint);
      assertThat(encoded).isEqualTo(new BigDecimal("0.12"));

      encoded = cm.encode("12e-2", percentageConstraint);
      assertThat(encoded).isEqualTo(new BigDecimal("0.12"));
   }

   @Test
   public void testCoordinatesConstraint() {
      final Constraint coordinatesConstraint = new Constraint(ConstraintType.Coordinates, null);
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      Object encoded = cm.encode("40.123, -74.123", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));

      encoded = cm.encode("40.123°N 74.123°W", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));

      encoded = cm.encode("40°7´22.8\"N 74°7´22.8\"W", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));

      encoded = cm.encode("40°7.38'N, 74°7.38'W", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));

      encoded = cm.encode("N40°7’22.8, W74°7’22.8\"", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));

      encoded = cm.encode("40 7 22.8, W74 7 22.8", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));

      encoded = cm.encode("40.123N 74.123W", coordinatesConstraint);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));
   }

   @Test
   public void testTryHard() {
      final ConstraintManager cm = new ConstraintManager();
      cm.setLocale(l);

      Object encoded = cm.encode("12%", null);
      assertThat(encoded).isEqualTo("12%");

      encoded = cm.encodeForFce("12%", null);
      assertThat(encoded).isEqualTo(new BigDecimal("0.12"));

      //yyyy-MM-dd'T'HH:mm:ss.SSSO
      final String time = "2013-12-01T23:12:18.784Z";
      encoded = cm.encode(time, null);
      assertThat(encoded).isEqualTo(time);

      encoded = cm.encodeForFce(time, null);
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", l);
      assertThat(encoded).isEqualTo(Date.from(ZonedDateTime.from(dtf.parse(time.trim())).toInstant()));

      encoded = cm.encode("True", null);
      assertThat(encoded).isEqualTo("True");

      encoded = cm.encodeForFce("True", null);
      assertThat(encoded).isEqualTo(Boolean.TRUE);

      encoded = cm.encodeForFce("4410", null);
      assertThat(encoded).isEqualTo(4410L);

      encoded = cm.encodeForFce("04410", null);
      assertThat(encoded).isEqualTo("04410");

      encoded = cm.encodeForFce("40°7.38'N, 74°7.38'W", null);
      assertThat(encoded).isEqualTo(new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(40.123, -74.123)));
   }
}
