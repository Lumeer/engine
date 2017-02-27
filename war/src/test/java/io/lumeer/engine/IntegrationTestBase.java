/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public abstract class IntegrationTestBase {

   protected static final String PATH_CONTEXT = "lumeer-test";
   private static final String ARCHIVE_NAME = PATH_CONTEXT + ".war";

   private static final String DB_HOST = System.getProperty("lumeer.db.host", "localhost");
   private static final int DB_PORT = Integer.getInteger("lumeer.db.port", 27017);

   private static MongodExecutable mongodExecutable;

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME)
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty", "de.flapdoodle")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties")
                       .addAsLibraries(Maven.resolver()
                                            .loadPomFromFile("pom.xml")
                                            .resolve("org.assertj:assertj-core", "de.flapdoodle.embed:de.flapdoodle.embed.mongo")
                                            .withTransitivity()
                                            .asFile()
                       );
   }

   @BeforeClass
   @RunAsClient
   public static void startEmbeddedMongoDb() throws Exception {
      if (!"localhost".equals(DB_HOST)) {
         // do not start embedded MongoDB when remote database is used
         return;
      }

      MongodStarter starter = MongodStarter.getDefaultInstance();

      IMongodConfig mongodConfig = new MongodConfigBuilder()
            .version(Version.Main.V3_4)
            .net(new Net(DB_HOST, DB_PORT, Network.localhostIsIPv6()))
            .build();

      mongodExecutable = starter.prepare(mongodConfig);
      mongodExecutable.start();
   }

   @AfterClass
   @RunAsClient
   public static void stopEmbeddedMongoDb() {
      if (mongodExecutable != null) {
         mongodExecutable.stop();
      }
   }
}
