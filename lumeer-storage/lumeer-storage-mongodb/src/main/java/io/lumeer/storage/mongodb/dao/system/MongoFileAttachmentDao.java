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
import java.util.List;
import javax.annotation.PostConstruct;

public class MongoFileAttachmentDao extends SystemScopedDao implements FileAttachmentDao {

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
         groupCollection.createIndex(Indexes.ascending(FileAttachment.ORGANIZATION_ID, FileAttachment.PROJECT_ID, FileAttachment.COLLECTION_ID, FileAttachment.DOCUMENT_ID, FileAttachment.ATTRIBUTE_ID), new IndexOptions().unique(false));
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
   public FileAttachment findFileAttachment(final FileAttachment fileAttachment) {
      return databaseCollection().find(idFilter(fileAttachment.getId())).first();
   }

   @Override
   public List<FileAttachment> findAllFileAttachments(final Organization organization, final Project project, final String collectionId) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId)
            );

      return databaseCollection().find(attachmentFilter).into(new ArrayList<>());
   }

   @Override
   public List<FileAttachment> findAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String documentId) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.DOCUMENT_ID, documentId)
            );

      return databaseCollection().find(attachmentFilter).into(new ArrayList<>());
   }

   @Override
   public List<FileAttachment> findAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String documentId, final String attributeId) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.DOCUMENT_ID, documentId),
                  Filters.eq(FileAttachmentCodec.ATTRIBUTE_ID, attributeId)
            );

      return databaseCollection().find(attachmentFilter).into(new ArrayList<>());
   }

   @Override
   public void removeFileAttachment(final FileAttachment fileAttachment) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(fileAttachment.getId()));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("FileAttachment '" + fileAttachment.getId() + "' was not deleted.");
      }
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
   public void removeAllFileAttachments(final Organization organization, final Project project, final String collectionId) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId)
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   @Override
   public void removeAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String attributeId) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.ATTRIBUTE_ID, attributeId)
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   @Override
   public void removeAllFileAttachments(final Organization organization, final Project project, final String collectionId, final String documentId, final String attributeId) {
      final Bson attachmentFilter =
            Filters.and(
                  Filters.eq(FileAttachmentCodec.ORGANIZATION_ID, organization.getId()),
                  Filters.eq(FileAttachmentCodec.PROJECT_ID, project.getId()),
                  Filters.eq(FileAttachmentCodec.COLLECTION_ID, collectionId),
                  Filters.eq(FileAttachmentCodec.DOCUMENT_ID, documentId),
                  Filters.eq(FileAttachmentCodec.ATTRIBUTE_ID, attributeId)
            );

      databaseCollection().deleteMany(attachmentFilter);
   }

   private MongoCollection<FileAttachment> databaseCollection() {
      return database.getCollection(COLLECTION_NAME, FileAttachment.class);
   }
}
