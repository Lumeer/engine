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
package io.lumeer.storage.mongodb.dao.organization;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.paymentIdFiler;
import static io.lumeer.storage.mongodb.util.MongoFilters.paymentStateFilter;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.dao.system.SystemScopedDao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class MongoPaymentDao extends SystemScopedDao implements PaymentDao {

   private static final String PREFIX = "payments_o-";

   @Override
   public Payment createPayment(final Organization organization, final Payment payment) {
      try {
         databaseCollection(organization).insertOne(payment);
         return payment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create payment " + payment, ex);
      }
   }

   @Override
   public List<Payment> getPayments(final Organization organization) {
      return databaseCollection(organization).find().sort(Sorts.descending(Payment.DATE)).into(new ArrayList<>());
   }

   @Override
   public Payment updatePayment(final Organization organization, final String id, final Payment payment) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         final Payment returnedPayment = databaseCollection(organization).findOneAndReplace(idFilter(id), payment, options);
         if (returnedPayment == null) {
            throw new StorageException("Payment '" + id + "' has not been updated.");
         }
         return returnedPayment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update payment " + payment, ex);
      }
   }

   @Override
   public Payment updatePayment(final Organization organization, final Payment payment) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         final Payment returnedPayment = databaseCollection(organization).findOneAndReplace(paymentIdFiler(payment.getPaymentId()), payment, options);
         if (returnedPayment == null) {
            throw new StorageException("Payment '" + payment.getPaymentId() + "' has not been updated.");
         }
         return returnedPayment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update payment " + payment, ex);
      }
   }

   @Override
   public Payment getPayment(final Organization organization, final String paymentId) {
      return databaseCollection(organization).find(paymentIdFiler(paymentId)).first();
   }

   @Override
   public Payment getLatestPayment(final Organization organization) {
      return databaseCollection(organization).find(paymentStateFilter(Payment.PaymentState.PAID.ordinal()))
                                             .sort(Sorts.descending(Payment.VALID_UNTIL)).limit(1).first();
   }

   @Override
   public void createPaymentRepository(final Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      MongoCollection<Document> groupCollection = database.getCollection(databaseCollectionName(organization));
      groupCollection.createIndex(Indexes.ascending(Payment.PAYMENT_ID), new IndexOptions().unique(true));
      groupCollection.createIndex(Indexes.descending(Payment.DATE), new IndexOptions().unique(true));
      groupCollection.createIndex(Indexes.descending(Payment.START), new IndexOptions().unique(true));
      groupCollection.createIndex(Indexes.descending(Payment.VALID_UNTIL), new IndexOptions().unique(true));
   }

   @Override
   public void deletePaymentRepository(final Organization organization) {
      database.getCollection(databaseCollectionName(organization)).drop();
   }

   private MongoCollection<Payment> databaseCollection(final Organization organization) {
      return database.getCollection(databaseCollectionName(organization), Payment.class);
   }

   private String databaseCollectionName(final Organization organization) {
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return PREFIX + organization.getId();
   }
}
