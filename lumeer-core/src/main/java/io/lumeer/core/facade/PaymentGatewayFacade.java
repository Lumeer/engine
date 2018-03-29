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

import io.lumeer.api.model.Payment;

import cz.gopay.api.v3.GPClientException;
import cz.gopay.api.v3.IGPConnector;
import cz.gopay.api.v3.impl.resteasy.ResteasyGPConnector;
import cz.gopay.api.v3.model.access.AccessToken;
import cz.gopay.api.v3.model.access.OAuth;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class PaymentGatewayFacade {

   private static final String API_URL = "";
   private static final String CLIENT_ID = "";
   private static final String CLIENT_CREDENTIALS = "";

   private long expiration;

   private AccessToken token = null;

   private IGPConnector connector;

   private boolean tokenAllType;

   public Payment createPayment(final Payment payment) {
      ensureToken(false);

      /*
      BasePayment payment = PaymentFactory.createBasePaymentBuilder()
    .order(<ORDER_NUMBER>, <AMOUNT>, Currency.EUR, <DESCRIPTION>)
    .addItem(<ITEM_NAME>, <AMOUNT>, <FEE>, <COUNT>)
    .addAdditionalParameter(<Key>, <VALUE>)
    .withCallback(<RETURN_URL>, <NOTIFY_URL>)
    .payer(<Payer>)
    .inLang(Lang.EN)
    .toEshop(<GO_ID>)
    .build();
try {
    Payment result = connector.createPayment(payment);
} catch (GPClientException e) {

}

try {
    Payment payment = connector.paymentStatus(<PAYMENT_ID>);
} catch (GPClientException e) {
     //
}


catch (GPClientException e) {
    for (ErrorElement err : e.getError().getErrorMessages()) {
        int code = err.getErrorCode();
        String message = err.getMessage();
        String field = err.getField();

    }
       */

      return payment;
   }

   public void ensureToken(boolean allType) {
      if ((allType && !tokenAllType) || token == null || expiration < System.currentTimeMillis()) {
         connector = ResteasyGPConnector.build(API_URL);
         try {
            connector.getAppToken(CLIENT_ID, CLIENT_CREDENTIALS, allType ? OAuth.SCOPE_PAYMENT_ALL : OAuth.SCOPE_PAYMENT_CREATE);
            token = connector.getAccessToken();
            expiration = System.currentTimeMillis() + token.getExpiresIn();
            tokenAllType = allType;
         } catch (GPClientException e) {
            token = null;
            expiration = -1;
         }
      }
   }

}
