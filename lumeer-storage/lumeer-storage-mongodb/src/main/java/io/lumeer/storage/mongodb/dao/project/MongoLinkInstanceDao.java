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
import static io.lumeer.storage.mongodb.util.MongoFilters.idsFilter;

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;
import io.lumeer.storage.mongodb.codecs.LinkInstanceCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoLinkInstanceDao extends MongoProjectScopedDao implements LinkInstanceDao {

   private static final String PREFIX = "linkinstances_p-";

   @Inject
   private Event<CreateLinkInstance> createLinkInstanceEvent;

   @Inject
   private Event<RemoveLinkInstance> removeLinkInstanceEvent;

   @Override
   public void createRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(LinkInstanceCodec.LINK_TYPE_ID), new IndexOptions().unique(false));
   }

   @Override
   public void deleteRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public LinkInstance createLinkInstance(final LinkInstance linkInstance) {
      try {
         databaseCollection().insertOne(linkInstance);

         return linkInstance;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create link instance: " + linkInstance, ex);
      }
   }

   @Override
   public List<LinkInstance> createLinkInstances(final List<LinkInstance> linkInstances) {
      return createLinkInstances(linkInstances, true);
   }

   @Override
   public List<LinkInstance> createLinkInstances(final List<LinkInstance> linkInstances, final boolean sendNotifications) {
      try {
         databaseCollection().insertMany(linkInstances);
         if (sendNotifications && createLinkInstanceEvent != null) {
            linkInstances.forEach(linkInstance -> createLinkInstanceEvent.fire(new CreateLinkInstance(linkInstance)));
         }
         return linkInstances;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create link instances: " + linkInstances, ex);
      }
   }

   @Override
   public LinkInstance updateLinkInstance(final String id, final LinkInstance linkInstance) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
      try {
         Bson update = new Document("$set", linkInstance).append("$inc", new Document(LinkInstanceCodec.DATA_VERSION, 1));
         LinkInstance updatedLinkInstance = databaseCollection().findOneAndUpdate(idFilter(id), update, options);

         if (updatedLinkInstance == null) {
            throw new StorageException("Link instance '" + id + "' has not been updated.");
         }

         return updatedLinkInstance;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update link instance: " + linkInstance, ex);
      }
   }

   @Override
   public void deleteLinkInstance(final String id, final DataDocument data) {
      LinkInstance linkInstance = databaseCollection().findOneAndDelete(idFilter(id));
      if (linkInstance == null) {
         throw new StorageException("Link instance '" + id + "' has not been deleted.");
      }
      if (removeLinkInstanceEvent != null) {
         if (data != null) {
            linkInstance.setData(data);
         }
         removeLinkInstanceEvent.fire(new RemoveLinkInstance(linkInstance));
      }
   }

   @Override
   public void deleteLinkInstances(final List<String> ids) {
      Bson idsFilter = idsFilter(ids);
      if (idsFilter != null) {
         databaseCollection().deleteMany(idsFilter);
      }
   }

   @Override
   public void deleteLinkInstancesByLinkTypesIds(final Set<String> linkTypeIds) {
      Bson filter = Filters.in(LinkInstanceCodec.LINK_TYPE_ID, linkTypeIds);
      databaseCollection().deleteMany(filter);
   }

   @Override
   public void deleteLinkInstancesByDocumentsIds(final Set<String> documentsIds) {
      Bson filter = Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, documentsIds);
      databaseCollection().deleteMany(filter);
   }

   @Override
   public LinkInstance getLinkInstance(final String id) {
      LinkInstance linkInstance = databaseCollection().find(idFilter(id)).first();
      if (linkInstance == null) {
         throw new StorageException("Cannot find link instance: " + id);
      }
      return linkInstance;
   }

   @Override
   public List<LinkInstance> getLinkInstances(final Set<String> ids) {
      Bson filter = MongoFilters.idsFilter(ids);
      if (filter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> getLinkInstancesByLinkType(final String linkTypeId) {
      return databaseCollection().find(Filters.eq(LinkInstanceCodec.LINK_TYPE_ID, linkTypeId)).into(new ArrayList<>());
   }

   @Override
   public Long getLinkInstancesCountByLinkType(final String linkTypeId) {
      return databaseCollection().countDocuments(Filters.in(LinkInstanceCodec.LINK_TYPE_ID, linkTypeId));
   }

   @Override
   public Map<String, Long> getLinkInstancesCounts() {
      return rawDatabaseCollection().aggregate(Collections.singletonList(Aggregates.sortByCount("$" + LinkInstanceCodec.LINK_TYPE_ID)))
                                    .into(new ArrayList<>())
                                    .stream()
                                    .collect(Collectors.toMap(doc -> doc.getString("_id"), doc -> Long.valueOf(doc.getInteger("count"))));
   }

   @Override
   public List<LinkInstance> getLinkInstancesByLinkTypes(final Set<String> linkTypeIds) {
      return databaseCollection().find(Filters.in(LinkInstanceCodec.LINK_TYPE_ID, linkTypeIds)).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> getLinkInstancesByDocumentIds(final Set<String> documentIds, final String linkTypeId) {
      Bson filter = Filters.and(Filters.eq(LinkInstanceCodec.LINK_TYPE_ID, linkTypeId), Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, documentIds));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> getLinkInstancesByDocumentIds(final Set<String> documentIds) {
      Bson filter = Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, documentIds);
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> searchLinkInstances(final SearchQuery query) {
      final FindIterable<LinkInstance> linkInstances = databaseCollection().find(linkInstancesFilter(query));
      addPaginationToQuery(linkInstances, query);
      return linkInstances.into(new ArrayList<>());
   }

   @Override
   public LinkInstance duplicateLinkInstance(final LinkInstance linkInstance, final String replaceDocumentId, final String newDocumentId, final Map<String, String> documentMap) {
      var link = new LinkInstance(linkInstance);

      if (link.getDocumentIds().contains(replaceDocumentId)) {
         link.setId(null);
         link.getDocumentIds().replaceAll(id -> id.equals(replaceDocumentId) ? newDocumentId : documentMap.getOrDefault(id, id));

         return createLinkInstance(link);
      }

      return null;
   }

   @Override
   public List<LinkInstance> duplicateLinkInstances(final List<LinkInstance> links, final String replaceDocumentId, final String newDocumentId, final Map<String, String> documentMap) {
      links.forEach(link -> {
         link.setDocumentIds(
               link.getDocumentIds().stream().map(id ->
                     id.equals(replaceDocumentId) ? newDocumentId : documentMap.getOrDefault(id, id)
               ).collect(Collectors.toList())
         );
         link.setOriginalLinkInstanceId(link.getId());
         link.setId(ObjectId.get().toString());
      });
      databaseCollection().insertMany(links);

      return links;
   }

   @Override
   public long deleteLinkInstances(final SearchQuery query) {
      final DeleteResult deleteResult = databaseCollection().deleteMany(linkInstancesFilter(query));
      return deleteResult.getDeletedCount();
   }

   @Override
   public long deleteLinkInstances(final Set<String> linkInstanceIds) {
      final DeleteResult deleteResult = databaseCollection().deleteMany(idsFilter(linkInstanceIds));
      return deleteResult.getDeletedCount();
   }

   private Bson linkInstancesFilter(final SearchQuery query) {
      List<Bson> filters = new ArrayList<>();
      for (SearchQueryStem stem : query.getStems()) {
         List<Bson> stemFilters = new ArrayList<>();
         if (stem.containsLinkTypeIdsQuery()) {
            stemFilters.add(Filters.in(LinkInstanceCodec.LINK_TYPE_ID, stem.getLinkTypeIds()));
         }
         if (stem.containsDocumentIdsQuery()) {
            stemFilters.add(Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, stem.getDocumentIds()));
         }
         if (!stemFilters.isEmpty()) {
            filters.add(Filters.and(stemFilters));
         }
      }
      return filters.size() > 0 ? Filters.or(filters) : new Document();
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

   private MongoCollection<Document> rawDatabaseCollection() {
      return database.getCollection(databaseCollectionName());
   }

   MongoCollection<LinkInstance> databaseCollection() {
      return database.getCollection(databaseCollectionName(), LinkInstance.class);
   }
}
