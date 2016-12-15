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
package io.lumeer.engine.rest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class BatchServiceTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "BatchServiceTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String TARGET_URI = "http://localhost:8080/";
   private final String PATH_PREFIX = "BatchServiceTest/rest/";
//   private final String PATH_PREFIX = "lumeer-engine/rest/";

   @Test
   public void testSplitBatch() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      // the path prefix '/lumeer-engine/' does not work in test classes
      /*Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + "collections/Super")
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .buildPost(Entity.text(""))
                                .invoke();
      System.out.println(response.getStatus());
      System.out.println(response.getStatusInfo());*/

      //Thread.sleep(5 * 60 * 1000L);
   }

}
