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
package io.lumeer.storage.mongodb.dao.organization;

import io.lumeer.api.model.InitialUserData;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.InitialUserDataDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.InitialUserDataCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoInitialUserDataDao extends MongoOrganizationScopedDao implements InitialUserDataDao {

   private static final String PREFIX = "initialUserData_o-";

   @Override
   public void createRepository(Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      ensureIndexes(organization);
   }

   @Override
   public void deleteRepository(Organization organization) {
      database.getCollection(databaseCollectionName(organization)).drop();
   }

   @Override
   public void ensureIndexes(final Organization organization) {
      MongoCollection<Document> collection = database.getCollection(databaseCollectionName(organization));

      final List<Document> indexes = collection.listIndexes().into(new ArrayList<>());

      if (indexes.isEmpty()) {
         collection.createIndex(Indexes.ascending(InitialUserDataCodec.PROJECT_ID), new IndexOptions().unique(true));
      }
   }

   @Override
   public InitialUserData upsert(final InitialUserData data) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         return databaseCollection().findOneAndReplace(dataFilter(data), data, options);
      } catch (MongoException ex) {
         throw new StorageException("Cannot update initial user data " + data, ex);
      }
   }

   private Bson dataFilter(InitialUserData data) {
      return Filters.eq(InitialUserDataCodec.PROJECT_ID, data.getProjectId());
   }

   @Override
   public void delete(final InitialUserData data) {
      DeleteResult result = databaseCollection().deleteOne(dataFilter(data));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Initial user data '" + data + "' has not been deleted.");
      }
   }

   @Override
   public List<InitialUserData> get() {
      return databaseCollection().find(new Document()).into(new ArrayList<>());
   }

   MongoCollection<InitialUserData> databaseCollection() {
      return database.getCollection(databaseCollectionName(), InitialUserData.class);
   }

   String databaseCollectionName() {
      if (getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollectionName(getOrganization().get());
   }

   private String databaseCollectionName(Organization organization) {
      return databaseCollectionName(organization.getId());
   }

   private String databaseCollectionName(String organizationId) {
      return PREFIX + organizationId;
   }
}
