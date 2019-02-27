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
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.filter.AttributeFilter;
import io.lumeer.storage.api.query.SearchQueryStem;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoDataDao extends CollectionScopedDao implements DataDao {

   private static final String ID = "_id";
   private static final String PREFIX = "data_c-";

   @Override
   public void createDataRepository(final String collectionId) {
      database.createCollection(dataCollectionName(collectionId));
      createFulltextIndexOnAllFields(collectionId);
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
   public long deleteAttribute(final String collectionId, final String attributeId) {
      final UpdateResult updateResult = dataCollection(collectionId).updateMany(new BsonDocument(), Updates.unset(attributeId));
      return updateResult.getModifiedCount();
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
   public List<DataDocument> getData(final String collectionId) {
      return MongoUtils.convertIterableToList(dataCollection(collectionId).find());
   }

   @Override
   public List<DataDocument> getData(final String collectionId, final Set<String> documentIds) {
      return MongoUtils.convertIterableToList(dataCollection(collectionId).find(MongoFilters.idsFilter(documentIds)));
   }

   @Override
   public List<DataDocument> searchData(final SearchQueryStem stem, final Pagination pagination, final Collection collection) {
      Bson filter = createFilterForStem(stem, collection);
      FindIterable<Document> iterable = dataCollection(collection.getId()).find(filter);
      addPaginationToQuery(iterable, pagination);
      return MongoUtils.convertIterableToList(iterable);
   }

   private Bson createFilterForStem(final SearchQueryStem stem, Collection collection) {
      List<Bson> filters = new ArrayList<>();

      if (stem.containsDocumentIdsQuery()) {
         filters.add(MongoFilters.idsFilter(stem.getDocumentIds()));
      }

      if (stem.containsFiltersQuery()) {
         List<Bson> attributeFilters = stem.getFilters().stream()
                                           .map(this::attributeFilter)
                                           .filter(Objects::nonNull)
                                           .collect(Collectors.toList());
         if (!attributeFilters.isEmpty()) {
            filters.addAll(attributeFilters);
         }
      }

      if (stem.containsFulltextsQuery()) {
         Bson fulltextsFilter = createFilterForFulltexts(collection, stem.getFulltexts());
         if (fulltextsFilter != null) {
            filters.add(fulltextsFilter);
         }
      }

      return filters.size() > 0 ? Filters.and(filters) : new Document();
   }

   private Bson attributeFilter(AttributeFilter filter) {
      switch (filter.getConditionType()) {
         case EQUALS:
            return Filters.eq(filter.getAttributeId(), filter.getValue());
         case NOT_EQUALS:
            return Filters.ne(filter.getAttributeId(), filter.getValue());
         case LOWER_THAN:
            return Filters.lt(filter.getAttributeId(), filter.getValue());
         case LOWER_THAN_EQUALS:
            return Filters.lte(filter.getAttributeId(), filter.getValue());
         case GREATER_THAN:
            return Filters.gt(filter.getAttributeId(), filter.getValue());
         case GREATER_THAN_EQUALS:
            return Filters.gte(filter.getAttributeId(), filter.getValue());
      }
      return null;
   }

   @Override
   public List<DataDocument> searchDataByFulltexts(final Set<String> fulltexts, final Pagination pagination, final List<Collection> projectCollections) {
      List<DataDocument> documents = new ArrayList<>();
      for (Collection collection : projectCollections) {
         Bson filter = createFilterForFulltexts(collection, fulltexts);
         if (filter != null) {
            FindIterable<Document> iterable = dataCollection(collection.getId()).find(filter);
            addPaginationToQuery(iterable, pagination);
            documents.addAll(MongoUtils.convertIterableToList(iterable));
         }
      }

      return documents;
   }

   private Bson createFilterForFulltexts(Collection collection, Set<String> fulltexts) {
      List<Bson> filters = fulltexts.stream().map(fulltext -> createFilterForFulltext(collection, fulltext))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());

      return filters.size() > 0 ? Filters.and(filters) : null;
   }

   private Bson createFilterForFulltext(Collection collection, String fulltext) {
      List<Attribute> fulltextAttrs = collection.getAttributes().stream()
                                                .filter(attr -> attr.getName().toLowerCase().contains(fulltext.toLowerCase()))
                                                .collect(Collectors.toList());

      List<Bson> attrFilters = collection.getAttributes().stream()
                                         .map(attr -> Filters.regex(attr.getId(), Pattern.compile(fulltext, Pattern.CASE_INSENSITIVE)))
                                         .collect(Collectors.toList());

      Bson contentFilter = !attrFilters.isEmpty() ? Filters.or(attrFilters) : null;

      if (fulltextAttrs.size() > 0) { // we search by presence of the matching attributes
         Bson attrNamesFilter = Filters.or(fulltextAttrs.stream().map(attr -> Filters.exists(attr.getId())).collect(Collectors.toList()));
         if (contentFilter != null) {
            return Filters.or(contentFilter, attrNamesFilter);
         }
         return attrNamesFilter;
      }

      return contentFilter;
   }

   MongoCollection<Document> dataCollection(String collectionId) {
      return database.getCollection(dataCollectionName(collectionId));
   }

   String dataCollectionName(String collectionId) {
      return PREFIX + collectionId;
   }

}
