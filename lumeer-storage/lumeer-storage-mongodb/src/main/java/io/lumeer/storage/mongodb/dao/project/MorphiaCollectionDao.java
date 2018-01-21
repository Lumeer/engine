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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaCollection;
import io.lumeer.storage.mongodb.model.common.MorphiaResource;
import io.lumeer.storage.mongodb.model.embedded.MorphiaAttribute;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MorphiaCollectionDao extends ProjectScopedDao implements CollectionDao {

   private static final String PREFIX = "collections_p-";

   @Override
   public void createCollectionsRepository(Project project) {
      database.createCollection(databaseCollection(project));
      datastore.ensureIndexes(databaseCollection(project), MorphiaCollection.class);
   }

   @Override
   public void deleteCollectionsRepository(Project project) {
      database.getCollection(databaseCollection(project)).drop();
   }

   @Override
   public Collection createCollection(final Collection collection) {
      MorphiaCollection morphiaCollection = new MorphiaCollection(collection);
      datastore.insert(databaseCollection(), morphiaCollection);
      return morphiaCollection;
   }

   @Override
   public Collection updateCollection(final String id, final Collection collection) {
      MorphiaCollection morphiaCollection = new MorphiaCollection(collection);
      morphiaCollection.setId(id);
      datastore.save(databaseCollection(), morphiaCollection);
      return morphiaCollection;
   }

   @Override
   public void deleteCollection(final String id) {
      WriteResult writeResult = datastore.delete(databaseCollection(), MorphiaCollection.class, new ObjectId(id));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Collection getCollectionByCode(final String code) {
      Collection collection = datastore.createQuery(databaseCollection(), MorphiaCollection.class)
                                       .field(MorphiaCollection.CODE).equal(code)
                                       .get();
      if (collection == null) {
         throw new ResourceNotFoundException(ResourceType.COLLECTION);
      }
      return collection;
   }

   @Override
   public List<Collection> getCollectionsByIds(final java.util.Collection<String> ids) {
      List<ObjectId> objectIds = ids.stream().map(ObjectId::new).collect(Collectors.toList());
      return new ArrayList<>(datastore.createQuery(databaseCollection(), MorphiaCollection.class)
            .field(MorphiaCollection.ID).in(objectIds).asList());
   }

   @Override
   public List<Collection> getCollections(final SearchQuery query) {
      return getCollections(createCollectionSearchQuery(query), query);
   }

   @Override
   public List<Collection> getCollections(final SuggestionQuery query) {
      return getCollections(createCollectionSuggestionQuery(query), query);
   }

   @Override
   public List<Collection> getCollectionsByAttributes(final SuggestionQuery query) {
      return getCollections(createAttributeSuggestionQuery(query), query);
   }

   @Override
   public Set<String> getAllCollectionCodes() {
      return datastore.createQuery(databaseCollection(), MorphiaCollection.class)
                      .project(MorphiaCollection.CODE, true)
                      .asList().stream()
                      .map(MorphiaResource::getCode)
                      .collect(Collectors.toSet());
   }

   private List<Collection> getCollections(Query<MorphiaCollection> morphiaQuery, DatabaseQuery databaseQuery) {
      FindOptions findOptions = createFindOptions(databaseQuery);
      return new ArrayList<>(morphiaQuery.asList(findOptions));
   }

   private Query<MorphiaCollection> createCollectionSearchQuery(SearchQuery searchQuery) {
      Query<MorphiaCollection> mongoQuery = datastore.createQuery(databaseCollection(), MorphiaCollection.class);

      mongoQuery.or(createPermissionsCriteria(mongoQuery, searchQuery));

      return searchQuery.isBasicQuery() ? mongoQuery : createAdvancedQuery(mongoQuery, searchQuery);
   }

   private Query<MorphiaCollection> createAdvancedQuery(Query<MorphiaCollection> mongoQuery, SearchQuery searchQuery) {
      if (searchQuery.isFulltextQuery()) {
         mongoQuery.search(searchQuery.getFulltext());
      }
      if (searchQuery.isCollectionCodesQuery()) {
         mongoQuery.field(MorphiaCollection.CODE).in(searchQuery.getCollectionCodes());
      }
      return mongoQuery;
   }

   private Query<MorphiaCollection> createCollectionSuggestionQuery(SuggestionQuery suggestionQuery) {
      Query<MorphiaCollection> mongoQuery = datastore.createQuery(databaseCollection(), MorphiaCollection.class);

      mongoQuery.or(createPermissionsCriteria(mongoQuery, suggestionQuery));
      mongoQuery.field(MorphiaCollection.NAME).startsWithIgnoreCase(suggestionQuery.getText());

      return mongoQuery;
   }

   private Query<MorphiaCollection> createAttributeSuggestionQuery(SuggestionQuery suggestionQuery) {
      Query<MorphiaCollection> mongoQuery = datastore.createQuery(databaseCollection(), MorphiaCollection.class);

      mongoQuery.or(createPermissionsCriteria(mongoQuery, suggestionQuery));
      mongoQuery.field(MorphiaCollection.ATTRIBUTES + "." + MorphiaAttribute.NAME).startsWithIgnoreCase(suggestionQuery.getText());

      return mongoQuery;
   }

   private String databaseCollection(Project project) {
      return PREFIX + project.getId();
   }

   String databaseCollection() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollection(getProject().get());
   }

}
