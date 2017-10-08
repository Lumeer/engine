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
package io.lumeer.storage.mongodb;

import static io.lumeer.storage.mongodb.EmbeddedMongoDb.*;

import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.storage.mongodb.model.MorphiaView;

import com.mongodb.client.MongoDatabase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

public abstract class MongoDbTestBase {

   private static Morphia morphia = new Morphia().mapPackage(MorphiaView.class.getPackage().getName());
   private static EmbeddedMongoDb embeddedMongoDb;

   static {
      morphia.getMapper().getOptions().setStoreEmpties(true);
   }

   protected MongoDbStorage mongoDbStorage;

   protected MongoDatabase database;
   protected AdvancedDatastore datastore;

   @BeforeClass
   public static void startEmbeddedMongoDb() {
      embeddedMongoDb = new EmbeddedMongoDb();
      embeddedMongoDb.start();
   }

   @AfterClass
   public static void stopEmbeddedMongoDb() {
      if (embeddedMongoDb != null) {
         embeddedMongoDb.stop();
      }
   }

   @Before
   public void connectMongoDbStorage() {
      mongoDbStorage = new MongoDbStorage(morphia);
      mongoDbStorage.connect(new StorageConnection(HOST, PORT, USER, PASSWORD), NAME, SSL);

      database = mongoDbStorage.getDatabase();
      datastore = mongoDbStorage.getDataStore();

      database.drop();
   }

   @After
   public void disconnectMongoDbStorage() {
      if (mongoDbStorage != null) {
         mongoDbStorage.disconnect();
      }
   }
}
