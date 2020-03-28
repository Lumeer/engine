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
package io.lumeer.engine;

import io.lumeer.core.cache.UserCache;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.mongodb.EmbeddedMongoDb;
import io.lumeer.test.arquillian.annotation.AfterUnDeploy;
import io.lumeer.test.arquillian.annotation.BeforeDeploy;

import com.mongodb.client.MongoDatabase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;

import javax.inject.Inject;

public abstract class IntegrationTestBase {

   protected static final String PATH_CONTEXT = "lumeer-test";
   private static final String ARCHIVE_NAME = PATH_CONTEXT + ".war";

   private static EmbeddedMongoDb embeddedMongoDb;

   @Inject
   @SystemDataStorage
   public DataStorage systemDataStorage;

   @Inject
   public UserCache userCache;

   @Inject
   public WorkspaceCache workspaceCache;

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME)
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "org.mongodb",
                             "de.flapdoodle", "com.univocity", "cz.gopay", "com.fasterxml", "org.codehaus.jackson", "org.graalvm",
                             "com.auth0", "okhttp3", "okio", "org.marvec.pusher", "io.sentry", "org.json.simple", "org.apache.commons.text")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties")
                       .addAsResource("templates/okr.en.json")
                       .addAsResource("templates/hr.en.json")
                       .addAsResource("moment-with-locales.min.js")
                       .addAsLibraries(Maven.resolver()
                                            .loadPomFromFile("pom.xml")
                                            .resolve("org.assertj:assertj-core", "de.flapdoodle.embed:de.flapdoodle.embed.mongo",
                                                  "org.mockito:mockito-core", "com.univocity:univocity-parsers",
                                                  "org.apache.logging.log4j:log4j-core",
                                                  "software.amazon.awssdk:s3",
                                                  "org.graalvm.sdk:graal-sdk",
                                                  "org.graalvm.sdk:polyglot-tck",
                                                  "org.graalvm.truffle:truffle-api",
                                                  "org.graalvm.js:js")
                                            .withTransitivity()
                                            .asFile()
                       );
   }

   @RunAsClient
   @BeforeDeploy
   public static void startEmbeddedMongoDb() {
      if (!EmbeddedMongoDb.SKIP) {
         embeddedMongoDb = new EmbeddedMongoDb();
         embeddedMongoDb.start();
      }
   }

   @RunAsClient
   @AfterUnDeploy
   public static void stopEmbeddedMongoDb() {
      if (embeddedMongoDb != null) {
         embeddedMongoDb.stop();
      }
   }

   @Before
   public void cleanDatabase() {
      ((MongoDatabase) systemDataStorage.getDatabase()).drop();
   }

   @Before
   public void clearCaches() {
      userCache.clear();
      workspaceCache.clear();
   }
}
