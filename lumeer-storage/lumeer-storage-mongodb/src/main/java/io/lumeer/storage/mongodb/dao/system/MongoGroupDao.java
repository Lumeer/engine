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
package io.lumeer.storage.mongodb.dao.system;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.GroupCodec;
import io.lumeer.storage.mongodb.dao.organization.OrganizationScopedDao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoGroupDao extends OrganizationScopedDao implements GroupDao {

   private static final String PREFIX = "groups_o-";

   @Override
   public void createGroupsRepository(Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      MongoCollection<Document> groupCollection = database.getCollection(databaseCollectionName(organization));
      groupCollection.createIndex(Indexes.ascending(GroupCodec.NAME), new IndexOptions().unique(true));
   }

   @Override
   public void deleteGroupsRepository(Organization organization) {
      database.getCollection(databaseCollectionName(organization)).drop();
   }

   @Override
   public Group createGroup( final Group group) {
      try {
         databaseCollection().insertOne(group);
         return group;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create group " + group, ex);
      }
   }

   @Override
   public Group updateGroup(final String id, final Group group) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         Group returnedGroup = databaseCollection().findOneAndReplace(idFilter(id), group, options);
         if (returnedGroup == null) {
            throw new StorageException("Group '" + id + "' has not been updated.");
         }
         return returnedGroup;
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
   public List<Group> getAllGroups() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   MongoCollection<Group> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Group.class);
   }

   String databaseCollectionName() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollectionName(getOrganization().get());
   }

   private String databaseCollectionName(Organization organization) {
      return PREFIX + organization.getId();
   }
}
