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

import io.lumeer.api.model.DashboardData;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.UpdateDashboardData;
import io.lumeer.storage.api.dao.DashboardDataDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.DashboardDataCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@RequestScoped
public class MongoDashboardDataDao extends MongoProjectScopedDao implements DashboardDataDao {

   private static final String PREFIX = "dashboard-data_p-";

   @Inject
   private Event<UpdateDashboardData> updateEvent;

   @Override
   public void createRepository(final Project project) {
      database.createCollection(getCollectionName(project));

      ensureIndexes(project);
   }

   @Override
   public void ensureIndexes(final Project project) {
      MongoCollection<Document> projectCollection = database.getCollection(getCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(DashboardDataCodec.USER_ID, DashboardDataCodec.TYPE, DashboardDataCodec.TYPE_ID), new IndexOptions().unique(true));
   }

   @Override
   public void deleteRepository(final Project project) {
      database.getCollection(getCollectionName(project)).drop();
   }

   @Override
   public DashboardData update(final DashboardData data) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                                                                     .upsert(true);
      try {
         Bson update = new Document("$set", data);
         final DashboardData returnedData = databaseCollection().findOneAndUpdate(dataFilter(data), update, options);
         if (updateEvent != null) {
            updateEvent.fire(new UpdateDashboardData(returnedData));
         }
         return returnedData;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update dashboard data " + data, ex);
      }
   }

   private Bson dataFilter(final DashboardData data) {
      return dataFilter(data.getType(), data.getTypeId(), data.getUserId());
   }

   private Bson dataFilter(final String type, final String typeId, final String userId) {
      return and(
            eq(DashboardDataCodec.USER_ID, userId),
            eq(DashboardDataCodec.TYPE, type),
            eq(DashboardDataCodec.TYPE_ID, typeId)
      );
   }

   @Override
   public List<DashboardData> getByUserId(final String userId) {
      return databaseCollection().find(eq(DashboardDataCodec.USER_ID, userId)).into(new ArrayList<>());
   }

   @Override
   public DashboardData getByTypeId(final String type, final String typeId, final String userId) {
      MongoCursor<DashboardData> mongoCursor = databaseCollection().find(dataFilter(type, typeId, userId)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new StorageException("Could not found dashboard data");
      }
      return mongoCursor.next();
   }

   @Override
   public void delete(final String type, final Set<String> typeIds, final String userId) {
      Bson filter = and(
            eq(DashboardDataCodec.USER_ID, userId),
            eq(DashboardDataCodec.TYPE, type),
            in(DashboardDataCodec.TYPE_ID, typeIds)
      );

      databaseCollection().deleteMany(filter);
   }

   public String getCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   String getDatabaseCollectionName() {
      if (getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return getCollectionName(getProject().get());
   }

   MongoCollection<DashboardData> databaseCollection() {
      return database.getCollection(getDatabaseCollectionName(), DashboardData.class);
   }
}
