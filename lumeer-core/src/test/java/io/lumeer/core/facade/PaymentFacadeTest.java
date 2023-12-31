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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Payment;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PaymentFacadeTest {

   private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

   @Test
   public void checkPaymentValues() {
      assertThat(
            Period.between(
                  LocalDate.of(2018, 5,31),
                  LocalDate.of(2018, 12, 1)
            ).getMonths()).isEqualTo(6);
      assertThat(
            Period.between(
                  LocalDate.of(2018, 4,30),
                  LocalDate.of(2018, 10, 30)
            ).getMonths()).isEqualTo(6);
      assertThat(
            Period.between(
                  LocalDate.of(2018, 5,31),
                  LocalDate.of(2018, 11, 30)
            ).getMonths()).isEqualTo(5);
      assertThat(
            Period.between(
                  LocalDate.of(2018, 5,31),
                  LocalDate.of(2018, 12, 31)
            ).getMonths()).isEqualTo(7);

      final PaymentFacade paymentFacade = new PaymentFacade();

      assertThat(paymentFacade.checkPaymentValues(new Payment("x",
            getDate("2018-05-31T00:00:00.000+0100"),
            Math.round(10 * 6 * 11.50),
            "id",
            getDate("2018-04-30T00:00:00.000+0100"),
            getDate("2018-10-30T00:00:00.000+0100"),
            Payment.PaymentState.CREATED,
            Payment.ServiceLevel.BASIC,
            10,
            "en",
            "EUR",
            "url",
            null
            )).getAmount()).isEqualTo(Math.round(10 * 6 * 11.50));

      assertThat(paymentFacade.checkPaymentValues(new Payment("x",
            getDate("2018-05-31T00:00:00.000+0100"),
            Math.round(10 * 12 * 9.00),
            "id",
            getDate("2018-04-30T00:00:00.000+0100"),
            getDate("2019-04-30T00:00:00.000+0100"),
            Payment.PaymentState.CREATED,
            Payment.ServiceLevel.BASIC,
            10,
            "en",
            "EUR",
            "url",
            null
      )).getAmount()).isEqualTo(Math.round(10 * 12 * 9.00));

      assertThat(paymentFacade.checkPaymentValues(new Payment("x",
            getDate("2018-05-31T00:00:00.000+0100"),
            Math.round(10 * 5 * 11.50),
            "id",
            getDate("2018-04-30T00:00:00.000+0100"),
            getDate("2018-10-30T00:00:00.000+0100"),
            Payment.PaymentState.CREATED,
            Payment.ServiceLevel.BASIC,
            10,
            "en",
            "EUR",
            "url",
            null
      )).getAmount()).isEqualTo(Math.round(10 * 6 * 11.50));

      assertThat(paymentFacade.checkPaymentValues(new Payment("x",
            getDate("2018-05-31T00:00:00.000+0100"),
            Math.round(10 * 6 * 11.50) - 1,
            "id",
            getDate("2018-04-30T00:00:00.000+0100"),
            getDate("2018-10-30T00:00:00.000+0100"),
            Payment.PaymentState.CREATED,
            Payment.ServiceLevel.BASIC,
            10,
            "en",
            "EUR",
            "url",
            null
      )).getAmount()).isEqualTo(Math.round(10 * 6 * 11.50));
   }

   private Date getDate(final String date) {
      return Date.from(Instant.from(DTF.parse(date)));
   }

}
