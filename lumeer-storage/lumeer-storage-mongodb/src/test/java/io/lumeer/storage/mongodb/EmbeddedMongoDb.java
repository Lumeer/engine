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
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Start;

public class EmbeddedMongoDb {

   public static final String HOST = System.getProperty("lumeer.db.host", "127.0.0.1");
   public static final String NAME = System.getProperty("lumeer.db.name", "lumeer-test");
   public static final int PORT = Integer.getInteger("lumeer.db.port", 27017);
   public static final String USER = System.getProperty("lumeer.db.user", "");
   public static final String PASSWORD = System.getProperty("lumeer.db.passwd", "");
   public static final Boolean SSL = Boolean.getBoolean("lumeer.db.ssl");

   public static final Boolean SKIP = Boolean.getBoolean("lumeer.db.embed.skip");

   private static ImmutableMongod mongod = createMongod();

   private TransitionWalker.ReachedState<RunningMongodProcess> running = null;

   public EmbeddedMongoDb() {
      if (!"localhost".equals(HOST) && !"127.0.0.1".equals(HOST)) {
         // do not start embedded MongoDB when remote database is used
         return;
      }
   }

   private static ImmutableMongod createMongod() {
      var builder = Mongod.builder();

      boolean isIpv6 = false;
      try {
         isIpv6 = de.flapdoodle.net.Net.localhostIsIPv6();
      } catch (UnknownHostException e) {
      }

      builder.net(Start.to(Net.class).initializedWith(Net.of(HOST, PORT, isIpv6)));

      if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
         builder.mongodArguments(Start.to(MongodArguments.class).initializedWith(MongodArguments.builder().putArgs("--storageEngine", "mmapv1").build()));
      }

      return builder.build();
   }

   public void start() {
      running = mongod.start(Version.V4_2_23);
   }

   public void stop() {
      if (running != null && running.current() != null) {
         running.current().stop();
      }
   }

}
