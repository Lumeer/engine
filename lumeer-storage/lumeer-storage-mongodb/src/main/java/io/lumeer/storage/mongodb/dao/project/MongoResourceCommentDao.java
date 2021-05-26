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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.CreateResourceComment;
import io.lumeer.engine.api.event.RemoveResourceComment;
import io.lumeer.engine.api.event.UpdateResourceComment;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.ResourceCommentCodec;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoResourceCommentDao extends MongoProjectScopedDao implements ResourceCommentDao {

   private static final String PREFIX = "comments_p-";

   @Inject
   private Event<CreateResourceComment> createResourceCommentEvent;

   @Inject
   private Event<UpdateResourceComment> updateResourceCommentEvent;

   @Inject
   private Event<RemoveResourceComment> removeResourceCommentEvent;

   @Override
   public void createRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));
      ensureIndexes(project);
   }

   @Override
   public void ensureIndexes(final Project project) {
      MongoCollection<ResourceComment> collection = databaseCollection(databaseCollectionName(project));
      collection.createIndex(Indexes.ascending(ResourceCommentCodec.RESOURCE_TYPE, ResourceCommentCodec.RESOURCE_ID), new IndexOptions().unique(false));
      collection.createIndex(Indexes.ascending(ResourceCommentCodec.RESOURCE_TYPE, ResourceCommentCodec.PARENT_ID, ResourceCommentCodec.RESOURCE_ID), new IndexOptions().unique(false));
      collection.createIndex(Indexes.descending(ResourceCommentCodec.RESOURCE_TYPE, ResourceCommentCodec.RESOURCE_ID, ResourceCommentCodec.CREATION_DATE), new IndexOptions().unique(false));
      collection.createIndex(Indexes.ascending(ResourceCommentCodec.AUTHOR), new IndexOptions().unique(false));
   }

   @Override
   public void deleteRepository(final Project project) {
      databaseCollection().drop();
   }

   @Override
   public ResourceComment createComment(final ResourceComment comment) {
      try {
         databaseCollection().insertOne(comment);

         if (createResourceCommentEvent != null) {
            createResourceCommentEvent.fire(new CreateResourceComment(comment));
         }

         return comment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create comment: " + comment, ex);
      }
   }

   @Override
   public ResourceComment getComment(final String id) {
      try {
         return databaseCollection().find(idFilter(id)).first();
      } catch (MongoException ex) {
         throw new StorageException("Cannot find comment id: " + id, ex);
      }
   }

   @Override
   public long getCommentsCount(final ResourceType resourceType, final String resourceId) {
      return databaseCollection().countDocuments(
            Filters.and(
                  Filters.eq(ResourceCommentCodec.RESOURCE_TYPE, resourceType.toString()),
                  Filters.eq(ResourceCommentCodec.RESOURCE_ID, resourceId))
      );
   }

   @Override
   public Map<String, Integer> getCommentsCounts(final ResourceType resourceType, final Set<String> resourceIds) {
      final List<Document> result = database.getCollection(databaseCollectionName()).aggregate(
            List.of(
                  Aggregates.match(
                        Filters.and(
                              Filters.eq(ResourceCommentCodec.RESOURCE_TYPE, resourceType.toString()),
                              Filters.in(ResourceCommentCodec.RESOURCE_ID, resourceIds))),
                  Aggregates.group("$"+ResourceCommentCodec.RESOURCE_ID, Accumulators.sum("count", 1))
            )
      ).into(new ArrayList<>());

      return result.stream().collect(Collectors.toMap(e -> e.getString("_id"), e -> e.getInteger("count")));
   }

   @Override
   public Map<String, Integer> getCommentsCounts(final ResourceType resourceType, final String parentId) {
      final List<Document> result = database.getCollection(databaseCollectionName()).aggregate(
            List.of(
                  Aggregates.match(
                        Filters.and(
                              Filters.eq(ResourceCommentCodec.RESOURCE_TYPE, resourceType.toString()),
                              Filters.eq(ResourceCommentCodec.PARENT_ID, parentId))),
                  Aggregates.group("$"+ResourceCommentCodec.RESOURCE_ID, Accumulators.sum("count", 1))
            )
      ).into(new ArrayList<>());

      return result.stream().collect(Collectors.toMap(e -> e.getString("_id"), e -> e.getInteger("count")));
   }

   @Override
   public ResourceComment updateComment(final ResourceComment comment) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Bson update = new org.bson.Document("$set", comment);
         ResourceComment updatedComment = databaseCollection().findOneAndUpdate(idFilter(comment.getId()), update, options);

         if (updatedComment == null) {
            throw new StorageException("Comment '" + comment.getId() + "' has not been updated.");
         }

         if (updateResourceCommentEvent != null) {
            updateResourceCommentEvent.fire(new UpdateResourceComment(comment));
         }

         return updatedComment;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update comment: " + comment, ex);
      }
   }

   @Override
   public boolean deleteComment(final ResourceComment comment) {
      ResourceComment originalComment = databaseCollection().findOneAndDelete(idFilter(comment.getId()));
      if (originalComment == null) {
         throw new StorageException("Comment '" + comment.getId() + "' has not been deleted.");
      }

      if (removeResourceCommentEvent != null) {
         removeResourceCommentEvent.fire(new RemoveResourceComment(comment));
      }

      return true;
   }

   @Override
   public boolean deleteComments(final ResourceType resourceType, final String resourceId) {
      DeleteResult result = databaseCollection().deleteMany(
            Filters.and(
                  Filters.eq(ResourceCommentCodec.RESOURCE_TYPE, resourceType.toString()),
                  Filters.eq(ResourceCommentCodec.RESOURCE_ID, resourceId)));

      // no event is fired here as this method only occurs when the resource is deleted completely

      return result.wasAcknowledged();
   }

   @Override
   public List<ResourceComment> getResourceComments(final ResourceType resourceType, final String resourceId, final int pageStart, final int pageLenght) {
      final FindIterable<ResourceComment> result = databaseCollection().find(
            Filters.and(
                  Filters.eq(ResourceCommentCodec.RESOURCE_TYPE, resourceType.toString()),
                  Filters.eq(ResourceCommentCodec.RESOURCE_ID, resourceId))
      ).sort(Sorts.descending(ResourceCommentCodec.CREATION_DATE));

      if (pageLenght > 0) {
         result.skip(pageStart).limit(pageLenght);
      }

      return result.into(new ArrayList<>());
   }

   @Override
   public long updateParentId(final ResourceType resourceType, final String resourceId, final String parentId) {
      final UpdateResult result = databaseCollection().updateMany(
            Filters.and(
                  Filters.eq(ResourceCommentCodec.RESOURCE_TYPE, resourceType.toString()),
                  Filters.eq(ResourceCommentCodec.RESOURCE_ID, resourceId)),
            Updates.set(ResourceCommentCodec.PARENT_ID, parentId)
      );

      return result.getModifiedCount();
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

   MongoCollection<ResourceComment> databaseCollection() {
      return databaseCollection(databaseCollectionName());
   }

   MongoCollection<ResourceComment> databaseCollection(final String collectionName) {
      return database.getCollection(collectionName, ResourceComment.class);
   }

}
