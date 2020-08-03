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

import io.lumeer.api.model.Payment;
import io.lumeer.core.exception.PaymentGatewayException;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import cz.gopay.api.v3.GPClientException;
import cz.gopay.api.v3.IGPConnector;
import cz.gopay.api.v3.impl.resteasy.ResteasyGPConnector;
import cz.gopay.api.v3.model.access.AccessToken;
import cz.gopay.api.v3.model.access.OAuth;
import cz.gopay.api.v3.model.common.Currency;
import cz.gopay.api.v3.model.payment.BasePayment;
import cz.gopay.api.v3.model.payment.Lang;
import cz.gopay.api.v3.model.payment.PaymentFactory;
import cz.gopay.api.v3.model.payment.support.ItemType;

@ApplicationScoped
public class PaymentGatewayFacade {

   private static String GOPAY_API = "https://gw.sandbox.gopay.com/api";

   private static String CLIENT_ID = "";
   private static String CLIENT_CREDENTIALS = "";
   private static long GOPAY_ID = 0L;
   private static final Map<String, String> ORDER_FORMAT;

   static {
      ORDER_FORMAT = new HashMap<>();
      ORDER_FORMAT.put("cs", "Služba Lumeer %s pro %d uživatelů od %s do %s.");
      ORDER_FORMAT.put("en", "Lumeer %s service for %d users since %s until %s.");
   }

   private boolean dryRun = true;

   private long expiration;

   private AccessToken token = null;

   private IGPConnector connector;

   private boolean tokenAllType;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @PostConstruct
   public void init() {
      GOPAY_API = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.GOPAY_API)).orElse("");
      GOPAY_ID = Long.valueOf(Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.GOPAY_ID)).orElse("0"));
      CLIENT_ID = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.GOPAY_CLIENT_ID)).orElse("");
      CLIENT_CREDENTIALS = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.GOPAY_CLIENT_CREDENTIALS)).orElse("");

      if (!configurationFacade.getEnvironment().equals(ConfigurationFacade.DeployEnvironment.DEVEL)) {
         setDryRun(false);
      }
   }

   public Payment createPayment(final Payment payment, final String returnUrl, final String notifyUrl) {
      if (dryRun) {
         payment.setState(Payment.PaymentState.CREATED);
         payment.setPaymentId(String.valueOf(new Random().nextLong()));
         return payment;
      }

      ensureToken(false);

      final String description = getPaymentDescription(payment);
      final BasePayment basePayment = PaymentFactory.createBasePaymentBuilder()
            .order(payment.getId(), payment.getAmount() * 100L,
                  Currency.getByCode(payment.getCurrency()), description)
            .addItem(description, payment.getAmount() * 100L, 1L, 21, ItemType.ITEM, "", "")
            .withCallback(returnUrl, notifyUrl)
            .inLang("cz".equals(payment.getLanguage()) ? Lang.CS : Lang.EN)
            .toEshop(GOPAY_ID)
            .build();

      try {
         cz.gopay.api.v3.model.payment.Payment createdPayment = connector.createPayment(basePayment);
         payment.setState(convertState(createdPayment.getState()));
         payment.setPaymentId(String.valueOf(createdPayment.getId()));
         payment.setGwUrl(createdPayment.getGwUrl());
      } catch (GPClientException e) {
         throw new PaymentGatewayException(e);
      }

      return payment;
   }

   private String getPaymentDescription(final Payment payment) {
      final DateFormat df;
      if ("cs".equals(payment.getLanguage())) {
         df = SimpleDateFormat.getDateInstance(DateFormat.SHORT, Locale.forLanguageTag("cs_CZ"));
      } else {
         df = SimpleDateFormat.getDateInstance(DateFormat.SHORT, Locale.ENGLISH);
      }

      return String.format(ORDER_FORMAT.get(payment.getLanguage()), payment.getServiceLevel().toString(), payment.getUsers(),
            df.format(payment.getStart()), df.format(payment.getValidUntil()));
   }

   public Payment.PaymentState getPaymentStatus(final String paymentId) {
      if (dryRun) {
         return Payment.PaymentState.PAID;
      }

      ensureToken(true);

      try {
         return convertState(connector.paymentStatus(Long.valueOf(paymentId)).getState());
      } catch (GPClientException e) {
         throw new PaymentGatewayException(e);
      }
   }

   private Payment.PaymentState convertState(final cz.gopay.api.v3.model.payment.Payment.SessionState state) {
      switch (state) {
         case CREATED:
            return Payment.PaymentState.CREATED;
         case PAID:
            return Payment.PaymentState.PAID;
         case CANCELED:
            return Payment.PaymentState.CANCELED;
         case PAYMENT_METHOD_CHOSEN:
            return Payment.PaymentState.PAYMENT_METHOD_CHOSEN;
         case AUTHORIZED:
            return Payment.PaymentState.AUTHORIZED;
         case REFUNDED:
            return Payment.PaymentState.REFUNDED;
         case TIMEOUTED:
            return Payment.PaymentState.TIMEOUTED;
         case PARTIALLY_REFUNDED:
            return Payment.PaymentState.REFUNDED;
         default:
            return Payment.PaymentState.TIMEOUTED;
      }
   }

   public void ensureToken(boolean allType) {
      if (dryRun) {
         return;
      }

      if ((allType && !tokenAllType) || token == null || expiration < System.currentTimeMillis()) {
         connector = ResteasyGPConnector.build(GOPAY_API);
         try {
            connector.getAppToken(CLIENT_ID, CLIENT_CREDENTIALS, allType ? OAuth.SCOPE_PAYMENT_ALL : OAuth.SCOPE_PAYMENT_CREATE);
            token = connector.getAccessToken();
            expiration = System.currentTimeMillis() + token.getExpiresIn();
            tokenAllType = allType;
         } catch (GPClientException e) {
            token = null;
            expiration = -1;
            throw new PaymentGatewayException(e);
         }
      }
   }

   public void setDryRun(final boolean dryRun) {
      this.dryRun = dryRun;
   }

}
