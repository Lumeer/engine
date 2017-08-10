/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.storage.mongodb;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public abstract class MongoDbTestBase {

   protected static final String DB_HOST = System.getProperty("lumeer.db.host", "localhost");
   protected static final String DB_NAME = System.getProperty("lumeer.db.name", "lumeer-test");
   protected static final int DB_PORT = Integer.getInteger("lumeer.db.port", 27017);
   protected static final String DB_USER = System.getProperty("lumeer.db.user", "");
   protected static final String DB_PASSWORD = System.getProperty("lumeer.db.passwd", "");
   protected static final Boolean DB_SSL = Boolean.getBoolean("lumeer.db.ssl");

   private static MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
   private static MongodExecutable mongodExecutable;
   private static MongodProcess mongodProcess;

   @BeforeClass
   public static void startEmbeddedMongoDb() throws Exception {
      if (!"localhost".equals(DB_HOST)) {
         // do not start embedded MongoDB when remote database is used
         return;
      }

      IMongodConfig mongodConfig = new MongodConfigBuilder()
            .version(Version.Main.V3_4)
            .net(new Net(DB_HOST, DB_PORT, Network.localhostIsIPv6()))
            .build();

      mongodExecutable = mongodStarter.prepare(mongodConfig);
      mongodProcess = mongodExecutable.start();
   }

   @AfterClass
   public static void stopEmbeddedMongoDb() {
      if (mongodExecutable != null && mongodProcess != null && mongodProcess.isProcessRunning()) {
         mongodProcess.stop();
         mongodExecutable.stop();
      }
   }
}
