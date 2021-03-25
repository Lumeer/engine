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
package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;

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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MongoAuditRecordDao extends MongoProjectScopedDao implements AuditDao {

   private static final String PREFIX = "auditlog_p-";

   @Override
   public void createRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));
      ensureIndexes(project);
   }

   @Override
   public void deleteRepository(final Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public void ensureIndexes(final Project project) {
      MongoCollection<Document> auditLogCollection = database.getCollection(databaseCollectionName(project));

      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.RESOUCE_ID), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOUCE_ID), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOUCE_ID, AuditRecord.USER), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.CHANGE_DATE), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOUCE_ID, AuditRecord.CHANGE_DATE), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.CHANGE_DATE), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOUCE_ID, AuditRecord.CHANGE_DATE, AuditRecord.USER), new IndexOptions().unique(false));
   }


   @Override
   public AuditRecord findLatestAuditRecord(final String parentId, final ResourceType resourceType, final String resourceId) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOUCE_ID, resourceId)
      );

      return databaseCollection().find(filters).sort(Sorts.descending(AuditRecord.CHANGE_DATE)).limit(1).first();
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOUCE_ID, resourceId)
      );

      return databaseCollection().find(filters).sort(Sorts.descending(AuditRecord.CHANGE_DATE)).into(new ArrayList<>());
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final ZonedDateTime noOlderThan) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOUCE_ID, resourceId),
            Filters.gte(AuditRecord.CHANGE_DATE, Date.from(noOlderThan.toInstant()))
      );

      return databaseCollection().find(filters).sort(Sorts.descending(AuditRecord.CHANGE_DATE)).into(new ArrayList<>());
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final int countLimit) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOUCE_ID, resourceId)
      );

      return databaseCollection().find(filters).sort(Sorts.descending(AuditRecord.CHANGE_DATE)).limit(countLimit).into(new ArrayList<>());
   }

   @Override
   public AuditRecord createAuditRecord(final AuditRecord record) {
      try {
         databaseCollection().insertOne(record);

         return record;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create audit log record: " + record, ex);
      }
   }

   @Override
   public AuditRecord updateAuditRecord(final AuditRecord record) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Bson update = new Document("$set", record);
         final AuditRecord updatedRecord = databaseCollection().findOneAndUpdate(idFilter(record.getId()), update, options);
         if (updatedRecord == null) {
            throw new StorageException("Audit log record '" + record.getId() + "' has not been updated.");
         }

         return updatedRecord;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update audit log record: " + record, ex);
      }
   }

   @Override
   public void deleteAuditRecord(final String id) {
      final AuditRecord record = databaseCollection().findOneAndDelete(idFilter(id));
      if (record == null) {
         throw new StorageException("Audit log record '" + id + "' has not been deleted.");
      }
   }

   @Override
   public void cleanAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final ZonedDateTime cleanOlderThan) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOUCE_ID, resourceId),
            Filters.lt(AuditRecord.CHANGE_DATE, Date.from(cleanOlderThan.toInstant()))
      );

      databaseCollection().deleteMany(filters);
   }

   private String databaseCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   String databaseCollectionName() {
      if (getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollectionName(getProject().get());
   }

   MongoCollection<AuditRecord> databaseCollection() {
      return database.getCollection(databaseCollectionName(), AuditRecord.class);
   }

}
