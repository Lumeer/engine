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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class PaymentFacade extends AbstractFacade {

   @Inject
   private PaymentDao paymentDao;

   public Payment createPayment(final Organization organization, final Payment payment) {
      checkPermissions(organization);

      return paymentDao.createPayment(organization, payment);
   }

   public List<Payment> getPayments(final Organization organization) {
      checkPermissions(organization);

      return paymentDao.getPayments(organization);
   }

   public Payment.ServiceLevel getCurrentServiceLevel(final Organization organization) {
      Payment.ServiceLevel serviceLevel = Payment.ServiceLevel.FREE;

      if (workspaceKeeper.getServiceLevel(organization) == null) {
         final LocalDateTime now = LocalDateTime.now();
         final Payment latestPayment = paymentDao.getLatestPayment(organization);

         // is the payment active? be tolerant to dates/time around the interval border
         if (latestPayment != null && now.isBefore(LocalDateTime.from(latestPayment.getValidUntil().toInstant().plus(Duration.ofDays(1))))
               && now.isAfter(LocalDateTime.from(latestPayment.getStart().toInstant().minus(Duration.ofDays(1))))) {
            serviceLevel = latestPayment.getServiceLevel();
         }

         workspaceKeeper.setServiceLevel(organization, serviceLevel);
      }

      return serviceLevel;
   }

   public String getAuthToken() {
      return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   }

   public Payment updatePayment(final Organization organization, final String paymentId, final Payment.PaymentState paymentState) {
      checkPermissions(organization);

      workspaceKeeper.clearServiceLevel(organization);

      final Payment payment = paymentDao.getPayment(organization, paymentId);
      payment.setState(paymentState);

      return paymentDao.updatePayment(organization, payment);
   }

   public Payment getPayment(final Organization organization, final String paymentId) {
      checkPermissions(organization);

      return paymentDao.getPayment(organization, paymentId);
   }

   private void checkPermissions(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      permissionsChecker.checkRole(organization, Role.MANAGE);
   }

}
