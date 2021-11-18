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
import static io.lumeer.storage.mongodb.util.MongoFilters.idsFilter;

import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.dao.FileAttachmentDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.FileAttachmentCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

public class MongoFileAttachmentDao extends MongoSystemScopedDao implements FileAttachmentDao {

   public static final String COLLECTION_NAME = "fileAttachments";

   @PostConstruct
   public void initDb() {
      createFileAttachmentRepository();
   }

   @Override
   public void createFileAttachmentRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(COLLECTION_NAME)) {
         database.createCollection(COLLECTION_NAME);

         MongoCollection<org.bson.Document> groupCollection = database.getCollection(COLLECTION_NAME);
         groupCollection.createIndex(Indexes.ascending(FileAttachment.ORGANIZATION_ID, FileAttachment.PROJECT_ID, FileAttachment.COLLECTION_ID, FileAttachment.DOCUMENT_ID, FileAttachment.ATTRIBUTE_ID, FileAttachment.ATTACHMENT_TYPE), new IndexOptions().unique(false));
      }
   }

   @Override
   public FileAttachment createFileAttachment(final FileAttachment fileAttachment) {
      try {
         databaseCollection().insertOne(fileAttachment);
         return fileAttachment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create FileAttachment " + fileAttachment, ex);
      }
   }

   @Override
   public List<FileAttachment> createFileAttachments(final List<FileAttachment> fileAttachments) {
      try {
         databaseCollection().insertMany(fileAttachments);
         return fileAttachments;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create FileAttachments " + fileAttachments, ex);
      }
   }

   @Override
   public FileAttachment updateFileAttachment(final FileAttachment fileAttachment) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         FileAttachment returnedFileAttachment = databaseCollection().findOneAndReplace(idFilter(fileAttachment.getId()), fileAttachment, options);
         if (returnedFileAttachment == null) {
            throw new StorageException("FileAttachment '" + fileAttachment.getId() + "' has not been updated.");
         }
         return returnedFileAttachment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update FileAttachment " + fileAttachment, ex);
      }
   }

   @Override
   public FileAttachment findFileAttachment(final String fileAttachmentId) {
      return databaseCollection().find(idFilter(fileAttachmentId)).first();
   }

   @Override
   public FileAttachment findFileAttachment(final FileAttachment fileAttachment) {
      return databaseCollection().find(idFilter(fileAttachment.getId())).first();
   }

   @Override
   public List<FileAttachment> findFileAttachments(final java.util.Collection<String> fileAttachmentIds) {
      Bson filter = idsFilter(fileAttachmentIds);
      if (filter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<FileAttachment> findAllFileAttachments(final Organization organization, final Project project, final String collectionId, final FileAttachment.AttachmentType type) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.ATTACHMENT_TYPE, type.ordinal())
            );

      return databaseCollection().find(attachmentFilter).into(new ArrayList<>());
   }

   @Override
   public List<FileAttachment> findAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String documentId, final FileAttachment.AttachmentType type) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.DOCUMENT_ID, documentId),
                  Filters.eq(FileAttachmentCodec.ATTACHMENT_TYPE, type.ordinal())
            );

      return databaseCollection().find(attachmentFilter).into(new ArrayList<>());
   }

   @Override
   public List<FileAttachment> findAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.DOCUMENT_ID, documentId),
                  Filters.eq(FileAttachmentCodec.ATTRIBUTE_ID, attributeId),
                  Filters.eq(FileAttachmentCodec.ATTACHMENT_TYPE, type.ordinal())
            );

      return databaseCollection().find(attachmentFilter).into(new ArrayList<>());
   }

   @Override
   public boolean removeFileAttachment(final String attachmentId) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(attachmentId));

      return result.getDeletedCount() == 1;
   }

   @Override
   public boolean removeFileAttachments(final java.util.Collection<String> fileAttachmentIds) {
      Bson filter = idsFilter(fileAttachmentIds);
      if (filter == null) {
         return false;
      }

      DeleteResult result = databaseCollection().deleteMany(filter);
      return result.getDeletedCount() == fileAttachmentIds.size();
   }

   @Override
   public boolean removeFileAttachment(final FileAttachment fileAttachment) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(fileAttachment.getId()));

      return result.getDeletedCount() == 1;
   }

   @Override
   public void removeAllFileAttachments(final Organization organization, final Project project) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId())
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   @Override
   public void removeAllFileAttachments(final Organization organization, final Project project, final String collectionId, final FileAttachment.AttachmentType type) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.ATTACHMENT_TYPE, type.ordinal())
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   @Override
   public void removeAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String attributeId, final FileAttachment.AttachmentType type) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.ATTRIBUTE_ID, attributeId),
                  Filters.eq(FileAttachmentCodec.ATTACHMENT_TYPE, type.ordinal())
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   @Override
   public void removeAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String documentId, final String attributeId, final FileAttachment.AttachmentType type) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.DOCUMENT_ID, documentId),
                  Filters.eq(FileAttachmentCodec.ATTRIBUTE_ID, attributeId),
                  Filters.eq(FileAttachmentCodec.ATTACHMENT_TYPE, type.ordinal())
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   private MongoCollection<FileAttachment> databaseCollection() {
      return database.getCollection(COLLECTION_NAME, FileAttachment.class);
   }
}
