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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.lumeer.api.model.Payment;
import io.lumeer.api.model.Payments;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class JsonPayments implements Payments {

   public static final String PAYMENTS = "payments";

   @JsonProperty(PAYMENTS)
   private final Set<JsonPayment> payments;

   public JsonPayments() {
      this.payments = new HashSet<>();
   }

   public JsonPayments(final Payments payments) {
      this.payments = JsonPayment.convert(payments.getPayments());
   }

   @JsonCreator
   public JsonPayments(@JsonProperty(PAYMENTS) final Set<JsonPayment> payments) {
      this.payments = payments;
   }

   @Override
   public Set<Payment> getPayments() {
      return Collections.unmodifiableSet(payments);
   }

   @Override
   public void addPayment(final Payment payment) {
      payments.add(JsonPayment.convert(payment));
   }

   public static JsonPayments convert(Payments payments) {
      return payments instanceof JsonPayments ? (JsonPayments) payments : new JsonPayments(payments);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final JsonPayments that = (JsonPayments) o;
      return Objects.equals(payments, that.payments);
   }

   @Override
   public int hashCode() {

      return Objects.hash(payments);
   }

   @Override
   public String toString() {
      return "JsonPayments{" +
            "payments=" + payments +
            '}';
   }
}
