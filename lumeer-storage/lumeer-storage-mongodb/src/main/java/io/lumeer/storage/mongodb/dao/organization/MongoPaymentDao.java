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
package io.lumeer.storage.mongodb.dao.organization;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.PaymentStats;
import io.lumeer.engine.api.event.CreateOrUpdatePayment;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ReferralPaymentDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.PaymentCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@RequestScoped
public class MongoPaymentDao extends MongoOrganizationScopedDao implements PaymentDao {

   private static final String PREFIX = "payments_o-";

   @Inject
   private Event<CreateOrUpdatePayment> createOrUpdatePaymentEvent;

   @Inject
   private ReferralPaymentDao referralPaymentDao;

   @Inject
   private UserDao userDao;

   @Override
   public Payment createPayment(final String organizationId, final Payment payment) {
      try {
         databaseCollection(organizationId).insertOne(payment);
         if (createOrUpdatePaymentEvent != null) {
            createOrUpdatePaymentEvent.fire(new CreateOrUpdatePayment(organizationId, payment));
         }
         return payment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create payment " + payment, ex);
      }
   }

   @Override
   public List<Payment> getPayments(final String organizationId) {
      return databaseCollection(organizationId).find().sort(Sorts.descending(Payment.DATE)).into(new ArrayList<>());
   }

   @Override
   public Payment updatePayment(final String organizationId, final String id, final Payment payment) {
      return updatePayment(organizationId, payment, MongoFilters.idFilter(id));
   }

   @Override
   public Payment updatePayment(final String organizationId, final Payment payment) {
      return updatePayment(organizationId, payment, paymentIdFilter(payment.getPaymentId()));
   }

   private Payment updatePayment(final String organizationId, final Payment payment, final Bson filter) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
      try {
         Bson update = new Document("$set", payment).append("$inc", new Document(PaymentCodec.VERSION, 1L));
         final Payment returnedPayment = databaseCollection(organizationId).findOneAndUpdate(filter, update, options);
         if (returnedPayment == null) {
            throw new StorageException("Payment '" + payment.getId() + "' has not been updated.");
         }
         if (createOrUpdatePaymentEvent != null) {
            createOrUpdatePaymentEvent.fire(new CreateOrUpdatePayment(organizationId, returnedPayment));
         }
         return returnedPayment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update payment " + payment, ex);
      }
   }

   @Override
   public Payment getPayment(final String organizationId, final String paymentId) {
      return databaseCollection(organizationId).find(paymentIdFilter(paymentId)).first();
   }

   @Override
   public Payment getPaymentByDbId(final String organizationId, final String id) {
      return databaseCollection(organizationId).find(MongoFilters.idFilter(id)).first();
   }

   @Override
   public Payment getLatestPayment(final String organizationId) {
      return databaseCollection(organizationId).find(paymentStateFilter(Payment.PaymentState.PAID.ordinal()))
                                             .sort(Sorts.descending(PaymentCodec.VALID_UNTIL)).limit(1).first();
   }

   public PaymentStats getReferralPayments(final String referral) {
      final Map<String, PaymentStats.PaymentAmount> commissions = new HashMap<>();
      final Map<String, PaymentStats.PaymentAmount> paidComissions = new HashMap<>();
      long count = userDao.getReferralsCount(referral);

      referralPaymentDao.getReferralPayments(referral).iterator().forEachRemaining(payment -> {
         var counter = payment.isPaid() ? paidComissions : commissions;
         counter.computeIfAbsent(payment.getCurrency(), currency -> new PaymentStats.PaymentAmount(0L, currency))
                .addAmount(payment.getAmount());
      });

      return new PaymentStats(count, commissions.values(), paidComissions.values());
   }

   @Override
   public Payment getPaymentAt(final String organizationId, final Date date) {
      return databaseCollection(organizationId)
            .find(Filters.and(paymentStateFilter(Payment.PaymentState.PAID.ordinal()),
                  paymentValidUntilFilter(date), paymentStartFilter(date)))
            .sort(Sorts.descending(PaymentCodec.VALID_UNTIL)).limit(1).first();
   }

   private Bson paymentIdFilter(final String paymentId) {
      return Filters.eq(Payment.PAYMENT_ID, paymentId);
   }

   private Bson paymentStateFilter(final int stateId) {
      return Filters.eq(Payment.STATE, stateId);
   }

   private Bson paymentValidUntilFilter(final Date date) {
      return Filters.gte(Payment.VALID_UNTIL, date);
   }

   private Bson paymentStartFilter(final Date date) {
      return Filters.lte(Payment.START, date);
   }

   @Override
   public void createRepository(final Organization organization) {
      database.createCollection(databaseCollectionName(organization.getId()));

      MongoCollection<Document> groupCollection = database.getCollection(databaseCollectionName(organization.getId()));
      groupCollection.createIndex(Indexes.ascending(PaymentCodec.PAYMENT_ID), new IndexOptions().unique(false));
      groupCollection.createIndex(Indexes.descending(PaymentCodec.DATE), new IndexOptions().unique(true));
      groupCollection.createIndex(Indexes.descending(PaymentCodec.START), new IndexOptions().unique(true));
      groupCollection.createIndex(Indexes.descending(PaymentCodec.VALID_UNTIL), new IndexOptions().unique(true));
      groupCollection.createIndex(Indexes.ascending(PaymentCodec.REFERRAL), new IndexOptions().unique(false));
   }

   @Override
   public void deleteRepository(final Organization organization) {
      database.getCollection(databaseCollectionName(organization.getId())).drop();
   }

   private MongoCollection<Payment> databaseCollection(final String organizationId) {
      return database.getCollection(databaseCollectionName(organizationId), Payment.class);
   }

   private String databaseCollectionName(final String organizationId) {
      return PREFIX + organizationId;
   }
}
