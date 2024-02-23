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

package io.lumeer.core.adapter

import io.lumeer.api.model.Organization
import io.lumeer.api.model.Payment
import io.lumeer.api.model.ServiceLimits
import io.lumeer.engine.api.cache.Cache
import io.lumeer.storage.api.dao.PaymentDao
import java.util.Date
import java.util.concurrent.TimeUnit

class PaymentAdapter(private val paymentDao: PaymentDao, private val paymentCache: Cache<Payment>? = null) {

   fun computeServiceLimits(organization: Organization, skipPayments: Boolean = false): ServiceLimits {
      val payment = getCurrentPayment(organization)
      val validUntil = getValidUntil(getFutureContinuousPayments(organization, Date()))
      if (payment != null && validUntil != null && payment.serviceLevel == Payment.ServiceLevel.BASIC) {
         return computeServiceLimits(payment, validUntil)
      }
      return if (skipPayments) {
         ServiceLimits.BASIC_LIMITS
      } else ServiceLimits.FREE_LIMITS
   }

   fun computeServiceLimitsAt(organization: Organization, date: Date): ServiceLimits {
      val payment = getPaymentAt(organization, date)
      val validUntil = getValidUntil(getFutureContinuousPayments(organization, date))
      if (payment != null && validUntil != null) {
         if (payment.serviceLevel == Payment.ServiceLevel.BASIC) {
            return computeServiceLimits(payment, validUntil)
         }
      }
      return ServiceLimits.FREE_LIMITS
   }

   private fun getCurrentPayment(organization: Organization): Payment? {
      if (paymentCache != null) {
         return paymentCache.computeIfAbsent(organization.id) { getPaymentAt(organization, Date()) }
      }
      return getPaymentAt(organization, Date())
   }

   private fun getValidUntil(continuousPayments: List<Payment>): Date? {
      val time = continuousPayments.maxOfOrNull { it.validUntil.time }
      return if (time != null && time > 0) Date(time) else null
   }

   fun getFutureContinuousPayments(organization: Organization, date: Date): List<Payment> {
      val result = arrayListOf<Payment>()
      var payment = getPaymentAt(organization, date)
      while (payment != null) {
         result.add(payment)
         payment = getPaymentAt(organization, Date(payment.validUntil.time + TimeUnit.MINUTES.toMillis(1)))
      }
      return result
   }

   private fun getPaymentAt(organization: Organization, date: Date): Payment? {
      val payment = paymentDao.getPaymentAt(organization.id, date)

      // is the payment active? be tolerant to dates/time around the interval border
      return if (payment != null && date.before(Date(payment.validUntil.time + TimeUnit.SECONDS.toMillis(1)))
         && date.after(Date(payment.start.time - TimeUnit.SECONDS.toMillis(1)))
      ) {
         payment
      } else null
   }

   private fun computeServiceLimits(payment: Payment, validUntil: Date): ServiceLimits {
      val fileSizeDb = payment.getParamInt(Payment.PaymentParam.MAX_CREATED_RECORDS, ServiceLimits.BASIC_LIMITS.fileSizeMb)
      val auditDays = payment.getParamInt(Payment.PaymentParam.AUDIT_DAYS, ServiceLimits.BASIC_LIMITS.auditDays)
      val maxCreatedRecords = payment.getParamInt(Payment.PaymentParam.MAX_CREATED_RECORDS, ServiceLimits.BASIC_LIMITS.maxCreatedRecords)
      val maxViewReadRecords = payment.getParamInt(Payment.PaymentParam.MAX_VIEW_READ_RECORDS, ServiceLimits.BASIC_LIMITS.maxViewReadRecords)
      val automationTimeout = payment.getParamInt(Payment.PaymentParam.AUTOMATION_TIMEOUT, ServiceLimits.BASIC_LIMITS.automationTimeout)
      return ServiceLimits(
         Payment.ServiceLevel.BASIC, ServiceLimits.BASIC_LIMITS.users.coerceAtMost(payment.users),
         ServiceLimits.BASIC_LIMITS.projects, ServiceLimits.BASIC_LIMITS.files, ServiceLimits.BASIC_LIMITS.documents,
         ServiceLimits.BASIC_LIMITS.dbSizeMb, validUntil, ServiceLimits.BASIC_LIMITS.rulesPerCollection,
         ServiceLimits.BASIC_LIMITS.functionsPerCollection, ServiceLimits.BASIC_LIMITS.isGroups, fileSizeDb, auditDays, maxCreatedRecords,
         maxViewReadRecords, automationTimeout
      )
   }

}
