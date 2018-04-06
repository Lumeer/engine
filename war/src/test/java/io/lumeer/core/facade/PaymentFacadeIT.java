/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.*;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.engine.IntegrationTestBase;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RunWith(Arquillian.class)
public class PaymentFacadeIT extends IntegrationTestBase {

   private static final String CODE = "ORG1";
   private static final String NAME = "Organization 1";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#ff7700";
   private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

   private Organization organization = null;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private PaymentGatewayFacade paymentGatewayFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Before
   public void beforeMethod() { // @BeforeClass requires static method, this does not work with injections
      paymentGatewayFacade.setDryRun(true);
      organization = createOrganization();
   }

   @Test
   public void testNoPayment() {
      final ServiceLimits limits = paymentFacade.getServiceLimitsAt(organization, new Date());
      assertThat(limits.getServiceLevel())
            .as("With no payments paid, we should have the FREE tier.")
            .isEqualTo(Payment.ServiceLevel.FREE);
   }

   @Test
   public void testServiceLevels() {
      final String paymentId = createPayment("2011-04-01T00:00:00.000+0100", "2011-04-30T23:59:59.999+0100", false);

      ServiceLimits limits = paymentFacade.getServiceLimitsAt(organization, getDate("2011-04-15T12:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("With unpaid payment, we should still be on the FREE tier.")
            .isEqualTo(Payment.ServiceLevel.FREE);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2010-04-15T12:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("With unpaid payment, we should still be on the FREE tier even before the terms.")
            .isEqualTo(Payment.ServiceLevel.FREE);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2012-04-15T12:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("With unpaid payment, we should still be on the FREE tier and also after the terms.")
            .isEqualTo(Payment.ServiceLevel.FREE);

      paymentFacade.updatePayment(organization, paymentId); // now set it to paid

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2011-04-15T12:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("With paid payment, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);
   }

   @Test
   public void testCornerCases() {
      createPayment("2009-04-01T00:00:00.000+0100", "2009-04-30T23:59:59.999+0100", true);

      ServiceLimits limits = paymentFacade.getServiceLimitsAt(organization, getDate("2009-04-15T12:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("With paid payment, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2009-04-01T00:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("At the beginning of the paid period, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2009-04-30T23:59:59.999+0100"));
      assertThat(limits.getServiceLevel())
            .as("At the end of the paid period, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2009-03-31T23:59:59.999+0100"));
      assertThat(limits.getServiceLevel())
            .as("Before the beginning of the paid period, we should be on the FREE tier.")
            .isEqualTo(Payment.ServiceLevel.FREE);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2009-05-01T00:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("After the end of the paid period, we should be on the FREE tier.")
            .isEqualTo(Payment.ServiceLevel.FREE);
   }

   @Test
   public void testTimezones() {
      createPayment("2008-04-01T00:00:00.000-0500", "2008-04-30T23:59:59.999-0500", true);

      ServiceLimits limits = paymentFacade.getServiceLimitsAt(organization, getDate("2008-04-15T12:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("No matter what time zone, in the middle of the paid period, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2008-04-01T06:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("No matter the time zone, at the beginning of the paid period, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2008-05-01T05:59:59.999+0100"));
      assertThat(limits.getServiceLevel())
            .as("No matter the time zone, at the end of the paid period, we should be on the BASIC tier.")
            .isEqualTo(Payment.ServiceLevel.BASIC);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2008-04-01T05:59:59.999+0100"));
      assertThat(limits.getServiceLevel())
            .as("No matter the time zone, before the beginning of the paid period, we should be on the FREE tier.")
            .isEqualTo(Payment.ServiceLevel.FREE);

      limits = paymentFacade.getServiceLimitsAt(organization, getDate("2008-05-01T06:00:00.000+0100"));
      assertThat(limits.getServiceLevel())
            .as("No matter the time zone, after the end of the paid period, we should be on the FREE tier.")
            .isEqualTo(Payment.ServiceLevel.FREE);
   }

   private Date getDate(final String date) {
      return Date.from(Instant.from(DTF.parse(date)));
   }

   private Organization createOrganization() {
      Organization organization = new JsonOrganization(CODE, NAME, ICON, COLOR, null, null);
      return organizationFacade.createOrganization(organization);
   }

   private String createPayment(final String from, final String until, final boolean paid) {
      Payment payment = new Payment(null, new Date(), 1770, "",
            getDate(from),
            getDate(until),
            Payment.PaymentState.CREATED, Payment.ServiceLevel.BASIC, 10, "cz", "CZK", null);
      final String paymentId = paymentFacade.createPayment(organization, payment, "", "").getPaymentId();

      if (paid) {
         paymentFacade.updatePayment(organization, paymentId); //this switches it to PAID in dry run mode
      }

      return paymentId;
   }
}
