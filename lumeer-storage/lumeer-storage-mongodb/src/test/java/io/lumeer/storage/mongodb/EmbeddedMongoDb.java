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

   public static final String HOST = System.getProperty("lumeer.db.host", "127.0.0.1");
   public static final String NAME = System.getProperty("lumeer.db.name", "lumeer-test");
   public static final int PORT = Integer.getInteger("lumeer.db.port", 27017);
   public static final String USER = System.getProperty("lumeer.db.user", "");
   public static final String PASSWORD = System.getProperty("lumeer.db.passwd", "");
   public static final Boolean SSL = Boolean.getBoolean("lumeer.db.ssl");

   public static final Boolean SKIP = Boolean.getBoolean("lumeer.db.embed.skip");

   private static MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
   private static IMongodConfig mongodConfig = createMongoConfig();

   private MongodExecutable mongodExecutable;
   private MongodProcess mongodProcess;

   public EmbeddedMongoDb() {
      if (!"localhost".equals(HOST) && !"127.0.0.1".equals(HOST)) {
         // do not start embedded MongoDB when remote database is used
         return;
      }

      mongodExecutable = mongodStarter.prepare(mongodConfig);
   }

   private static IMongodConfig createMongoConfig() {
      try {
         final MongodConfigBuilder builder = new MongodConfigBuilder();
         builder.version(Version.Main.V4_0).net(new Net(HOST, PORT, Network.localhostIsIPv6()));

         if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            builder.withLaunchArgument("--storageEngine", "mmapv1");
         }

         return builder.build();
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
