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

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.InformationRecord;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.InformationStoreDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;

public class MongoInformationStoreDao extends MongoOrganizationScopedDao implements InformationStoreDao {

   private static final String PREFIX = "informationStore_o-";

   private static final long TIME_TO_KEEP = 60L * 60 * 1000;

   @Override
   public void ensureIndexes(final Organization organization) {
      MongoCollection<Document> collection = database.getCollection(databaseCollectionName(organization));
      collection.createIndex(Indexes.ascending(InformationRecord.USER_ID), new IndexOptions().unique(true));
      collection.createIndex(Indexes.descending(InformationRecord.DATE), new IndexOptions().unique(false));
   }

   @Override
   public InformationRecord addInformation(final InformationRecord informationRecord) {
      try {
         databaseCollection().insertOne(informationRecord);

         return informationRecord;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create information record " + informationRecord, ex);
      }
   }

   @Override
   public InformationRecord findInformation(final String id, final String userId) {
      final Bson filter = Filters.and(
              idFilter(id),
              Filters.eq(InformationRecord.USER_ID, userId)
      );

      final InformationRecord result = databaseCollection().find(filter).first();

      if (result == null) {
         throw new StorageException("Information record '" + id + "' not found.");
      }

      return result;
   }

   @Override
   public void deleteInformation(final String id) {
      final DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Information record '" + id + "' has not been deleted.");
      }
   }

   @Override
   public void deleteStaleInformation() {
      final Bson filter = Filters.lt(InformationRecord.DATE, new Date(new Date().toInstant().toEpochMilli() - TIME_TO_KEEP));

      databaseCollection().deleteMany(filter);
   }

   @Override
   public void createRepository(final Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      ensureIndexes(organization);
   }

   @Override
   public void deleteRepository(final Organization organization) {
      database.getCollection(databaseCollectionName(organization)).drop();
   }

   MongoCollection<InformationRecord> databaseCollection() {
      return database.getCollection(databaseCollectionName(), InformationRecord.class);
   }

   String databaseCollectionName() {
      if (getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollectionName(getOrganization().get());
   }

   private String databaseCollectionName(final Organization organization) {
      return databaseCollectionName(organization.getId());
   }

   private String databaseCollectionName(final String organizationId) {
      return PREFIX + organizationId;
   }

}
