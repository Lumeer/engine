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
package io.lumeer.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.lumeer.api.model.Payment;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class JsonPayment implements Payment {

   private LocalDateTime date;
   private long amount;
   private String paymentId;
   private LocalDateTime validUntil;
   private PaymentState state;
   private ServiceLevel serviceLevel;

   @JsonCreator
   public JsonPayment(@JsonProperty(DATE) LocalDateTime date, @JsonProperty(AMOUNT) long amount, @JsonProperty(PAYMENT_ID) String paymentId,
         @JsonProperty(VALID_UNTIL) LocalDateTime validUntil, @JsonProperty(STATE) PaymentState state, @JsonProperty(SERVICE_LEVEL) ServiceLevel serviceLevel) {
      this.date = date;
      this.amount = amount;
      this.paymentId = paymentId;
      this.validUntil = validUntil;
      this.state = state;
      this.serviceLevel = serviceLevel;
   }

   public JsonPayment(final Payment payment) {
      this.date = payment.getDate();
      this.amount = payment.getAmount();
      this.paymentId = payment.getPaymentId();
      this.validUntil = payment.getValidUntil();
      this.state = payment.getState();
      this.serviceLevel = payment.getServiceLevel();
   }

   @Override
   public LocalDateTime getDate() {
      return date;
   }

   @Override
   public void setDate(final LocalDateTime date) {
      this.date = date;
   }

   @Override
   public long getAmount() {
      return amount;
   }

   @Override
   public void setAmount(final long amount) {
      this.amount = amount;
   }

   @Override
   public String getPaymentId() {
      return paymentId;
   }

   @Override
   public void setPaymentId(final String paymentId) {
      this.paymentId = paymentId;
   }

   @Override
   public LocalDateTime getValidUntil() {
      return validUntil;
   }

   @Override
   public void setValidUntil(final LocalDateTime validUntil) {
      this.validUntil = validUntil;
   }

   @Override
   public PaymentState getState() {
      return state;
   }

   @Override
   public void setState(final PaymentState state) {
      this.state = state;
   }

   @Override
   public ServiceLevel getServiceLevel() {
      return serviceLevel;
   }

   @Override
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
      final JsonPayment that = (JsonPayment) o;
      return amount == that.amount &&
            Objects.equals(date, that.date) &&
            Objects.equals(paymentId, that.paymentId) &&
            Objects.equals(validUntil, that.validUntil) &&
            state == that.state &&
            serviceLevel == that.serviceLevel;
   }

   @Override
   public int hashCode() {
      return Objects.hash(date, amount, paymentId, validUntil, state, serviceLevel);
   }

   @Override
   public String toString() {
      return "JsonPayment{" +
            "date=" + date +
            ", amount=" + amount +
            ", paymentId='" + paymentId + '\'' +
            ", validUntil=" + validUntil +
            ", state=" + state +
            ", serviceLevel=" + serviceLevel +
            '}';
   }

   public static JsonPayment convert(Payment payment) {
      return payment instanceof JsonPayment ? (JsonPayment) payment : new JsonPayment(payment);
   }

   public static Set<JsonPayment> convert(Set<Payment> payments) {
      return payments.stream()
                       .map(JsonPayment::new)
                       .collect(Collectors.toSet());
   }

}
