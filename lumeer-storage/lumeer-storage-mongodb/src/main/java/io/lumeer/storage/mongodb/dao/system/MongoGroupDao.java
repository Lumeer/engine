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
package io.lumeer.storage.mongodb.dao.system;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Group;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.GroupCodec;
import io.lumeer.storage.mongodb.model.MongoGroup;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoGroupDao extends SystemScopedDao implements GroupDao {

   private static final String COLLECTION_NAME = "groups";

   @PostConstruct
   public void checkRepository() {
      if (database.getCollection(COLLECTION_NAME) == null) {
         createGroupsRepository();
      }
   }

   public void createGroupsRepository() {
      database.createCollection(databaseCollectionName());

      MongoCollection<Document> groupCollection = database.getCollection(databaseCollectionName());
      groupCollection.createIndex(Indexes.ascending(GroupCodec.ORGANIZATION_ID, GroupCodec.NAME), new IndexOptions().unique(true));
   }

   public void deleteGroupsRepository() {
      database.getCollection(databaseCollectionName()).drop();
   }

   @Override
   public Group createGroup(final String organizationId, final Group group) {
      MongoGroup mongoGroup = new MongoGroup(group, organizationId);
      try {
         databaseCollection().insertOne(mongoGroup);
         return mongoGroup.toGroup();
      } catch (MongoException ex) {
         throw new StorageException("Cannot create group " + group, ex);
      }
   }

   @Override
   public Group updateGroup(final String id, final Group group) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
      Bson update = Updates.set(GroupCodec.NAME, group.getName());
      try {
         MongoGroup returnedGroup = databaseCollection().findOneAndUpdate(idFilter(id), update, options);
         if (returnedGroup == null) {
            throw new StorageException("Group '" + id + "' has not been updated.");
         }
         return returnedGroup.toGroup();
      } catch (MongoException ex) {
         throw new StorageException("Cannot update group " + group, ex);
      }
   }

   @Override
   public void deleteGroup(final String id) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Group '" + id + "' has not been deleted.");
      }
   }

   @Override
   public List<Group> getAllGroups(final String organizationId) {
      Bson filter = Filters.eq(GroupCodec.ORGANIZATION_ID, organizationId);
      return databaseCollection().find(filter).map(MongoGroup::toGroup).into(new ArrayList<>());
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<MongoGroup> databaseCollection() {
      return database.getCollection(databaseCollectionName(), MongoGroup.class);
   }
}
