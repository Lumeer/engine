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
package io.lumeer.storage.mongodb.dao.collection;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.filter.AttributeFilter;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.mongodb.MongoUtils;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class MongoDataDao extends CollectionScopedDao implements DataDao {

   @Inject
   private CollectionDao collectionDao;

   private static final String ID = "_id";
   private static final String PREFIX = "data_c-";

   @Override
   public void createDataRepository(final String collectionId) {
      database.createCollection(dataCollectionName(collectionId));
      createFulltextIndexOnAllFields(collectionId);
   }

   void setCollectionDao(CollectionDao collectionDao) {
      this.collectionDao = collectionDao;
   }

   private void createFulltextIndexOnAllFields(final String collectionId) {
      dataCollection(collectionId).createIndex(Indexes.text("$**"));
   }

   @Override
   public void deleteDataRepository(final String collectionId) {
      dataCollection(collectionId).drop();
   }

   @Override
   public DataDocument createData(final String collectionId, final String documentId, final DataDocument data) {
      Document document = new Document(data).append(ID, new ObjectId(documentId));
      dataCollection(collectionId).insertOne(document);
      return data;
   }

   @Override
   public List<DataDocument> createData(final String collectionId, final List<DataDocument> data) {
      List<Document> documents = data.stream().map(dataDocument -> new Document(dataDocument).append(ID, new ObjectId(dataDocument.getId()))).collect(Collectors.toList());
      dataCollection(collectionId).insertMany(documents);

      for (int i = 0; i < documents.size(); i++) {
         Object idObj = documents.get(i).get(ID);
         String id = idObj instanceof String ? (String) idObj : ((ObjectId) idObj).toHexString();
         data.get(i).setId(id);
      }
      return data;
   }

   @Override
   public DataDocument updateData(final String collectionId, final String documentId, final DataDocument data) {
      Document document = new Document(data);
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      Document updatedDocument = dataCollection(collectionId).findOneAndReplace(idFilter(documentId), document, options);
      if (updatedDocument == null) {
         throw new StorageException("Document '" + documentId + "' has not been updated (replaced).");
      }
      return MongoUtils.convertDocument(updatedDocument);
   }

   @Override
   public DataDocument patchData(final String collectionId, final String documentId, final DataDocument data) {
      data.remove(ID);
      Document updateDocument = new Document("$set", new Document(data));
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      Document patchedDocument = dataCollection(collectionId).findOneAndUpdate(idFilter(documentId), updateDocument, options);
      if (patchedDocument == null) {
         throw new StorageException("Document '" + documentId + "' has not been patched (partially updated).");
      }
      return MongoUtils.convertDocument(patchedDocument);
   }

   @Override
   public void deleteData(final String collectionId, final String documentId) {
      dataCollection(collectionId).deleteOne(idFilter(documentId));
   }

   @Override
   public DataDocument getData(final String collectionId, final String documentId) {
      MongoCursor<Document> mongoCursor = dataCollection(collectionId).find(idFilter(documentId)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.DOCUMENT);
      }
      return MongoUtils.convertDocument(mongoCursor.next());
   }

   @Override
   public List<DataDocument> getData(final String collectionId, final SearchQuery query) {
      final MongoCollection<Document> collection = dataCollection(collectionId);
      final FindIterable<Document> mongoIterable = collection.find(createFilter(collectionId, query));
      addPaginationToQuery(mongoIterable, query);
      final List<DataDocument> foundDocuments = MongoUtils.convertIterableToList(mongoIterable);

      return foundDocuments;
   }

   @Override
   public long getDataCount(final String collectionId, final SearchQuery query) {
      final MongoCollection<Document> collection = dataCollection(collectionId);
      return collection.countDocuments(createFilter(collectionId, query));
   }

   private Bson createFilter(String collectionId, SearchQuery query) {
      List<Bson> filters = new ArrayList<>();
      // does not work as expected - cannot search for a single character for example, only whole words
      if (query.isFulltextQuery()) {

         Collection collection = collectionDao.getCollectionById(collectionId);
         List<Attribute> fulltextAttrs = collection.getAttributes().stream().filter(attr -> attr.getName().toLowerCase().contains(query.getFulltext().toLowerCase())).collect(Collectors.toList());

         // we only search by presence of the matching attributes
         if (fulltextAttrs.size() > 0) {
            filters.add(Filters.or(fulltextAttrs.stream().map(attr -> Filters.exists(attr.getId())).collect(Collectors.toList())));
         } else if (collection.getAttributes().size() > 0) { // we search by content
            filters.add(Filters.or(collection.getAttributes().stream().map(attr -> Filters.regex(attr.getId(), query.getFulltext(), "i")).collect(Collectors.toList())));
         }
      }
      if (query.isDocumentIdsQuery()) {
         List<ObjectId> ids = query.getDocumentIds().stream().filter(ObjectId::isValid).map(ObjectId::new).collect(Collectors.toList());
         if (!ids.isEmpty()) {
            filters.add(Filters.in(ID, ids));
         }
      }
      if (query.isFiltersQuery()) {
         List<Bson> attributeFilters = query.getFilters().stream()
                                            .map(this::attributeFilter)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
         filters.addAll(attributeFilters);
      }

      final Bson result = filters.size() > 0 ? Filters.and(filters) : new Document();
      return result;
   }

   private Bson attributeFilter(AttributeFilter filter) {
      switch (filter.getConditionType()) {
         case EQUALS:
            return Filters.eq(filter.getAttributeName(), filter.getValue());
         case NOT_EQUALS:
            return Filters.ne(filter.getAttributeName(), filter.getValue());
         case LOWER_THAN:
            return Filters.lt(filter.getAttributeName(), filter.getValue());
         case LOWER_THAN_EQUALS:
            return Filters.lte(filter.getAttributeName(), filter.getValue());
         case GREATER_THAN:
            return Filters.gt(filter.getAttributeName(), filter.getValue());
         case GREATER_THAN_EQUALS:
            return Filters.gte(filter.getAttributeName(), filter.getValue());
      }
      return null;
   }

   MongoCollection<Document> dataCollection(String collectionId) {
      return database.getCollection(dataCollectionName(collectionId));
   }

   String dataCollectionName(String collectionId) {
      return PREFIX + collectionId;
   }

}
