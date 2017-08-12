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

import io.lumeer.core.WorkspaceCache;
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
   public WorkspaceCache workspaceCache;

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME)
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "org.mongodb", "io.netty", "de.flapdoodle")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties")
                       .addAsLibraries(Maven.resolver()
                                            .loadPomFromFile("pom.xml")
                                            .resolve("org.assertj:assertj-core", "de.flapdoodle.embed:de.flapdoodle.embed.mongo", "org.mongodb.morphia:morphia", "org.mockito:mockito-core")
                                            .withTransitivity()
                                            .asFile()
                       );
   }

   @RunAsClient
   @BeforeDeploy
   public static void startEmbeddedMongoDb() {
      embeddedMongoDb = new EmbeddedMongoDb();
      embeddedMongoDb.start();
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
      workspaceCache.clear();
   }
}
