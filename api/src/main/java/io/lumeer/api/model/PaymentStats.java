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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class PaymentStats {

   public static class PaymentAmount {
      private long amount;
      final private String currency;

      public PaymentAmount(final long amount, final String currency) {
         this.amount = amount;
         this.currency = currency;
      }

      public long getAmount() {
         return amount;
      }

      public long addAmount(final long amount) {
         return this.amount += amount;
      }

      public String getCurrency() {
         return currency;
      }
   }

   final private long registeredUsers;
   final private java.util.Collection<PaymentAmount> commissions;
   final private java.util.Collection<PaymentAmount> paidCommissions;

   public PaymentStats(
         @JsonProperty("registeredUsers") final long registeredUsers,
         @JsonProperty("commissions") final java.util.Collection<PaymentAmount> commissions,
         @JsonProperty("paidCommissions") final java.util.Collection<PaymentAmount> paidCommissions
         ) {
      this.registeredUsers = registeredUsers;
      this.commissions = commissions;
      this.paidCommissions = paidCommissions;
   }

   public long getRegisteredUsers() {
      return registeredUsers;
   }

   public java.util.Collection<PaymentAmount> getCommissions() {
      return commissions;
   }

   public java.util.Collection<PaymentAmount> getPaidCommissions() {
      return paidCommissions;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final PaymentStats that = (PaymentStats) o;
      return registeredUsers == that.registeredUsers &&
            Objects.equals(commissions, that.commissions) &&
            Objects.equals(paidCommissions, that.paidCommissions);
   }

   @Override
   public int hashCode() {
      return Objects.hash(registeredUsers, commissions, paidCommissions);
   }

   @Override
   public String toString() {
      return "PaymentStats{" +
            "registeredUsers=" + registeredUsers +
            ", commissions=" + commissions +
            ", paidCommissions=" + paidCommissions +
            '}';
   }
}
