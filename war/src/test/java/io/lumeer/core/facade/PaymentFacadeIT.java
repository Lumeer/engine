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

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.engine.IntegrationTestBase;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
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
   private static final String PID1 = "700000001";
   private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

   private Organization organization = null;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private PaymentGatewayFacade paymentGatewayFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @BeforeClass
   public void beforeClass() {
      organization = createOrganization();
   }

   @AfterClass
   public void afterClass() {
      dropOrganization();
   }

   @Test
   public void testServiceLevels() {
      paymentGatewayFacade.setDryRun(true);

      Payment payment = new Payment(new Date(), 1770, PID1,
            getDate("2018-04-01T12:00:00.000+0100"),
            getDate("2019-04-01T12:00:00.000+0100"),
            Payment.PaymentState.CREATED, Payment.ServiceLevel.BASIC, 10, "cz", "CZK");

      paymentFacade.createPayment(organization, payment, "", "");
      paymentFacade.updatePayment(organization, PID1);
   }

   private Date getDate(final String date) {
      return Date.from(Instant.from(DTF.parse(date)));
   }

   private Organization createOrganization() {
      Organization organization = new JsonOrganization(CODE, NAME, ICON, COLOR, null, null);
      return organizationFacade.createOrganization(organization);
   }

   private void dropOrganization() {
      organizationFacade.deleteOrganization(CODE);
   }
}
