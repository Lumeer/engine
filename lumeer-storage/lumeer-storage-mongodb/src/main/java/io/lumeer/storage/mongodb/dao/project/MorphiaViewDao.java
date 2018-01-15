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
import java.util.Set;
import java.util.stream.Collectors;
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

   @Override
   public Set<String> getAllViewCodes() {
      return datastore.createQuery(databaseCollection(), MorphiaView.class)
                      .project(MorphiaView.CODE, true)
                      .asList().stream()
                      .map(MorphiaView::getCode)
                      .collect(Collectors.toSet());
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
