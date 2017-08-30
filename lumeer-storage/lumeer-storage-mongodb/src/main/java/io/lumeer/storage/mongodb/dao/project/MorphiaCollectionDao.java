/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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
import io.lumeer.storage.mongodb.model.MorphiaView;
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
      datastore.ensureIndexes(databaseCollection(project), MorphiaView.class);
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
   public List<Collection> getAllCollections() {
      return new ArrayList<>(datastore.createQuery(databaseCollection(), MorphiaCollection.class).asList());
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
