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
package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.DocumentCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoDocumentDao extends ProjectScopedDao implements DocumentDao {

   private static final String PREFIX = "documents_p-";

   @Inject
   private Event<RemoveDocument> removeDocumentEvent;

   @Override
   public void createDocumentsRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<org.bson.Document> collection = database.getCollection(databaseCollectionName(project));
      collection.createIndex(Indexes.ascending(DocumentCodec.COLLECTION_ID), new IndexOptions().unique(false));
   }

   @Override
   public void deleteDocumentsRepository(final Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public Document createDocument(final Document document) {
      try {
         document.setDataVersion(0);
         databaseCollection().insertOne(document);

         return document;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create document: " + document, ex);
      }
   }

   @Override
   public List<Document> createDocuments(final List<Document> documents) {
      List<Document> returnDocuments = documents.stream().map(Document::new)
                                                .peek(document -> document.setDataVersion(0))
                                                .collect(Collectors.toList());
      databaseCollection().insertMany(returnDocuments);
      return new ArrayList<>(returnDocuments);
   }

   @Override
   public Document updateDocument(final String id, final Document document, final Document originalDocument) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Bson update = new org.bson.Document("$set", document).append("$inc", new org.bson.Document(DocumentCodec.DATA_VERSION, 1));
         Document updatedDocument = databaseCollection().findOneAndUpdate(idFilter(id), update, options);

         if (updatedDocument == null) {
            throw new StorageException("Document '" + id + "' has not been updated.");
         }

         return updatedDocument;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update document: " + document, ex);
      }
   }

   @Override
   public void deleteDocument(final String id) {
      Document document = databaseCollection().findOneAndDelete(idFilter(id));
      if (document == null) {
         throw new StorageException("Document '" + id + "' has not been deleted.");
      }
      if (removeDocumentEvent != null) {
         removeDocumentEvent.fire(new RemoveDocument(document));
      }
   }

   @Override
   public void deleteDocuments(final String collectionId) {
      final Bson filter = Filters.eq(DocumentCodec.COLLECTION_ID, collectionId);

      databaseCollection().deleteMany(filter);

      // no event is fired here as this method only occurs when the collection is deleted completely
   }

   @Override
   public Document getDocumentById(final String id) {
      Bson filter = idFilter(id);
      Document document = databaseCollection().find(filter).first();
      if (document == null) {
         throw new ResourceNotFoundException(ResourceType.DOCUMENT);
      }

      return document;
   }

   @Override
   public List<Document> getDocumentsByIds(final String... ids) {
      Bson filter = MongoFilters.idsFilter(Arrays.stream(ids).collect(Collectors.toSet()));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByCollection(final String collectionId) {
      return databaseCollection().find(Filters.eq(DocumentCodec.COLLECTION_ID, collectionId)).into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByParentIds(final Collection<String> parentIds) {
      Bson filter = parentIdsFilter(parentIds);
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   private Bson parentIdsFilter(Collection<String> parentIds) {
      String field = MongoUtils.concatParams(DocumentCodec.META_DATA, Document.META_PARENT_ID);
      return Filters.in(field, parentIds);
   }

   private String databaseCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   String databaseCollectionName() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollectionName(getProject().get());
   }

   MongoCollection<Document> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Document.class);
   }
}
