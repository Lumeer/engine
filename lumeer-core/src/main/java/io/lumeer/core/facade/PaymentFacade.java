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
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class PaymentFacade extends AbstractFacade {

   @Inject
   private PaymentDao paymentDao;

   @Inject
   private PaymentGatewayFacade paymentGateway;

   private Payment currentPayment = null;

   public Payment createPayment(final Organization organization, final Payment payment, final String notifyUrl, final String returnUrl) {
      checkManagePermissions(organization);

      final Payment establishedPayment = paymentGateway.createPayment(payment, returnUrl, notifyUrl);

      return paymentDao.createPayment(organization, establishedPayment);
   }

   public List<Payment> getPayments(final Organization organization) {
      checkManagePermissions(organization);

      return paymentDao.getPayments(organization);
   }

   private Payment getCurrentPayment(final Organization organization) {
      if (currentPayment == null) {
         final Date now = new Date();
         final Payment latestPayment = getPaymentAt(organization, now);

         // is the payment active? be tolerant to dates/time around the interval border
         if (latestPayment != null) {
            currentPayment = latestPayment;
         }
      }

      return currentPayment;
   }

   private Payment getPaymentAt(final Organization organization, final Date date) {
      final Payment payment = paymentDao.getPaymentAt(organization, date);

      // is the payment active? be tolerant to dates/time around the interval border
      if (payment != null
            && date.before(new Date(payment.getValidUntil().getTime() + TimeUnit.SECONDS.toMillis(1)))
            && date.after(new Date(payment.getStart().getTime() - TimeUnit.SECONDS.toMillis(1)))) {
         return payment;
      }

      return null;
   }

   public Payment.ServiceLevel getCurrentServiceLevel(final Organization organization) {
      checkReadPermissions(organization);

      Payment.ServiceLevel serviceLevel = Payment.ServiceLevel.FREE;

      if (workspaceKeeper.getServiceLevel(organization) == null) {
         final Payment payment = getCurrentPayment(organization);
         if (payment != null) {
            serviceLevel = payment.getServiceLevel();
         }

         workspaceKeeper.setServiceLevel(organization, serviceLevel);
      } else {
         serviceLevel = workspaceKeeper.getServiceLevel(organization);
      }

      return serviceLevel;
   }

   public ServiceLimits getCurrentServiceLimits(final Organization organization) {
      checkReadPermissions(organization);

      final Payment payment = getCurrentPayment(organization);

      if (payment != null) {
         workspaceKeeper.setServiceLevel(organization, payment.getServiceLevel());

         if (payment.getServiceLevel() == Payment.ServiceLevel.BASIC) {
            return new ServiceLimits(Payment.ServiceLevel.BASIC, Math.min(ServiceLimits.BASIC_LIMITS.getUsers(), payment.getUsers()),
                  ServiceLimits.BASIC_LIMITS.getProjects(), ServiceLimits.BASIC_LIMITS.getFiles(), ServiceLimits.BASIC_LIMITS.getDocuments(),
                  ServiceLimits.BASIC_LIMITS.getDbSizeMb(), payment.getValidUntil());
         }
      }

      return ServiceLimits.FREE_LIMITS;
   }

   public ServiceLimits getServiceLimitsAt(final Organization organization, final Date date) {
      checkManagePermissions(organization);

      final Payment payment = getPaymentAt(organization, date);

      if (payment != null) {
         if (payment.getServiceLevel() == Payment.ServiceLevel.BASIC) {
            return new ServiceLimits(Payment.ServiceLevel.BASIC, Math.min(ServiceLimits.BASIC_LIMITS.getUsers(), payment.getUsers()),
                  ServiceLimits.BASIC_LIMITS.getProjects(), ServiceLimits.BASIC_LIMITS.getFiles(), ServiceLimits.BASIC_LIMITS.getDocuments(),
                  ServiceLimits.BASIC_LIMITS.getDbSizeMb(), payment.getValidUntil());
         }
      }

      return ServiceLimits.FREE_LIMITS;
   }

   public Payment updatePayment(final Organization organization, final String paymentId) {
      checkManagePermissions(organization);

      currentPayment = null;
      workspaceKeeper.clearServiceLevel(organization);

      final Payment payment = paymentDao.getPayment(organization, paymentId);
      payment.setState(paymentGateway.getPaymentStatus(paymentId));

      return paymentDao.updatePayment(organization, payment);
   }

   public Payment getPayment(final Organization organization, final String paymentId) {
      checkManagePermissions(organization);

      return paymentDao.getPayment(organization, paymentId);
   }

   private void checkManagePermissions(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      permissionsChecker.checkRole(organization, Role.MANAGE);
   }

   private void checkReadPermissions(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      permissionsChecker.checkRole(organization, Role.READ);
   }
}
