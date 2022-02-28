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
package io.lumeer.storage.mongodb.dao.collection;

import static io.lumeer.storage.mongodb.util.MongoFilters.createFilterForFulltexts;
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Pagination;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.filter.LinkSearchAttributeFilter;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoLinkDataDao extends MongoCollectionScopedDao implements LinkDataDao {

   private static final String ID = "_id";
   private static final String PREFIX = "linkData_c-";

   @Override
   public void createDataRepository(final String linkTypeId) {
      database.createCollection(linkDataCollectionName(linkTypeId));
      createFulltextIndexOnAllFields(linkTypeId);
   }

   private void createFulltextIndexOnAllFields(final String linkTypeId) {
      linkDataCollection(linkTypeId).createIndex(Indexes.text("$**"));
   }

   @Override
   public void deleteDataRepository(final String linkTypeId) {
      linkDataCollection(linkTypeId).drop();
   }

   @Override
   public DataDocument createData(final String linkTypeId, final String linkInstanceId, final DataDocument data) {
      Document document = new Document(data).append(ID, new ObjectId(linkInstanceId));
      linkDataCollection(linkTypeId).insertOne(document);
      return data;
   }

   @Override
   public List<DataDocument> createData(final String linkTypeId, final List<DataDocument> data) {
      List<Document> documents = data.stream().map(dataDocument -> new Document(dataDocument).append(ID, new ObjectId(dataDocument.getId()))).collect(Collectors.toList());
      linkDataCollection(linkTypeId).insertMany(documents);

      for (int i = 0; i < documents.size(); i++) {
         Object idObj = documents.get(i).get(ID);
         String id = idObj instanceof String ? (String) idObj : ((ObjectId) idObj).toHexString();
         data.get(i).setId(id);
      }

      return data;
   }

   @Override
   public DataDocument updateData(final String linkTypeId, final String linkInstanceId, final DataDocument data) {
      Document document = new Document(data);
      document.remove(ID);
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);

      Document updatedDocument = linkDataCollection(linkTypeId).findOneAndReplace(idFilter(linkInstanceId), document, options);
      if (updatedDocument == null) {
         throw new StorageException("LinkInstance '" + linkInstanceId + "' has not been updated (replaced).");
      }
      return MongoUtils.convertDocument(updatedDocument);
   }

   @Override
   public DataDocument patchData(final String linkTypeId, final String linkInstanceId, final DataDocument data) {
      data.remove(ID);

      if (data.size() == 0) {
         return getData(linkTypeId, linkInstanceId);
      }

      Document updateDocument = new Document("$set", new Document(data));
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true);

      Document patchedDocument = linkDataCollection(linkTypeId).findOneAndUpdate(idFilter(linkInstanceId), updateDocument, options);
      if (patchedDocument == null) {
         throw new StorageException("LinkInstance '" + linkInstanceId + "' has not been patched (partially updated).");
      }
      return MongoUtils.convertDocument(patchedDocument);
   }

   @Override
   public void deleteData(final String linkTypeId, final String linkInstanceId) {
      linkDataCollection(linkTypeId).deleteOne(idFilter(linkInstanceId));
   }

   @Override
   public void deleteData(final String linkTypeId, final Set<String> linkInstanceIds) {
      Bson filter = MongoFilters.idsFilter(linkInstanceIds);
      if (filter == null) {
         return;
      }
      linkDataCollection(linkTypeId).deleteMany(filter);
   }

   @Override
   public long deleteAttribute(final String linkTypeId, final String attributeId) {
      final UpdateResult updateResult = linkDataCollection(linkTypeId).updateMany(new BsonDocument(), Updates.unset(attributeId));
      return updateResult.getModifiedCount();
   }

   @Override
   public DataDocument getData(final String linkTypeId, final String linkInstanceId) {
      MongoCursor<Document> mongoCursor = linkDataCollection(linkTypeId).find(idFilter(linkInstanceId)).iterator();
      if (!mongoCursor.hasNext()) {
         return new DataDocument();
      }
      return MongoUtils.convertDocument(mongoCursor.next());
   }

   @Override
   public List<DataDocument> getData(final String linkTypeId) {
      return MongoUtils.convertIterableToList(linkDataCollection(linkTypeId).find());
   }

   @Override
   public List<DataDocument> getData(final String linkTypeId, final Integer skip, final Integer limit) {
      return MongoUtils.convertIterableToList(linkDataCollection(linkTypeId).find().skip(skip).limit(limit));
   }

   @Override
   public Stream<DataDocument> getDataStream(final String linkTypeId) {
      return StreamSupport.stream(linkDataCollection(linkTypeId).find().map(MongoUtils::convertDocument).spliterator(), false);
   }

   @Override
   public List<DataDocument> getData(final String linkTypeId, final Set<String> linkInstanceIds) {
      Bson idsFilter = MongoFilters.idsFilter(linkInstanceIds);
      if (idsFilter == null) {
         return Collections.emptyList();
      }
      return MongoUtils.convertIterableToList(linkDataCollection(linkTypeId).find(idsFilter));
   }

   @Override
   public List<DataDocument> searchData(final SearchQueryStem stem, final Pagination pagination, final LinkType linkType) {
      Bson filter = createFilterForStem(stem, linkType);
      FindIterable<Document> iterable = linkDataCollection(linkType.getId()).find(filter);
      addPaginationToQuery(iterable, pagination);
      return MongoUtils.convertIterableToList(iterable);
   }

   @Override
   public List<DataDocument> searchDataByFulltexts(final Set<String> fulltexts, final Pagination pagination, final List<LinkType> linkTypes) {
      List<DataDocument> documents = new ArrayList<>();
      for (LinkType linkType : linkTypes) {
         Bson filter = createFilterForFulltexts(linkType.getAttributes(), fulltexts);
         if (filter != null) {
            FindIterable<Document> iterable = linkDataCollection(linkType.getId()).find(filter);
            addPaginationToQuery(iterable, pagination);
            documents.addAll(MongoUtils.convertIterableToList(iterable));
         }
      }

      return documents;
   }

   @Override
   public List<DataDocument> duplicateData(final String linkTypeId, final Map<String, String> linkIds) {
      final List<DataDocument> newData = new ArrayList<>();

      final Bson idsFilter = MongoFilters.idsFilter(linkIds.keySet());
      if (idsFilter != null) {
         linkDataCollection(linkTypeId).find(idsFilter).forEach((Consumer<? super Document>) d -> {
            final DataDocument doc = MongoUtils.convertDocument(d);

            if (linkIds.containsKey(doc.getId())) {
               doc.setId(linkIds.get(doc.getId()));
               newData.add(doc);
            }
         });

         if (newData.size() > 0) {
            linkDataCollection(linkTypeId).insertMany(newData.stream().map(Document::new).collect(Collectors.toList()));
         }
      }

      return newData;
   }

   private Bson createFilterForStem(final SearchQueryStem stem, final LinkType linkType) {
      List<Bson> filters = new ArrayList<>();

      if (stem.containsLinkInstanceIdsQuery()) {
         Bson idsFilter = MongoFilters.idsFilter(stem.getLinkInstanceIds());
         if (idsFilter != null) {
            filters.add(idsFilter);
         }
      }

      if (stem.containsLinkFiltersQuery()) {
         List<Bson> linkFilters = stem.getLinkFilters().stream()
                                      .map(this::linkAttributeFilter)
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.toList());
         if (!linkFilters.isEmpty()) {
            filters.addAll(linkFilters);
         }
      }

      if (stem.containsFulltextsQuery()) {
         Bson fulltextsFilter = createFilterForFulltexts(linkType.getAttributes(), stem.getFulltexts());
         if (fulltextsFilter != null) {
            filters.add(fulltextsFilter);
         }
      }

      return filters.size() > 0 ? Filters.and(filters) : new Document();
   }

   private Bson linkAttributeFilter(LinkSearchAttributeFilter filter) {
      return MongoFilters.attributeFilter(filter);
   }

   MongoCollection<Document> linkDataCollection(String linkTypeId) {
      return database.getCollection(linkDataCollectionName(linkTypeId));
   }

   String linkDataCollectionName(String linkTypeId) {
      return PREFIX + linkTypeId;
   }

}
