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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Payment {

   public static final String DATE = "date";
   public static final String AMOUNT = "amount";
   public static final String PAYMENT_ID = "paymentId";
   public static final String START = "start";
   public static final String VALID_UNTIL = "validUntil";
   public static final String STATE = "state";
   public static final String SERVICE_LEVEL = "serviceLevel";

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

   private LocalDateTime date;
   private long amount;
   private String paymentId;
   private LocalDateTime start;
   private LocalDateTime validUntil;
   private PaymentState state;
   private ServiceLevel serviceLevel;

   @JsonCreator
   public Payment(@JsonProperty(DATE) LocalDateTime date, @JsonProperty(AMOUNT) long amount, @JsonProperty(PAYMENT_ID) String paymentId,
         @JsonProperty(START) LocalDateTime start, @JsonProperty(VALID_UNTIL) LocalDateTime validUntil, @JsonProperty(STATE) PaymentState state,
         @JsonProperty(SERVICE_LEVEL) ServiceLevel serviceLevel) {
      this.date = date;
      this.amount = amount;
      this.paymentId = paymentId;
      this.start = start;
      this.validUntil = validUntil;
      this.state = state;
      this.serviceLevel = serviceLevel;
   }

   public LocalDateTime getDate() {
      return date;
   }

   public void setDate(final LocalDateTime date) {
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

   public LocalDateTime getStart() {
      return start;
   }

   public void setStart(final LocalDateTime start) {
      this.start = start;
   }

   public LocalDateTime getValidUntil() {
      return validUntil;
   }

   public void setValidUntil(final LocalDateTime validUntil) {
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

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Payment that = (Payment) o;
      return amount == that.amount &&
            Objects.equals(date, that.date) &&
            Objects.equals(paymentId, that.paymentId) &&
            Objects.equals(start, that.start) &&
            Objects.equals(validUntil, that.validUntil) &&
            state == that.state &&
            serviceLevel == that.serviceLevel;
   }

   @Override
   public int hashCode() {
      return Objects.hash(date, amount, paymentId, start, validUntil, state, serviceLevel);
   }

   @Override
   public String toString() {
      return "Payment{" +
            "date=" + date +
            ", amount=" + amount +
            ", paymentId='" + paymentId + '\'' +
            ", start=" + start +
            ", validUntil=" + validUntil +
            ", state=" + state +
            ", serviceLevel=" + serviceLevel +
            '}';
   }

}
