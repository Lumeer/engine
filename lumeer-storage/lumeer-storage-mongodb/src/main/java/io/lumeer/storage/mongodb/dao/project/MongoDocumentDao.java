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

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.DocumentCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@RequestScoped
public class MongoDocumentDao extends MongoProjectScopedDao implements DocumentDao {

   private static final String PREFIX = "documents_p-";

   @Inject
   private Event<RemoveDocument> removeDocumentEvent;

   @Override
   public void createRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<org.bson.Document> collection = database.getCollection(databaseCollectionName(project));
      collection.createIndex(Indexes.ascending(DocumentCodec.COLLECTION_ID), new IndexOptions().unique(false));
      collection.createIndex(Indexes.descending(DocumentCodec.CREATION_DATE), new IndexOptions().unique(false));
      collection.createIndex(Indexes.descending(DocumentCodec.UPDATE_DATE), new IndexOptions().unique(false));
   }

   @Override
   public void deleteRepository(final Project project) {
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
   public Document updateDocument(final String id, final Document document) {
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
   public void deleteDocument(final String id, final DataDocument data) {
      Document document = databaseCollection().findOneAndDelete(idFilter(id));
      if (document == null) {
         throw new StorageException("Document '" + id + "' has not been deleted.");
      }
      if (removeDocumentEvent != null) {
         if (data != null) {
            document.setData(data);
         }
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
   public Long getDocumentsCountByCollection(final String collectionId) {
      return databaseCollection().countDocuments(Filters.eq(DocumentCodec.COLLECTION_ID, collectionId));
   }

   @Override
   public Map<String, Long> getDocumentsCounts() {
      return rawDatabaseCollection().aggregate(Collections.singletonList(Aggregates.sortByCount("$" + DocumentCodec.COLLECTION_ID)))
                                    .into(new ArrayList<>())
                                    .stream()
                                    .collect(Collectors.toMap(doc -> doc.getString("_id"), doc -> Long.valueOf(doc.getInteger("count"))));
   }

   @Override
   public List<Document> getDocumentsByIds(final String... ids) {
      Bson idsFilter = MongoFilters.idsFilter(Arrays.stream(ids).collect(Collectors.toSet()));
      if (idsFilter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(idsFilter).into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByCreator(final String collectionId, final String userId, final Set<String> ids) {
      Bson idsFilter = MongoFilters.idsFilter(ids);
      if (idsFilter == null) {
         return Collections.emptyList();
      }
      Bson filter = Filters.and(idsFilter, creatorFilter(collectionId, userId));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByCreator(final String collectionId, final String userId, final Pagination pagination) {
      Bson filter = creatorFilter(collectionId, userId);
      return getDocumentsPaginated(filter, pagination);
   }

   private Bson creatorFilter(final String collectionId, final String userId) {
      return Filters.and(Filters.eq(DocumentCodec.CREATED_BY, userId), Filters.eq(DocumentCodec.COLLECTION_ID, collectionId));
   }

   private List<Document> getDocumentsPaginated(final Bson filter, final Pagination pagination) {
      FindIterable<Document> iterable = databaseCollection().find(filter);
      addPaginationToQuery(iterable, pagination);
      return iterable.into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByIds(final Set<String> ids) {
      Bson idsFilter = MongoFilters.idsFilter(ids);
      if (idsFilter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(idsFilter).into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByCollection(final String collectionId) {
      return databaseCollection().find(Filters.eq(DocumentCodec.COLLECTION_ID, collectionId)).into(new ArrayList<>());
   }

   @Override
   public Set<String> getDocumentsIdsByCollection(final String collectionId) {
      return databaseCollection().find(Filters.eq(DocumentCodec.COLLECTION_ID, collectionId))
                                 .projection(Projections.include(DocumentCodec.ID))
                                 .into(new ArrayList<>()).stream()
                                 .map(Document::getId)
                                 .collect(Collectors.toSet());
   }

   @Override
   public List<Document> getDocumentsByCollection(final String collectionId, final Set<String> ids) {
      Bson idsFilter = MongoFilters.idsFilter(ids);
      if (idsFilter == null) {
         return Collections.emptyList();
      }
      Bson filter = Filters.and(idsFilter, Filters.eq(DocumentCodec.COLLECTION_ID, collectionId));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByCollection(final String collectionId, final Pagination pagination) {
      Bson filter = Filters.eq(DocumentCodec.COLLECTION_ID, collectionId);
      return getDocumentsPaginated(filter, pagination);
   }

   @Override
   public List<Document> getDocumentsWithTemplateId() {
      return databaseCollection().find(Filters.exists(DocumentCodec.META_DATA + "." + Document.META_TEMPLATE_ID)).into(new ArrayList<>());
   }

   @Override
   public List<Document> getRecentDocuments(final String collectionId, boolean byUpdate) {
      return databaseCollection()
            .find(Filters.eq(DocumentCodec.COLLECTION_ID, collectionId))
            .sort(Sorts.descending(byUpdate ? DocumentCodec.UPDATE_DATE : DocumentCodec.CREATION_DATE))
            .limit(10)
            .into(new ArrayList<>());
   }

   @Override
   public List<Document> getDocumentsByCollectionIds(final Collection<String> collectionIds) {
      return databaseCollection()
            .find(Filters.in(DocumentCodec.COLLECTION_ID, collectionIds))
            .into(new ArrayList<>());
   }

   @Override
   public List<Document> duplicateDocuments(final List<Document> documents) {
      List<Document> insertDocuments = new ArrayList<>();
      documents.forEach(d -> {
         var insertDocument = new Document(d);
         insertDocument.createIfAbsentMetaData().put(Document.META_ORIGINAL_DOCUMENT_ID, d.getId());
         insertDocument.setId(ObjectId.get().toString());
         insertDocuments.add(insertDocument);
      });
      databaseCollection().insertMany(insertDocuments);

      return insertDocuments;
   }

   @Override
   public List<Document> getDocumentsByParentId(final String parentId) {
      Bson idsFilter = parentIdsFilter(Set.of(parentId));
      return databaseCollection().find(idsFilter).into(new ArrayList<>());
   }

   private Bson parentIdsFilter(Collection<String> parentIds) {
      String field = MongoUtils.concatParams(DocumentCodec.META_DATA, Document.META_PARENT_ID);
      return Filters.in(field, parentIds);
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

   private MongoCollection<org.bson.Document> rawDatabaseCollection() {
      return database.getCollection(databaseCollectionName());
   }

   MongoCollection<Document> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Document.class);
   }
}
