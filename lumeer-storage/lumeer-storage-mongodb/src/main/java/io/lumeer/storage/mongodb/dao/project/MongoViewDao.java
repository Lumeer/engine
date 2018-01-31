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

import static io.lumeer.storage.mongodb.util.MongoFilters.codeFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.codecs.QueryCodec;
import io.lumeer.storage.mongodb.codecs.ViewCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoViewDao extends ProjectScopedDao implements ViewDao {

   private static final String PREFIX = "views_p-";

   @Override
   public void createViewsRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(ViewCodec.CODE), new IndexOptions().unique(true));
      projectCollection.createIndex(Indexes.ascending(ViewCodec.NAME), new IndexOptions().unique(true));
      projectCollection.createIndex(Indexes.text(ViewCodec.NAME));
   }

   @Override
   public void deleteViewsRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   public View createView(final View view) {
      try {
         JsonView jsonView = new JsonView(view);
         databaseCollection().insertOne(jsonView);
         return jsonView;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create view: " + view, ex);
      }
   }

   public View updateView(final String id, final View view) {
      JsonView jsonView = new JsonView(view);
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      try {
         View updatedView = databaseCollection().findOneAndReplace(idFilter(id), jsonView, options);
         if (updatedView == null) {
            throw new StorageException("View '" + view.getId() + "' has not been updated.");
         }
         return updatedView;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update view: " + view, ex);
      }
   }

   public void deleteView(final String id) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("View '" + id + "' has not been deleted.");
      }
   }

   public View getViewByCode(final String code) {
      MongoCursor<JsonView> mongoCursor = databaseCollection().find(codeFilter(code)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.VIEW);
      }
      return mongoCursor.next();
   }

   public List<View> getViews(SearchQuery query) {
      FindIterable<JsonView> findIterable = databaseCollection().find(MongoViewDao.viewSearchFilter(query));
      if (query.hasPagination()) {
         findIterable.skip(query.getPage() * query.getPageSize())
                     .limit(query.getPageSize());
      }
      return findIterable.into(new ArrayList<>());
   }

   @Override
   public List<View> getViews(final SuggestionQuery query) {
      FindIterable<JsonView> findIterable  = databaseCollection().find(suggestionsFilter(query));
      addPaginationToSuggestionQuery(findIterable, query);
      return findIterable.into(new ArrayList<>());
   }

   private Bson suggestionsFilter(final SuggestionQuery query) {
      Bson regex = Filters.regex(ViewCodec.NAME, Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
      return Filters.and(regex, MongoFilters.permissionsFilter(query));
   }

   @Override
   public Set<String> getAllViewCodes() {
      return databaseCollection().find().projection(Projections.include(ViewCodec.CODE)).into(new ArrayList<>())
                                 .stream()
                                 .map(Resource::getCode)
                                 .collect(Collectors.toSet());
   }

   private static Bson viewSearchFilter(SearchQuery query) {
      List<Bson> filters = query.getCollectionCodes().stream()
                                .map(MongoViewDao::collectionFilter)
                                .collect(Collectors.toList());
      if (query.getFulltext() != null && !query.getFulltext().isEmpty()) {
         filters.add(Filters.text(query.getFulltext()));
      }
      filters.add(MongoFilters.permissionsFilter(query));

      return Filters.and(filters);
   }

   private static Bson collectionFilter(String collectionCode) {
      return Filters.eq(ViewCodec.QUERY + "." + QueryCodec.COLLECTION_CODES, collectionCode);
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

   MongoCollection<JsonView> databaseCollection() {
      return database.getCollection(databaseCollectionName(), JsonView.class);
   }
}
