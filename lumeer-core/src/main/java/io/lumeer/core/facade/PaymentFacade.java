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

import static java.util.Map.entry;

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class PaymentFacade extends AbstractFacade {

   private static final Map<String, Double> FULL_PRICES = Map.ofEntries(
         entry("CZK", 205.0d),
         entry("EUR", 8.0d),
         entry("USD", 9.9d)
   );
   private static final Map<String, Double> DISCOUNT_PRICES = Map.ofEntries(
         entry("CZK", 169.0d),
         entry("EUR", 6.7d),
         entry("USD", 8.3d)
   );

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private PaymentDao paymentDao;

   @Inject
   private PaymentGatewayFacade paymentGateway;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Inject
   private CompanyContactFacade companyContactFacade;

   private Payment currentPayment = null;

   public Payment createPayment(final Organization organization, final Payment payment, final String notifyUrl, final String returnUrl) {
      checkManagePermissions(organization);
      final Payment storedPayment = paymentDao.createPayment(organization, payment);

      final Payment establishedPayment = paymentGateway.createPayment(storedPayment, returnUrl, notifyUrl + "/" + storedPayment.getId());

      final Payment result = paymentDao.updatePayment(organization, storedPayment.getId(), establishedPayment);

      freshdeskFacade.logTicket(authenticatedUser.getCurrentUser(), "New payment for organization " + organization.getCode(),
            "A new order of Lumeer was placed for " + payment.getUsers() + " users with amount of "
                  + payment.getCurrency() + " " + payment.getAmount());

      return result;
   }

   private Organization getOrganizationUnsafe(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);

      return organization;
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

   public Map<String, ServiceLimits> getAllServiceLimits(List<Organization> organizations){
       return organizations.stream().collect(Collectors.toMap(Organization::getId, this::getCurrentServiceLimits));
   }

   public ServiceLimits getCurrentServiceLimits(final Organization organization) {
      checkReadPermissions(organization);
      ServiceLimits serviceLimits = workspaceKeeper.getServiceLimits(organization);

      if (serviceLimits != null) {
         return serviceLimits;
      }

      final Payment payment = getCurrentPayment(organization);
      final Optional<Date> validUntil = getValidUntil(getFutureContinuousPayments(organization, new Date()));

      if (payment != null && validUntil.isPresent()) {
         if (payment.getServiceLevel() == Payment.ServiceLevel.BASIC) {
            serviceLimits = new ServiceLimits(Payment.ServiceLevel.BASIC, Math.min(ServiceLimits.BASIC_LIMITS.getUsers(), payment.getUsers()),
                  ServiceLimits.BASIC_LIMITS.getProjects(), ServiceLimits.BASIC_LIMITS.getFiles(), ServiceLimits.BASIC_LIMITS.getDocuments(),
                  ServiceLimits.BASIC_LIMITS.getDbSizeMb(), validUntil.get());
            workspaceKeeper.setServiceLimits(organization, serviceLimits);
            return serviceLimits;
         }
      }

      workspaceKeeper.setServiceLimits(organization, ServiceLimits.FREE_LIMITS);

      return ServiceLimits.FREE_LIMITS;
   }

   private List<Payment> getFutureContinuousPayments(final Organization organization, final Date date) {
      final List<Payment> result = new ArrayList<>();

      Payment payment = getPaymentAt(organization, date);

      while (payment != null) {
         result.add(payment);
         payment = getPaymentAt(organization, new Date(payment.getValidUntil().getTime() + TimeUnit.MINUTES.toMillis(1)));
      }

      return result;
   }

   private Optional<Date> getValidUntil(final List<Payment> continuousPayments) {
      long time = continuousPayments.stream().map(payment -> payment.getValidUntil().getTime()).max(Long::compareTo).orElse(-1L);
      return Optional.ofNullable(time >= 0 ? new Date(time) : null);
   }

   public ServiceLimits getServiceLimitsAt(final Organization organization, final Date date) {
      checkManagePermissions(organization);

      final Payment payment = getPaymentAt(organization, date);
      final Optional<Date> validUntil = getValidUntil(getFutureContinuousPayments(organization, date));

      if (payment != null && validUntil.isPresent()) {
         if (payment.getServiceLevel() == Payment.ServiceLevel.BASIC) {
            return new ServiceLimits(Payment.ServiceLevel.BASIC, Math.min(ServiceLimits.BASIC_LIMITS.getUsers(), payment.getUsers()),
                  ServiceLimits.BASIC_LIMITS.getProjects(), ServiceLimits.BASIC_LIMITS.getFiles(), ServiceLimits.BASIC_LIMITS.getDocuments(),
                  ServiceLimits.BASIC_LIMITS.getDbSizeMb(), validUntil.get());
         }
      }

      return ServiceLimits.FREE_LIMITS;
   }

   public Payment updatePayment(final String organizationCode, final String id) {
      final Organization organization = getOrganizationUnsafe(organizationCode);

      currentPayment = null;
      workspaceKeeper.clearServiceLimits(organization);

      final Payment payment = paymentDao.getPaymentByDbId(organization, id);
      payment.setState(paymentGateway.getPaymentStatus(payment.getPaymentId()));

      final Payment result = paymentDao.updatePayment(organization, payment);

      freshdeskFacade.logTicket(authenticatedUser.getCurrentUser(), "Payment status updated for organization " + organizationCode,
            "Payment in amount of " + payment.getCurrency() + " " + payment.getAmount() + " is in state " + payment.getState().name() +
                  ". Invoice might need to be prepared for " + companyContactFacade.getCompanyContact(organization));

      return result;
   }

   public Payment getPayment(final Organization organization, final String paymentId) {
      checkManagePermissions(organization);

      return paymentDao.getPayment(organization, paymentId);
   }

   public Payment checkPaymentValues(final Payment payment) {
      int months = Period.between(
            LocalDate.ofInstant(payment.getStart().toInstant(), ZoneId.systemDefault()),
            LocalDate.ofInstant(payment.getValidUntil().toInstant(), ZoneId.systemDefault())
            ).getMonths();
      final Map<String, Double> prices = months >= 12 ? DISCOUNT_PRICES : FULL_PRICES;
      long minPrice = Math.round(months * prices.get(payment.getCurrency()) * payment.getUsers());

      if (payment.getAmount() < minPrice) {
         payment.setAmount(minPrice);
      }

      return payment;
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
