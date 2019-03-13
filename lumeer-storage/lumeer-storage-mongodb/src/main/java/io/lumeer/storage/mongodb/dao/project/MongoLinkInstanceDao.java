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

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;
import io.lumeer.storage.mongodb.codecs.LinkInstanceCodec;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoLinkInstanceDao extends ProjectScopedDao implements LinkInstanceDao {

   private static final String PREFIX = "linkinstances_p-";

   @Inject
   private Event<CreateLinkInstance> createLinkInstanceEvent;

   @Inject
   private Event<UpdateLinkInstance> updateLinkInstanceEvent;

   @Inject
   private Event<RemoveLinkInstance> removeLinkInstanceEvent;

   @Override
   public void createLinkInstanceRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(LinkInstanceCodec.LINK_TYPE_ID), new IndexOptions().unique(false));
   }

   @Override
   public void deleteLinkInstanceRepository(Project project) {
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
      try {
         databaseCollection().insertMany(linkInstances);
         if (createLinkInstanceEvent != null) {
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
   public void deleteLinkInstance(final String id) {
      LinkInstance linkInstance = databaseCollection().findOneAndDelete(idFilter(id));
      if (linkInstance == null) {
         throw new StorageException("Link instance '" + id + "' has not been deleted.");
      }
      if (removeLinkInstanceEvent != null) {
         removeLinkInstanceEvent.fire(new RemoveLinkInstance(linkInstance));
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
      Bson filter = Filters.in(LinkInstanceCodec.ID, ids.stream().map(ObjectId::new).collect(Collectors.toSet()));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> getLinkInstancesByLinkType(final String linkTypeId) {
      return databaseCollection().find(Filters.eq(LinkInstanceCodec.LINK_TYPE_ID, linkTypeId)).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> getLinkInstancesByDocumentIds(final Set<String> documentIds, final String linkTypeId) {
      Bson filter = Filters.and(Filters.eq(LinkInstanceCodec.LINK_TYPE_ID, linkTypeId), Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, documentIds));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> searchLinkInstances(final SearchQuery query) {
      final FindIterable<LinkInstance> linkInstances = databaseCollection().find(linkInstancesFilter(query));
      addPaginationToQuery(linkInstances, query);
      return linkInstances.into(new ArrayList<>());
   }

   @Override
   public long deleteLinkInstances(final SearchQuery query) {
      final DeleteResult deleteResult = databaseCollection().deleteMany(linkInstancesFilter(query));
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
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollectionName(getProject().get());
   }

   MongoCollection<LinkInstance> databaseCollection() {
      return database.getCollection(databaseCollectionName(), LinkInstance.class);
   }
}
