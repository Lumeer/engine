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
import io.lumeer.api.model.AuditType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.AuditRecordCodec;

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
import java.util.Set;

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

      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.RESOURCE_ID), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOURCE_ID), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOURCE_ID, AuditRecord.USER), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.CHANGE_DATE), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOURCE_ID, AuditRecord.CHANGE_DATE), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.CHANGE_DATE), new IndexOptions().unique(false));
      auditLogCollection.createIndex(Indexes.ascending(AuditRecord.RESOURCE_TYPE, AuditRecord.PARENT_ID, AuditRecord.RESOURCE_ID, AuditRecord.CHANGE_DATE, AuditRecord.USER), new IndexOptions().unique(false));
   }

   @Override
   public AuditRecord findLatestAuditRecord(final String parentId, final ResourceType resourceType, final String resourceId) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOURCE_ID, resourceId)
      );

      return findAuditRecords(filters, 1).stream().findFirst().orElse(null);
   }

   @Override
   public AuditRecord findLatestAuditRecord(final String parentId, final ResourceType resourceType, final String resourceId, final AuditType type) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOURCE_ID, resourceId),
            Filters.or(Filters.eq(AuditRecord.TYPE, type.toString()), Filters.eq(AuditRecord.TYPE, null))
      );

      return findAuditRecords(filters, 1).stream().findFirst().orElse(null);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final ZonedDateTime noOlderThan) {
      final Bson filters = Filters.and(
            projectFilter(collectionIds, linkTypeIds, viewIds),
            Filters.gte(AuditRecord.CHANGE_DATE, Date.from(noOlderThan.toInstant()))
      );

      return findAuditRecords(filters, -1);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String userId, final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final ZonedDateTime noOlderThan) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.USER, userId),
            projectFilter(collectionIds, linkTypeIds, viewIds),
            Filters.gte(AuditRecord.CHANGE_DATE, Date.from(noOlderThan.toInstant()))
      );

      return findAuditRecords(filters, -1);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final int countLimit) {
      final Bson filters = projectFilter(collectionIds, linkTypeIds, viewIds);

      return findAuditRecords(filters, countLimit);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String userId, final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final int countLimit) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.USER, userId),
            projectFilter(collectionIds, linkTypeIds, viewIds)
      );

      return findAuditRecords(filters, countLimit);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final ZonedDateTime noOlderThan) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.gte(AuditRecord.CHANGE_DATE, Date.from(noOlderThan.toInstant()))
      );

      return findAuditRecords(filters, -1);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final int countLimit) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId)
      );

      return findAuditRecords(filters, countLimit);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final ZonedDateTime noOlderThan) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOURCE_ID, resourceId),
            Filters.gte(AuditRecord.CHANGE_DATE, Date.from(noOlderThan.toInstant()))
      );

      return findAuditRecords(filters, -1);
   }

   @Override
   public List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final int countLimit) {
      final Bson filters = Filters.and(
            Filters.eq(AuditRecord.RESOURCE_TYPE, resourceType.toString()),
            Filters.eq(AuditRecord.PARENT_ID, parentId),
            Filters.eq(AuditRecord.RESOURCE_ID, resourceId)
      );

      return findAuditRecords(filters, countLimit);
   }

   private List<AuditRecord> findAuditRecords(final Bson filter, final int countLimit) {
      if (countLimit > 0) {
         return databaseCollection().find(filter).sort(Sorts.descending(AuditRecord.CHANGE_DATE)).limit(countLimit).into(new ArrayList<>());
      } else {
         return databaseCollection().find(filter).sort(Sorts.descending(AuditRecord.CHANGE_DATE)).into(new ArrayList<>());
      }
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
   public AuditRecord getAuditRecord(final String id) {
      final AuditRecord record = databaseCollection().find(idFilter(id)).first();
      if (record == null) {
         throw new StorageException("Audit log record '" + id + "' not found.");
      }
      return record;
   }

   @Override
   public void deleteAuditRecord(final String id) {
      final AuditRecord record = databaseCollection().findOneAndDelete(idFilter(id));
      if (record == null) {
         throw new StorageException("Audit log record '" + id + "' has not been deleted.");
      }
   }

   @Override
   public List<AuditRecord> findAuditRecords(final ZonedDateTime olderThan, final AuditType type) {
      final Bson filter = Filters.and(
            Filters.eq(AuditRecord.TYPE, type.toString()),
            Filters.lt(AuditRecord.CHANGE_DATE, Date.from(olderThan.toInstant()))
      );

      return  databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public void cleanAuditRecords(final ZonedDateTime olderThan) {
      final Bson filter = Filters.lt(AuditRecord.CHANGE_DATE, Date.from(olderThan.toInstant()));

      databaseCollection().deleteMany(filter);
   }

   private Bson projectFilter(final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds) {
      return Filters.or(
            Filters.eq(AuditRecord.RESOURCE_TYPE, ResourceType.PROJECT.toString()),
            Filters.and(
                  Filters.eq(AuditRecord.RESOURCE_TYPE, ResourceType.VIEW.toString()),
                  Filters.in(AuditRecord.RESOURCE_ID, viewIds)
            ),
            Filters.and(
                  Filters.eq(AuditRecord.RESOURCE_TYPE, ResourceType.COLLECTION.toString()),
                  Filters.in(AuditRecord.RESOURCE_ID, collectionIds)
            ),
            Filters.and(
                  Filters.eq(AuditRecord.RESOURCE_TYPE, ResourceType.DOCUMENT.toString()),
                  Filters.in(AuditRecord.PARENT_ID, collectionIds)
            ),
            Filters.and(
                  Filters.eq(AuditRecord.RESOURCE_TYPE, ResourceType.LINK_TYPE.toString()),
                  Filters.in(AuditRecord.RESOURCE_ID, linkTypeIds)
            ),
            Filters.and(
                  Filters.eq(AuditRecord.RESOURCE_TYPE, ResourceType.LINK.toString()),
                  Filters.in(AuditRecord.PARENT_ID, linkTypeIds)
            )
      );
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
