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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;

import java.util.Date;
import java.util.List;

public interface PaymentDao extends OrganizationScopedDao {

   Payment createPayment(final Organization organization, final Payment payment);

   List<Payment> getPayments(final Organization organization);

   /* Uses database id */
   Payment updatePayment(final Organization organization, final String id, final Payment payment);

   /* Uses payment id */
   Payment updatePayment(final Organization organization, final Payment payment);

   Payment getPayment(final Organization organization, final String paymentId);

   Payment getPaymentByDbId(final Organization organization, final String id);

   Payment getPaymentAt(final Organization organization, final Date date);

   Payment getLatestPayment(final Organization organization);
}
