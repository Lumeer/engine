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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchSuggestionQuery;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.QueryCodec;
import io.lumeer.storage.mongodb.codecs.QueryStemCodec;
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
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoViewDao extends ProjectScopedDao implements ViewDao {

   private static final String PREFIX = "views_p-";

   @Inject
   private Event<CreateResource> createResourceEvent;

   @Inject
   private Event<UpdateResource> updateResourceEvent;

   @Inject
   private Event<RemoveResource> removeResourceEvent;

   @Override
   public void createViewsRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(ViewCodec.CODE), new IndexOptions().unique(true));
      projectCollection.createIndex(Indexes.ascending(ViewCodec.NAME), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.text(ViewCodec.NAME));
   }

   @Override
   public void deleteViewsRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public View createView(final View view) {
      try {
         databaseCollection().insertOne(view);
         if (createResourceEvent != null) {
            createResourceEvent.fire(new CreateResource(view));
         }
         return view;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create view: " + view, ex);
      }
   }

   @Override
   public View updateView(final String id, final View view) {
      return updateView(id, view, null);
   }

   @Override
   public View updateView(final String id, final View view, final View originalView) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      try {
         View updatedView = databaseCollection().findOneAndReplace(idFilter(id), view, options);
         if (updatedView == null) {
            throw new StorageException("View '" + id + "' has not been updated.");
         }

         checkRemovedPermissions(originalView, updatedView);
         if (updateResourceEvent != null) {
            updateResourceEvent.fire(new UpdateResource(updatedView, originalView));
         }
         return updatedView;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update view: " + view, ex);
      }
   }

   @Override
   public void deleteView(final String id) {
      final View view = databaseCollection().findOneAndDelete(idFilter(id));
      if (view == null) {
         throw new StorageException("View '" + id + "' has not been deleted.");
      }
      if (removeResourceEvent != null) {
         removeResourceEvent.fire(new RemoveResource(view));
      }
   }

   @Override
   public View getViewByCode(final String code) {
      MongoCursor<View> mongoCursor = databaseCollection().find(codeFilter(code)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.VIEW);
      }
      return mongoCursor.next();
   }

   @Override
   public List<View> getAllViews() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   @Override
   public List<View> getViews(DatabaseQuery query) {
      FindIterable<View> findIterable = databaseCollection().find(MongoFilters.permissionsFilter(query));
      addPaginationToQuery(findIterable, query);
      return findIterable.into(new ArrayList<>());
   }

   @Override
   public List<View> getViews(final SearchSuggestionQuery query, boolean skipPermissions) {
      FindIterable<View> findIterable = databaseCollection().find(suggestionsFilter(query, skipPermissions));
      addPaginationToQuery(findIterable, query);
      return findIterable.into(new ArrayList<>());
   }

   private Bson suggestionsFilter(final SearchSuggestionQuery query, boolean skipPermissions) {
      Bson regex = Filters.regex(ViewCodec.NAME, Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
      if (skipPermissions) {
         return regex;
      }
      return Filters.and(regex, MongoFilters.permissionsFilter(query));
   }

   @Override
   public List<View> getViewsByLinkTypeIds(final List<String> linkTypeIds) {
      FindIterable<View> findIterable = databaseCollection().find(
            Filters.in(MongoUtils.concatParams(ViewCodec.QUERY, QueryCodec.STEMS, QueryStemCodec.LINK_TYPE_IDS), linkTypeIds)
      );
      return findIterable.into(new ArrayList<>());
   }

   @Override
   public List<View> getViewsByCollectionId(final String collectionId) {
      FindIterable<View> findIterable = databaseCollection().find(
            Filters.in(MongoUtils.concatParams(ViewCodec.QUERY, QueryCodec.STEMS, QueryStemCodec.COLLECTION_ID), collectionId)
      );
      return findIterable.into(new ArrayList<>());
   }

   @Override
   public Set<String> getAllViewCodes() {
      return databaseCollection().find().projection(Projections.include(ViewCodec.CODE)).into(new ArrayList<>())
                                 .stream()
                                 .map(Resource::getCode)
                                 .collect(Collectors.toSet());
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

   MongoCollection<View> databaseCollection() {
      return database.getCollection(databaseCollectionName(), View.class);
   }
}
