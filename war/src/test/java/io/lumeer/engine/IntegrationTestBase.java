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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;

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
                             "de.flapdoodle", "com.univocity", "cz.gopay", "org.codehaus.jackson", "org.graalvm", "javax.ws.rs",
                             "com.auth0", "okhttp3", "okio", "org.marvec.pusher", "io.sentry", "org.json.simple", "org.apache.commons.text",
                             "org.apache.commons.io", "com.floreysoft.jmte", "kotlin", "com.google.gson", "org.apache.commons.collections4")
                       .addAsWebInfResource("WEB-INF/beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties")
                       .addAsResource("email-templates/past_due_date.en.html")
                       .addAsResource("email-templates/due_date_soon.en.html")
                       .addAsResource("email-templates/state_update.en.html")
                       .addAsResource("email-templates/task_assigned.en.html")
                       .addAsResource("email-templates/task_unassigned.en.html")
                       .addAsResource("email-templates/invitation.en.html")
                       .addAsResource("email-templates/past_due_date.cs.html")
                       .addAsResource("email-templates/due_date_soon.cs.html")
                       .addAsResource("email-templates/state_update.cs.html")
                       .addAsResource("email-templates/task_assigned.cs.html")
                       .addAsResource("email-templates/task_unassigned.cs.html")
                       .addAsResource("email-templates/invitation.cs.html")
                       .addAsResource("email-templates/subject.properties")
                       .addAsResource("META-INF/javamail.default.address.map")
                       .addAsResource("templates/okr.en.json")
                       .addAsResource("templates/hr.en.json")
                       .addAsResource("moment-with-locales.min.js")
                       .addAsResource("moment-business.min.js")
                       .addAsResource("he.min.js")
                       .addAsResource("numbro.min.js")
                       .addAsResource("numbro-languages.min.js")
                       .addAsResource("lumeer-data-filters.min.js")
                       .addAsResource("test.json")
                       .addAsLibraries(Maven.resolver()
                                            .loadPomFromFile("pom.xml")
                                            .resolve("org.assertj:assertj-core", "de.flapdoodle.embed:de.flapdoodle.embed.mongo",
                                                  "javax.xml.bind:jaxb-api", "org.conscrypt:conscrypt-openjdk-uber",
                                                  "org.mockito:mockito-core", "com.univocity:univocity-parsers",
                                                  "org.apache.logging.log4j:log4j-core", "jakarta.json.bind:jakarta.json.bind-api",
                                                  "com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider",
                                                  "com.fasterxml.jackson.module:jackson-module-jaxb-annotations",
                                                  "com.fasterxml.jackson.core:jackson-annotations",
                                                  "com.fasterxml.jackson.datatype:jackson-datatype-jsr310",
                                                  "software.amazon.awssdk:s3",
                                                  "org.graalvm.polyglot:polyglot",
                                                  "org.graalvm.js:js-language")
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

   @BeforeEach
   public void cleanDatabase() {
      ((MongoDatabase) systemDataStorage.getDatabase()).drop();
   }

   @BeforeEach
   public void clearCaches() {
      userCache.clear();
      workspaceCache.clear();
   }
}
