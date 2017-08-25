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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaView;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MorphiaViewDao extends ProjectScopedDao implements ViewDao {

   private static final String PREFIX = "views_p-";

   @Override
   public void createViewsRepository(Project project) {
      database.createCollection(databaseCollection(project));
      datastore.ensureIndexes(databaseCollection(project), MorphiaView.class);
   }

   @Override
   public void deleteViewsRepository(Project project) {
      database.getCollection(databaseCollection(project)).drop();
   }

   public View createView(final View view) {
      MorphiaView morphiaView = new MorphiaView(view);
      datastore.insert(databaseCollection(), morphiaView);
      return morphiaView;
   }

   public View updateView(final String id, final View view) {
      MorphiaView morphiaView = new MorphiaView(view);
      morphiaView.setId(id);
      datastore.save(databaseCollection(), morphiaView);
      return morphiaView;
   }

   public void deleteView(final String id) {
      WriteResult writeResult = datastore.delete(databaseCollection(), MorphiaView.class, new ObjectId(id));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   public View getViewByCode(final String code) {
      View view = datastore.createQuery(databaseCollection(), MorphiaView.class)
                           .field(MorphiaView.CODE).equal(code)
                           .get();
      if (view == null) {
         throw new ResourceNotFoundException(ResourceType.VIEW);
      }
      return view;
   }

   public List<View> getViews(SearchQuery query) {
      Query<MorphiaView> viewQuery = createViewSearchQuery(query);
      FindOptions findOptions = createFindOptions(query);

      return new ArrayList<>(viewQuery.asList(findOptions));
   }

   @Override
   public List<View> getViews(final SuggestionQuery query) {
      Query<MorphiaView> viewQuery = createViewSuggestionQuery(query);
      FindOptions findOptions = createFindOptions(query);

      return new ArrayList<>(viewQuery.asList(findOptions));
   }

   private Query<MorphiaView> createViewSearchQuery(SearchQuery query) {
      Query<MorphiaView> viewQuery = datastore.createQuery(databaseCollection(), MorphiaView.class);

      if (query.isFulltextQuery()) {
         viewQuery.search(query.getFulltext());
      }
      viewQuery.or(createPermissionsCriteria(viewQuery, query));

      return viewQuery;
   }

   private Query<MorphiaView> createViewSuggestionQuery(SuggestionQuery query) {
      Query<MorphiaView> viewQuery = datastore.createQuery(databaseCollection(), MorphiaView.class);

      viewQuery.field(MorphiaView.NAME).startsWithIgnoreCase(query.getText());
      viewQuery.or(createPermissionsCriteria(viewQuery, query));

      return viewQuery;
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
