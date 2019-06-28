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
package io.lumeer.engine.api.event;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CreateOrUpdatePayment {

   private final Organization organization;
   private final Payment payment;

   public CreateOrUpdatePayment(final Organization organization, final Payment payment) {
      this.organization = organization;
      this.payment = payment;
   }

   public Payment getPayment() {
      return payment;
   }

   public Organization getOrganization() {
      return organization;
   }
}
