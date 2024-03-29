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

import static io.lumeer.storage.mongodb.util.MongoFilters.codeFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchSuggestionQuery;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.AttributeCodec;
import io.lumeer.storage.mongodb.codecs.CollectionCodec;
import io.lumeer.storage.mongodb.codecs.CollectionPurposeCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@RequestScoped
public class MongoCollectionDao extends MongoProjectScopedDao implements CollectionDao {

   private static final String PREFIX = "collections_p-";

   @Inject
   private Event<CreateResource> createResourceEvent;

   @Inject
   private Event<UpdateResource> updateResourceEvent;

   @Inject
   private Event<RemoveResource> removeResourceEvent;

   @Override
   public void createRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));
      ensureIndexes(project);
   }

   @Override
   public void ensureIndexes(final Project project) {
      final String dropIndex = "code_text_name_text_attributes.name_text";
      final String attrIndex = "attributes.name_1";
      final String nameIndex = "name_text";
      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));

      final List<Document> indexes = projectCollection.listIndexes().into(new ArrayList<>());
      if (indexes.stream().anyMatch(index -> dropIndex.equals(index.getString("name")))) {
         projectCollection.dropIndex(dropIndex);
      }

      final Optional<Document> attr = indexes.stream().filter(index -> attrIndex.equals(index.getString("name"))).findFirst();
      if (attr.isPresent() && attr.get().getBoolean("unique", false)) {
         projectCollection.dropIndex(attrIndex);
      }

      final Optional<Document> nameIdx = indexes.stream().filter(index -> nameIndex.equals(index.getString("name"))).findFirst();
      if (nameIdx.isPresent() && nameIdx.get().getBoolean("unique", false)) {
         projectCollection.dropIndex(nameIndex);
      }

      projectCollection.createIndex(Indexes.ascending(CollectionCodec.NAME), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.ascending(CollectionCodec.CODE), new IndexOptions().unique(true));
      projectCollection.createIndex(Indexes.ascending(CollectionCodec.ATTRIBUTES + "." + AttributeCodec.NAME), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.text(CollectionCodec.NAME));
      projectCollection.createIndex(Indexes.ascending(CollectionCodec.PURPOSE), new IndexOptions().unique(false));
   }

   @Override
   public void deleteRepository(final Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public Collection createCollection(final Collection collection) {
      try {
         databaseCollection().insertOne(collection);
         if (createResourceEvent != null) {
            createResourceEvent.fire(new CreateResource(collection));
         }
         return collection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create collection: " + collection, ex);
      }
   }

   @Override
   public Collection updateCollection(final String id, final Collection collection, final Collection originalCollection) {
      return updateCollection(id, collection, originalCollection, true);
   }

   @Override
   public Collection bookAttributesNum(final String id, final Collection collection, final int count) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Bson update = new Document("$inc", new Document(CollectionCodec.LAST_ATTRIBUTE_NUM, count));
         final Collection updatedCollection = databaseCollection().findOneAndUpdate(idFilter(id), update, options);
         if (updatedCollection == null) {
            throw new StorageException("Collection '" + id + "' has not been updated.");
         }
         return updatedCollection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update collection: " + collection, ex);
      }
   }

   @Override
   public Collection updateCollection(final String id, final Collection collection, final Collection originalCollection, final boolean pushNotification) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Bson update = new Document("$set", collection).append("$inc", new Document(CollectionCodec.VERSION, 1L));
         final Collection updatedCollection = databaseCollection().findOneAndUpdate(idFilter(id), update, options);
         if (updatedCollection == null) {
            throw new StorageException("Collection '" + id + "' has not been updated.");
         }
         if (pushNotification && updateResourceEvent != null) {
            updateResourceEvent.fire(new UpdateResource(updatedCollection, originalCollection));
         }
         return updatedCollection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update collection: " + collection, ex);
      }
   }

   public Collection updateCollectionRules(final Collection collection) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE);

      try {
         Bson update = new Document("$set", new Document(CollectionCodec.RULES, collection.getRules()));
         final Collection originalCollection = databaseCollection().findOneAndUpdate(idFilter(collection.getId()), update, options);
         if (originalCollection == null) {
            throw new StorageException("Collection '" + collection.getId() + "' has not been updated.");
         }
         return originalCollection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update collection: " + collection, ex);
      }
   }

   @Override
   public void deleteCollection(final String id) {
      final Collection collection = databaseCollection().findOneAndDelete(idFilter(id));
      if (collection == null) {
         throw new StorageException("Collection '" + id + "' has not been deleted.");
      }
      if (removeResourceEvent != null) {
         removeResourceEvent.fire(new RemoveResource(collection));
      }
   }

   @Override
   public Collection getCollectionByCode(final String code) {
      return getCollectionByFilter(codeFilter(code));
   }

   @Override
   public Collection getCollectionById(final String id) {
      return getCollectionByFilter(idFilter(id));
   }

   private Collection getCollectionByFilter(Bson filter) {
      MongoCursor<Collection> mongoCursor = databaseCollection().find(filter).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.COLLECTION);
      }
      return mongoCursor.next();
   }

   @Override
   public List<Collection> getCollectionsByIds(final java.util.Collection<String> ids) {
      Bson filter = MongoFilters.idsFilter(ids);
      if (filter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<Collection> getAllCollections() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   @Override
   public List<Collection> getCollections(final DatabaseQuery query) {
      Bson filter = MongoFilters.permissionsFilter(query);
      return searchCollectionsByFilter(filter, query);
   }

   private List<Collection> searchCollectionsByFilter(Bson filter, DatabaseQuery query) {
      FindIterable<Collection> iterable = databaseCollection().find(filter);
      addPaginationToQuery(iterable, query);

      return iterable.into(new ArrayList<>());
   }

   @Override
   public List<Collection> getCollections(final SearchSuggestionQuery query, final boolean skipPermissions) {
      List<Bson> aggregates = collectionSuggestionAggregation(query, skipPermissions);
      return databaseCollection().aggregate(aggregates).into(new ArrayList<>());
   }

   private List<Bson> collectionSuggestionAggregation(SearchSuggestionQuery query, boolean skipPermissions) {
      List<Bson> aggregates = new ArrayList<>();

      aggregates.add(Aggregates.match(collectionSuggestionQuery(query, skipPermissions)));

      if (query.hasPriorityCollectionIdsQuery()) {
         List<ObjectId> ids = query.getPriorityCollectionIds().stream().map(ObjectId::new).collect(Collectors.toList());
         Document condition = new Document("$cond",
               new Document("if",
                     new Document("$in", Arrays.asList("$" + CollectionCodec.ID, ids))
               ).append("then", 0)
                .append("else", 1));

         Bson project = Aggregates.addFields(new Field<>("_collectionPriority", condition));
         aggregates.add(project);

         Bson sort = Aggregates.sort(Sorts.ascending("_collectionPriority"));
         aggregates.add(sort);
      }

      addPaginationToAggregates(aggregates, query);

      return aggregates;
   }

   private Bson collectionSuggestionQuery(SearchSuggestionQuery query, boolean skipPermissions) {
      Bson regex = Filters.regex(CollectionCodec.NAME, Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
      if (skipPermissions) {
         return regex;
      }

      return Filters.and(regex, MongoFilters.permissionsFilter(query));
   }

   @Override
   public List<Collection> getCollectionsByAttributes(final SearchSuggestionQuery query, final boolean skipPermissions) {
      Bson filter = attributeSuggestionQuery(query, skipPermissions);
      return searchCollectionsByFilter(filter, query);
   }

   @Override
   public List<Collection> getCollectionsByPurpose(final CollectionPurposeType purposeType) {
      Bson filter = Filters.eq(MongoUtils.concatParams(CollectionCodec.PURPOSE, CollectionPurposeCodec.TYPE), purposeType.toString());
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   private Bson attributeSuggestionQuery(SearchSuggestionQuery query, boolean skipPermissions) {
      Bson regex = Filters.regex(MongoUtils.concatParams(CollectionCodec.ATTRIBUTES, AttributeCodec.NAME), Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
      if (skipPermissions) {
         return regex;
      }
      return Filters.and(regex, MongoFilters.permissionsFilter(query));
   }

   @Override
   public long getCollectionsCount() {
      return databaseCollection().countDocuments();
   }

   @Override
   public Set<String> getAllCollectionCodes() {
      return databaseCollection().find()
                                 .projection(Projections.include(CollectionCodec.CODE))
                                 .into(new ArrayList<>())
                                 .stream().map(Resource::getCode)
                                 .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getAllCollectionNames() {
      return databaseCollection().find()
                                 .projection(Projections.include(CollectionCodec.NAME))
                                 .into(new ArrayList<>())
                                 .stream().map(Resource::getName)
                                 .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getAllCollectionIds() {
      return databaseCollection().find()
                                 .projection(Projections.include(CollectionCodec.ID))
                                 .into(new ArrayList<>())
                                 .stream().map(Resource::getId)
                                 .collect(Collectors.toSet());
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

   MongoCollection<Collection> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Collection.class);
   }
}
