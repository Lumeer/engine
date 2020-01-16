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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import io.lumeer.api.model.DefaultViewConfig;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.UpdateDefaultViewConfig;
import io.lumeer.storage.api.dao.DefaultViewConfigDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.DefaultViewConfigCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoDefaultViewConfigDao extends MongoProjectScopedDao implements DefaultViewConfigDao {

   private static final String PREFIX = "default_configs_p-";

   @Inject
   private Event<UpdateDefaultViewConfig> updateDefaultViewConfigEvent;

   @Override
   public void createRepository(final Project project) {
      database.createCollection(getCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(getCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(DefaultViewConfigCodec.USER_ID, DefaultViewConfigCodec.KEY, DefaultViewConfigCodec.PERSPECTIVE), new IndexOptions().unique(true));
   }

   @Override
   public void deleteRepository(final Project project) {
      database.getCollection(getCollectionName(project)).drop();
   }

   @Override
   public DefaultViewConfig updateConfig(final DefaultViewConfig config) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                                                                     .upsert(true);
      try {
         Bson update = new Document("$set", config);
         final DefaultViewConfig returnedConfig = databaseCollection().findOneAndUpdate(configFilter(config), update, options);
         if (updateDefaultViewConfigEvent != null) {
            updateDefaultViewConfigEvent.fire(new UpdateDefaultViewConfig(returnedConfig));
         }
         return returnedConfig;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update default view config " + config, ex);
      }
   }

   private Bson configFilter(final DefaultViewConfig config) {
      return and(
            eq(DefaultViewConfigCodec.USER_ID, config.getUserId()),
            eq(DefaultViewConfigCodec.KEY, config.getKey()),
            eq(DefaultViewConfigCodec.PERSPECTIVE, config.getPerspective())
      );
   }

   @Override
   public List<DefaultViewConfig> getConfigs(final String userId) {
      return databaseCollection().find(eq(DefaultViewConfigCodec.USER_ID, userId)).into(new ArrayList<>());
   }

   @Override
   public void deleteByCollection(final String collectionId) {
      databaseCollection().deleteMany(eq(DefaultViewConfigCodec.KEY, collectionId));
   }

   public String getCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   String getDatabaseCollectionName() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return getCollectionName(getProject().get());
   }

   MongoCollection<DefaultViewConfig> databaseCollection() {
      return database.getCollection(getDatabaseCollectionName(), DefaultViewConfig.class);
   }
}
