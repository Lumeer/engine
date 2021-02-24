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

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchSuggestionQuery;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.LinkTypeCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.BasicDBList;
import com.mongodb.MongoException;
import com.mongodb.QueryOperators;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoLinkTypeDao extends MongoProjectScopedDao implements LinkTypeDao {

   public static final String PREFIX = "linktypes_p-";

   @Inject
   private Event<CreateLinkType> createLinkTypeEvent;

   @Inject
   private Event<UpdateLinkType> updateLinkTypeEvent;

   @Inject
   private Event<RemoveLinkType> removeLinkTypeEvent;

   @Override
   public void createRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(LinkTypeCodec.NAME), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.ascending(LinkTypeCodec.COLLECTION_IDS));
   }

   @Override
   public void deleteRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public LinkType createLinkType(final LinkType linkType) {
      try {
         databaseCollection().insertOne(linkType);
         if (createLinkTypeEvent != null) {
            createLinkTypeEvent.fire(new CreateLinkType(linkType));
         }
         return linkType;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create link type: " + linkType, ex);
      }
   }

   @Override
   public LinkType updateLinkType(final String id, final LinkType linkType, final LinkType originalLinkType) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(false);
      try {
         Bson update = new Document("$set", linkType).append("$inc", new Document(LinkTypeCodec.VERSION, 1L));
         LinkType updatedLinkType = databaseCollection().findOneAndUpdate(idFilter(id), update, options);
         if (updatedLinkType == null) {
            throw new StorageException("Link type '" + id + "' has not been updated.");
         }
         if (originalLinkType != null) {
            updatedLinkType.copyComputedProperties(originalLinkType);
         }
         if (updateLinkTypeEvent != null) {
            updateLinkTypeEvent.fire(new UpdateLinkType(updatedLinkType, originalLinkType));
         }
         return updatedLinkType;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update link type: " + linkType, ex);
      }
   }

   @Override
   public void deleteLinkType(final String id) {
      LinkType linkType = databaseCollection().findOneAndDelete(idFilter(id));
      if (linkType == null) {
         throw new StorageException("Link type '" + id + "' has not been deleted.");
      }
      if (removeLinkTypeEvent != null) {
         removeLinkTypeEvent.fire(new RemoveLinkType(linkType));
      }
   }

   @Override
   public void deleteLinkTypesByCollectionId(final String collectionId) {
      databaseCollection().deleteMany(collectionIdFilter(collectionId));
   }

   @Override
   public LinkType getLinkType(final String id) {
      LinkType linkType = databaseCollection().find(idFilter(id)).first();
      if (linkType == null) {
         throw new StorageException("Cannot find link type: " + id);
      }
      return linkType;
   }

   @Override
   public List<LinkType> getAllLinkTypes() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   @Override
   public List<LinkType> getLinkTypesByIds(final Set<String> ids) {
      Bson filter = MongoFilters.idsFilter(ids);
      if (filter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkType> getLinkTypes(final SearchSuggestionQuery query) {
      List<Bson> aggregates = linkTypesSuggestionAggregation(query);
      return databaseCollection().aggregate(aggregates).into(new ArrayList<>());
   }

   private List<Bson> linkTypesSuggestionAggregation(SearchSuggestionQuery query) {
      List<Bson> aggregates = new ArrayList<>();

      aggregates.add(Aggregates.match(linkTypesSuggestionFilter(query)));
      addCollectionIdsAggregation(aggregates, query);
      addPriorityCollectionIdsAggregation(aggregates, query);
      addPaginationToAggregates(aggregates, query);

      return aggregates;
   }

   private void addCollectionIdsAggregation(List<Bson> aggregates, final SearchSuggestionQuery query) {
      if (!query.hasCollectionIdsQuery()) {
         return;
      }
      BasicDBList subsetList = new BasicDBList();
      subsetList.add("$" + LinkTypeCodec.COLLECTION_IDS);
      subsetList.add(new ArrayList<>(query.getCollectionIds()));

      Document isSubset = new Document("$setIsSubset", subsetList);

      aggregates.add(Aggregates.addFields(new Field<>("_isSubset", isSubset)));
      aggregates.add(Aggregates.match(Filters.eq("_isSubset", true)));
   }

   private void addPriorityCollectionIdsAggregation(List<Bson> aggregates, final SearchSuggestionQuery query) {
      if (!query.hasPriorityCollectionIdsQuery()) {
         return;
      }

      BasicDBList intersectionList = new BasicDBList();
      intersectionList.add("$" + LinkTypeCodec.COLLECTION_IDS);
      intersectionList.add(new ArrayList<>(query.getPriorityCollectionIds()));

      Document intersection = new Document("$setIntersection", intersectionList);
      Document size = new Document(QueryOperators.SIZE, intersection);

      aggregates.add(Aggregates.addFields(new Field<>("_linkPriority", size)));
      aggregates.add(Aggregates.sort(Sorts.descending("_linkPriority")));
   }

   private Bson linkTypesSuggestionFilter(SearchSuggestionQuery query) {
      return Filters.regex(LinkTypeCodec.NAME, Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
   }

   @Override
   public List<LinkType> getLinkTypesByAttributes(final SearchSuggestionQuery query) {
      List<Bson> aggregates = attributeSuggestionAggregation(query);
      return databaseCollection().aggregate(aggregates).into(new ArrayList<>());
   }

   private List<Bson> attributeSuggestionAggregation(final SearchSuggestionQuery query) {
      List<Bson> aggregates = new ArrayList<>();

      aggregates.add(Aggregates.match(attributeSuggestionQuery(query)));
      addCollectionIdsAggregation(aggregates, query);
      addPriorityCollectionIdsAggregation(aggregates, query);
      addPaginationToAggregates(aggregates, query);

      return aggregates;
   }

   private Bson attributeSuggestionQuery(final SearchSuggestionQuery query) {
      return Filters.regex(MongoUtils.concatParams(LinkTypeCodec.ATTRIBUTES, LinkTypeCodec.NAME), Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
   }

   @Override
   public List<LinkType> getLinkTypesByCollectionId(final String collectionId) {
      return databaseCollection().find(collectionIdFilter(collectionId)).into(new ArrayList<>());
   }

   private Bson collectionIdFilter(String collectionId) {
      return Filters.in(LinkTypeCodec.COLLECTION_IDS, Collections.singletonList(collectionId));
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

   MongoCollection<LinkType> databaseCollection() {
      return database.getCollection(databaseCollectionName(), LinkType.class);
   }
}
