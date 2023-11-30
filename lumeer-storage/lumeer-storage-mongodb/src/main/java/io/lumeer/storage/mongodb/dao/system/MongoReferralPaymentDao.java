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
package io.lumeer.storage.mongodb.dao.system;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.ReferralPayment;
import io.lumeer.storage.api.dao.ReferralPaymentDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.ReferralPaymentCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoReferralPaymentDao extends MongoSystemScopedDao implements ReferralPaymentDao {

   private static final String COLLECTION_NAME = "referralPayments";

   @PostConstruct
   public void checkRepository() {
      createReferralPaymentsRepository();
   }

   public void createReferralPaymentsRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(databaseCollectionName())) {
         database.createCollection(databaseCollectionName());
      }
      MongoCollection<Document> referralPaymentCollection = database.getCollection(databaseCollectionName());
      referralPaymentCollection.createIndex(Indexes.ascending(ReferralPayment.REFERRAL), new IndexOptions().unique(false));
   }

   public void deleteUsersRepository() {
      database.getCollection(databaseCollectionName()).drop();
   }


   @Override
   public ReferralPayment createReferralPayment(final ReferralPayment referralPayment) {
      try {
         databaseCollection().insertOne(referralPayment);
         return referralPayment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create ReferralPayment " + referralPayment, ex);
      }
   }

   @Override
   public ReferralPayment getReferralPayment(final String id) {
      return databaseCollection().find(idFilter(id)).first();
   }

   @Override
   public ReferralPayment patchReferralPayment(final String id, final ReferralPayment referralPayment) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         ReferralPayment returnedReferralPayment = databaseCollection().findOneAndReplace(idFilter(id), referralPayment, options);
         if (returnedReferralPayment == null) {
            throw new StorageException("ReferralPayment '" + id + "' has not been updated.");
         }
         return returnedReferralPayment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update ReferralPayment " + referralPayment, ex);
      }
   }

   @Override
   public List<ReferralPayment> getReferralPayments(final String referral) {
      Bson filters = Filters.eq(ReferralPaymentCodec.REFERRAL, referral);

      return databaseCollection().find(filters).into(new ArrayList<>());
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<ReferralPayment> databaseCollection() {
      return database.getCollection(databaseCollectionName(), ReferralPayment.class);
   }

}
