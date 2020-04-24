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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Objects;

public class Payment {

   public static final String ID = "id";
   public static final String DATE = "date";
   public static final String AMOUNT = "amount";
   public static final String PAYMENT_ID = "paymentId";
   public static final String START = "start";
   public static final String VALID_UNTIL = "validUntil";
   public static final String STATE = "state";
   public static final String SERVICE_LEVEL = "serviceLevel";
   public static final String USERS = "users";
   public static final String LANGUAGE = "language";
   public static final String CURRENCY = "currency";
   public static final String GW_URL = "gwUrl";
   public static final String REFERRAL = "referral";

   public enum PaymentState {
      CREATED, PAYMENT_METHOD_CHOSEN, AUTHORIZED, PAID, CANCELED, TIMEOUTED, REFUNDED;

      public static PaymentState fromInt(int i) {
         if (i < 0 || i > PaymentState.values().length) {
            throw new IndexOutOfBoundsException("There is no such PaymentState with index " + i);
         }

         return values()[i];
      }
   }

   public enum ServiceLevel {
      FREE, BASIC, CORPORATE;

      public static ServiceLevel fromInt(int i) {
         if (i < 0 || i > ServiceLevel.values().length) {
            throw new IndexOutOfBoundsException("There is no such ServiceLevel with index " + i);
         }

         return values()[i];
      }
   }

   private String id;
   private Date date;
   private long amount;
   private String paymentId;
   private Date start;
   private Date validUntil;
   private PaymentState state;
   private ServiceLevel serviceLevel;
   private int users;
   private String language;
   private String currency;
   private String gwUrl;
   private long version;
   private String referral;

   @JsonCreator
   public Payment(@JsonProperty(ID) final String id,
         @JsonProperty(DATE) Date date, @JsonProperty(AMOUNT) long amount, @JsonProperty(PAYMENT_ID) String paymentId,
         @JsonProperty(START) Date start, @JsonProperty(VALID_UNTIL) Date validUntil, @JsonProperty(STATE) PaymentState state,
         @JsonProperty(SERVICE_LEVEL) ServiceLevel serviceLevel, @JsonProperty(USERS) int users,
         @JsonProperty(LANGUAGE) final String language, @JsonProperty(CURRENCY) final String currency,
         @JsonProperty(GW_URL) final String gwUrl, @JsonProperty(REFERRAL) final String referral) {
      this.id = id;
      this.date = date;
      this.amount = amount;
      this.paymentId = paymentId;
      this.start = start;
      this.validUntil = validUntil;
      this.state = state;
      this.serviceLevel = serviceLevel;
      this.users = users;
      this.language = language;
      this.currency = currency;
      this.gwUrl = gwUrl;
      this.referral = referral;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public Date getDate() {
      return date;
   }

   public void setDate(final Date date) {
      this.date = date;
   }

   public long getAmount() {
      return amount;
   }

   public void setAmount(final long amount) {
      this.amount = amount;
   }

   public String getPaymentId() {
      return paymentId;
   }

   public void setPaymentId(final String paymentId) {
      this.paymentId = paymentId;
   }

   public Date getStart() {
      return start;
   }

   public void setStart(final Date start) {
      this.start = start;
   }

   public Date getValidUntil() {
      return validUntil;
   }

   public void setValidUntil(final Date validUntil) {
      this.validUntil = validUntil;
   }

   public PaymentState getState() {
      return state;
   }

   public void setState(final PaymentState state) {
      this.state = state;
   }

   public ServiceLevel getServiceLevel() {
      return serviceLevel;
   }

   public void setServiceLevel(final ServiceLevel serviceLevel) {
      this.serviceLevel = serviceLevel;
   }

   public int getUsers() {
      return users;
   }

   public void setUsers(final int users) {
      this.users = users;
   }

   public String getLanguage() {
      return language;
   }

   public void setLanguage(final String language) {
      this.language = language;
   }

   public String getCurrency() {
      return currency;
   }

   public void setCurrency(final String currency) {
      this.currency = currency;
   }

   public String getGwUrl() {
      return gwUrl;
   }

   public void setGwUrl(final String gwUrl) {
      this.gwUrl = gwUrl;
   }

   public long getVersion() {
      return version;
   }

   public void setVersion(final long version) {
      this.version = version;
   }

   public String getReferral() {
      return referral;
   }

   public void setReferral(final String referral) {
      this.referral = referral;
   }

   @Override
   public String toString() {
      return "Payment{" +
            "id='" + id + '\'' +
            ", date=" + date +
            ", amount=" + amount +
            ", paymentId='" + paymentId + '\'' +
            ", start=" + start +
            ", validUntil=" + validUntil +
            ", state=" + state +
            ", serviceLevel=" + serviceLevel +
            ", users=" + users +
            ", language='" + language + '\'' +
            ", currency='" + currency + '\'' +
            ", gwUrl='" + gwUrl + '\'' +
            ", referral='" + referral + '\'' +
            '}';
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Payment payment = (Payment) o;
      return amount == payment.amount &&
            users == payment.users &&
            Objects.equals(id, payment.id) &&
            Objects.equals(date, payment.date) &&
            Objects.equals(paymentId, payment.paymentId) &&
            Objects.equals(start, payment.start) &&
            Objects.equals(validUntil, payment.validUntil) &&
            state == payment.state &&
            serviceLevel == payment.serviceLevel &&
            Objects.equals(language, payment.language) &&
            Objects.equals(currency, payment.currency) &&
            Objects.equals(referral, payment.referral) &&
            Objects.equals(gwUrl, payment.gwUrl);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, date, amount, paymentId, start, validUntil, state, serviceLevel, users, language, currency, gwUrl);
   }
}
