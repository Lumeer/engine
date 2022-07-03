/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.PaymentStats;
import io.lumeer.api.model.ReferralPayment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.core.adapter.PaymentAdapter;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.engine.api.event.UpdateServiceLimits;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ReferralPaymentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class PaymentFacade extends AbstractFacade {

   private static final Map<String, Double> FULL_PRICES = Map.ofEntries(
         entry("CZK", 290.00d),
         entry("EUR", 11.50d),
         entry("USD", 13.00d)
   );
   private static final Map<String, Double> DISCOUNT_PRICES = Map.ofEntries(
         entry("CZK", 220.0d),
         entry("EUR", 9.00d),
         entry("USD", 10.00d)
   );

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private PaymentDao paymentDao;

   @Inject
   private UserFacade userFacade;

   @Inject
   private ReferralPaymentDao referralPaymentDao;

   @Inject
   private PaymentGatewayFacade paymentGateway;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Inject
   private CompanyContactDao companyContactDao;

   @Inject
   private Event<UpdateServiceLimits> updateServiceLimitsEvent;

   @Inject
   private CacheFactory cacheFactory;

   private Cache<Payment> paymentCache;

   private PaymentAdapter paymentAdapter;

   @PostConstruct
   public void initCache() {
      paymentCache = cacheFactory.getCache();

      paymentAdapter = new PaymentAdapter(paymentDao, paymentCache);
   }

   public Payment createPayment(final Organization organization, final Payment payment, final String notifyUrl, final String returnUrl) {
      checkManagePermissions(organization);

      final User currentUser = authenticatedUser.getCurrentUser();
      if (currentUser.getReferral() != null) {
         payment.setReferral(currentUser.getReferral());
      }

      final Payment storedPayment = paymentDao.createPayment(organization.getId(), payment);

      final Payment establishedPayment = paymentGateway.createPayment(storedPayment, returnUrl, notifyUrl + "/" + storedPayment.getId());

      final Payment result = paymentDao.updatePayment(organization.getId(), storedPayment.getId(), establishedPayment);

      freshdeskFacade.logTicket(authenticatedUser.getCurrentUser(), "New payment for organization " + organization.getCode(),
            "A new order of Lumeer was placed for " + payment.getUsers() + " users with amount of "
                  + payment.getCurrency() + " " + payment.getAmount());

      if (updateServiceLimitsEvent != null) {
         updateServiceLimitsEvent.fire(new UpdateServiceLimits(organization, getCurrentServiceLimits(organization)));
      }

      return result;
   }

   private Organization getOrganizationUnsafe(final String organizationId) {
      return organizationDao.getOrganizationById(organizationId);
   }

   public List<Payment> getPayments(final Organization organization) {
      checkManagePermissions(organization);

      return paymentDao.getPayments(organization.getId());
   }

   public Map<String, ServiceLimits> getAllServiceLimits(List<Organization> organizations) {
      return organizations.stream().collect(Collectors.toMap(Organization::getId, this::getCurrentServiceLimits));
   }

   public ServiceLimits getCurrentServiceLimits(final Organization organization) {
      checkReadPermissions(organization);
      ServiceLimits serviceLimits = workspaceKeeper.getServiceLimits(organization);

      if (serviceLimits != null) {
         return serviceLimits;
      }

      serviceLimits = paymentAdapter.computeServiceLimits(organization, permissionsChecker.skipPayments());
      workspaceKeeper.setServiceLimits(organization, ServiceLimits.FREE_LIMITS);
      return serviceLimits;
   }

   public Payment updatePayment(final String organizationId, final String id) {
      final Organization organization = getOrganizationUnsafe(organizationId);

      paymentCache.remove(organizationId);
      workspaceKeeper.clearServiceLimits(organization);

      final Payment payment = paymentDao.getPaymentByDbId(organization.getId(), id);
      final Payment.PaymentState newState = paymentGateway.getPaymentStatus(payment.getPaymentId());

      if (payment.getState() != Payment.PaymentState.PAID && newState == Payment.PaymentState.PAID && StringUtils.isNotEmpty(payment.getReferral())) {
         storeReferralPayment(payment);
      }

      // when PAID is set manually, ignore GoPay updates to a timeouted payment
      if (payment.getState() == Payment.PaymentState.PAID && newState == Payment.PaymentState.TIMEOUTED) {
         return payment;
      }

      payment.setState(newState);

      final Payment result = paymentDao.updatePayment(organization.getId(), payment);

      freshdeskFacade.logTicket(authenticatedUser.getCurrentUser(), "Payment status updated for organization " + organizationId,
            "Payment in amount of " + payment.getCurrency() + " " + payment.getAmount() + " is in state " + payment.getState().name() +
                  ". Invoice might need to be prepared for " + companyContactDao.getCompanyContact(organization));

      if (updateServiceLimitsEvent != null) {
         updateServiceLimitsEvent.fire(new UpdateServiceLimits(organization, getCurrentServiceLimits(organization)));
      }

      return result;
   }

   private void storeReferralPayment(final Payment payment) {
      final long months = getPaymentMonthsDuration(payment);
      if (months >= 12) {
         final String referral = payment.getReferral();
         final String userId = Utils.str36toHex(referral);
         final boolean isAffiliate = userFacade.isUserAffiliate(userId);
         final long amount = Math.round(((double) payment.getAmount() / months) * 12);
         final long commission = Math.round(isAffiliate ? amount * 0.1 : amount * 0.05);

         referralPaymentDao.createReferralPayment(new ReferralPayment(null, payment.getReferral(), commission, payment.getCurrency(), false));
      }
   }

   public Payment getPayment(final Organization organization, final String paymentId) {
      checkManagePermissions(organization);

      return paymentDao.getPayment(organization.getId(), paymentId);
   }

   public Payment checkPaymentValues(final Payment payment) {
      long months = getPaymentMonthsDuration(payment);
      final Map<String, Double> prices = months >= 12 ? DISCOUNT_PRICES : FULL_PRICES;
      long minPrice = Math.round(Math.floor(months * prices.get(payment.getCurrency()) * payment.getUsers()));

      if (payment.getAmount() < minPrice) {
         payment.setAmount(minPrice);
      }

      return payment;
   }

   private long getPaymentMonthsDuration(final Payment payment) {
      return Period.between(
            LocalDate.ofInstant(payment.getStart().toInstant(), ZoneId.systemDefault()),
            LocalDate.ofInstant(payment.getValidUntil().toInstant(), ZoneId.systemDefault())
      ).toTotalMonths();
   }

   public PaymentStats getReferralPayments() {
      User user = authenticatedUser.getCurrentUser();

      if (user != null) {
         String referenceNo = Utils.strHexTo36(user.getId());

         return paymentDao.getReferralPayments(referenceNo);
      }

      return new PaymentStats(0L, Lists.emptyList(), Lists.emptyList());
   }

   private void checkManagePermissions(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      permissionsChecker.checkRole(organization, RoleType.Manage);
   }

   private void checkReadPermissions(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      permissionsChecker.checkRole(organization, RoleType.Read);
   }
}
