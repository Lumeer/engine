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

import java.io.IOException;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class EmbeddedMongoDb {

   public static final String HOST = System.getProperty("lumeer.db.host", "localhost");
   public static final String NAME = System.getProperty("lumeer.db.name", "lumeer-test");
   public static final int PORT = Integer.getInteger("lumeer.db.port", 27017);
   public static final String USER = System.getProperty("lumeer.db.user", "");
   public static final String PASSWORD = System.getProperty("lumeer.db.passwd", "");
   public static final Boolean SSL = Boolean.getBoolean("lumeer.db.ssl");

   private static final MongodStarter mongodStarter = MongodStarter.getDefaultInstance();

   private MongodExecutable mongodExecutable;
   private MongodProcess mongodProcess;

   public EmbeddedMongoDb() {
      if (!"localhost".equals(HOST)) {
         // do not start embedded MongoDB when remote database is used
         return;
      }

      IMongodConfig mongodConfig = createMongoConfig();
      mongodExecutable = mongodStarter.prepare(mongodConfig);
   }

   private static IMongodConfig createMongoConfig() {
      try {
         return new MongodConfigBuilder()
               .version(Version.Main.V3_4)
               .net(new Net(HOST, PORT, Network.localhostIsIPv6()))
               .build();
      } catch (IOException ex) {
         throw new RuntimeException(ex);

      }
   }

   public void start() {
      try {
         mongodProcess = mongodExecutable.start();
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   public void stop() {
      if (mongodExecutable != null && mongodProcess != null && mongodProcess.isProcessRunning()) {
         mongodProcess.stop();
         mongodExecutable.stop();
      }
   }

}
