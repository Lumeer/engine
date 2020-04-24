package io.lumeer.api.model;/*
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ReferralPayment {

   public static final String ID = "id";
   public static final String REFERRAL = "referral";
   public static final String AMOUNT = "amount";
   public static final String CURRENCY = "currency";
   public static final String PAID = "paid";

   private String id;
   private String referral;
   private long amount;
   private String currency;
   private boolean paid;

   @JsonCreator
   public ReferralPayment(
         @JsonProperty(ID) final String id,
         @JsonProperty(REFERRAL) final String referral,
         @JsonProperty(AMOUNT) final long amount,
         @JsonProperty(CURRENCY) final String currency,
         @JsonProperty(PAID) final boolean paid) {
      this.id = id;
      this.referral = referral;
      this.amount = amount;
      this.currency = currency;
      this.paid = paid;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getReferral() {
      return referral;
   }

   public void setReferral(final String referral) {
      this.referral = referral;
   }

   public long getAmount() {
      return amount;
   }

   public void setAmount(final long amount) {
      this.amount = amount;
   }

   public boolean isPaid() {
      return paid;
   }

   public void setPaid(final boolean paid) {
      this.paid = paid;
   }

   public String getCurrency() {
      return currency;
   }

   public void setCurrency(final String currency) {
      this.currency = currency;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final ReferralPayment that = (ReferralPayment) o;
      return Objects.equals(id, that.id);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "ReferralPayment{" +
            "id='" + id + '\'' +
            ", referral='" + referral + '\'' +
            ", amount='" + amount + '\'' +
            ", currency='" + currency + '\'' +
            ", paid=" + paid +
            '}';
   }
}
