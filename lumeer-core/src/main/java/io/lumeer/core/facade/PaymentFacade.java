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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class PaymentFacade extends AbstractFacade {

   @Inject
   private PaymentDao paymentDao;

   public Payment createPayment(final Payment payment) {
      checkPermissions();

      return paymentDao.createPayment(payment);
   }

   public List<Payment> getPayments() {
      checkPermissions();

      return paymentDao.getPayments();
   }

   public Payment.ServiceLevel getCurrentServiceLevel() {
      Payment.ServiceLevel serviceLevel = Payment.ServiceLevel.FREE;

      if (workspaceKeeper.getServiceLevel() == null) {
         final LocalDateTime now = LocalDateTime.now();
         final List<Payment> payments = paymentDao.getPayments().stream().filter(p -> p.getDate().isAfter(now) && p.getValidUntil().isBefore(now)).collect(Collectors.toList());

         if (payments.size() > 0) {
            serviceLevel = Payment.ServiceLevel.values()[payments.stream().map(p -> p.getServiceLevel().ordinal()).max(Integer::max).orElse(0)];
         }

         workspaceKeeper.setServiceLevel(serviceLevel);
      }

      return serviceLevel;
   }

   public String getAuthToken() {
      return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   }

   public Payment updatePayment(final String paymentId, final Payment.PaymentState paymentState) {
      checkPermissions();

      workspaceKeeper.clearServiceLevel();

      final Payment payment = paymentDao.getPayment(paymentId);
      payment.setState(paymentState);

      return paymentDao.updatePayment(payment);
   }

   public Payment getPayment(final String paymentId) {
      checkPermissions();

      return paymentDao.getPayment(paymentId);
   }

   private void checkPermissions() {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, Role.MANAGE);
   }


}
