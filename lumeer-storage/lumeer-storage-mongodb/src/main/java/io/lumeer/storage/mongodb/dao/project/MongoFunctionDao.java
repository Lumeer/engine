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

import static com.mongodb.client.model.Filters.*;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.function.FunctionResourceType;
import io.lumeer.api.model.function.FunctionRow;
import io.lumeer.storage.api.dao.FunctionDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.FunctionRowCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoFunctionDao extends MongoProjectScopedDao implements FunctionDao {

   private static final String PREFIX = "functions_p-";

   @Override
   public void createRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(FunctionRowCodec.RESOURCE_ID, FunctionRowCodec.TYPE, FunctionRowCodec.ATTRIBUTE_ID), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.ascending(FunctionRowCodec.DEPENDENT_COLLECTION_ID, FunctionRowCodec.DEPENDENT_ATTRIBUTE_ID), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.ascending(FunctionRowCodec.DEPENDENT_LINK_TYPE_ID, FunctionRowCodec.DEPENDENT_ATTRIBUTE_ID), new IndexOptions().unique(false));
   }

   @Override
   public void deleteRepository(final Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public void createRows(final List<FunctionRow> rows) {
      try {
         databaseCollection().insertMany(rows);
      } catch (MongoException ex) {
         throw new StorageException("Cannot create function rows: " + rows, ex);
      }
   }

   @Override
   public List<FunctionRow> searchByAnyCollection(final String collectionId, final String attributeId) {
      Bson filter = or(
            collectionFilter(collectionId, attributeId),
            dependentCollectionFilter(collectionId, attributeId)
      );
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   private Bson collectionFilter(final String collectionId, final String attributeId) {
      return resourceFilter(collectionId, FunctionResourceType.COLLECTION, attributeId);
   }

   private Bson resourceFilter(final String resourceId, final FunctionResourceType type, final String attributeId) {
      List<Bson> filters = new ArrayList<>(Arrays.asList(eq(FunctionRowCodec.RESOURCE_ID, resourceId), eq(FunctionRowCodec.TYPE, type.toString())));
      if (attributeId != null) {
         filters.add(eq(FunctionRowCodec.ATTRIBUTE_ID, attributeId));
      }
      return and(filters);
   }

   private Bson dependentCollectionFilter(final String collectionId, final String attributeId) {
      if (attributeId != null) {
         return and(eq(FunctionRowCodec.DEPENDENT_COLLECTION_ID, collectionId), eq(FunctionRowCodec.DEPENDENT_ATTRIBUTE_ID, attributeId));
      }
      return eq(FunctionRowCodec.DEPENDENT_COLLECTION_ID, collectionId);
   }

   @Override
   public List<FunctionRow> searchByDependentCollection(final String collectionId, final String attributeId) {
      return databaseCollection().find(dependentCollectionFilter(collectionId, attributeId)).into(new ArrayList<>());
   }

   @Override
   public List<FunctionRow> searchByAnyLinkType(final String linkTypeId, final String attributeId) {
      Bson filter = or(
            linkFilter(linkTypeId, attributeId),
            dependentLinkFilter(linkTypeId, attributeId)
      );
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   private Bson linkFilter(final String linkTypeId, final String attributeId) {
      return resourceFilter(linkTypeId, FunctionResourceType.LINK, attributeId);
   }

   private Bson dependentLinkFilter(final String linkTypeId, final String attributeId) {
      if (attributeId != null) {
         return and(eq(FunctionRowCodec.DEPENDENT_LINK_TYPE_ID, linkTypeId), eq(FunctionRowCodec.DEPENDENT_ATTRIBUTE_ID, attributeId));
      }
      return eq(FunctionRowCodec.DEPENDENT_LINK_TYPE_ID, linkTypeId);
   }

   @Override
   public List<FunctionRow> searchByDependentLinkType(final String linkTypeId, final String attributeId) {
      return databaseCollection().find(dependentLinkFilter(linkTypeId, attributeId)).into(new ArrayList<>());
   }

   @Override
   public List<FunctionRow> searchByResource(final String resourceId, final String attributeId, final FunctionResourceType type) {
      return databaseCollection().find(resourceFilter(resourceId, type, attributeId)).into(new ArrayList<>());
   }

   @Override
   public void deleteByResources(final FunctionResourceType type, final String... resourceIds) {
      Bson filter = and(in(FunctionRowCodec.RESOURCE_ID, resourceIds), eq(FunctionRowCodec.TYPE, type.toString()));
      databaseCollection().deleteMany(filter);
   }

   @Override
   public void deleteByCollection(final String collectionsId, final String attributeId) {
      databaseCollection().deleteMany(collectionFilter(collectionsId, attributeId));
   }

   @Override
   public void deleteByLinkType(final String linkTypeId, final String attributeId) {
      databaseCollection().deleteMany(linkFilter(linkTypeId, attributeId));
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

   MongoCollection<FunctionRow> databaseCollection() {
      return database.getCollection(databaseCollectionName(), FunctionRow.class);
   }
}
