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

import io.lumeer.engine.api.batch.SplitBatch;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.Query;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
   private final String COLLECTION_NAME = "Supeř Kolekce +ěščřžý";
//   private final String PATH_PREFIX = "lumeer-engine/rest/";

   @Test
   public void testSplitBatch() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      // the path prefix '/lumeer-engine/' does not work in test classes
      Response response = client.target(TARGET_URI)
            .path(PATH_PREFIX + "collections/" + COLLECTION_NAME)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .buildDelete()
            .invoke();

      int status = response.getStatus();
      response.close();
      Assert.assertTrue(status == 404 || status == 204);

      response = client.target(TARGET_URI)
                       .path(PATH_PREFIX + "collections/" + COLLECTION_NAME)
                       .request(MediaType.APPLICATION_JSON_TYPE)
                       .buildPost(Entity.text(""))
                       .invoke();
      status = response.getStatus();
      response.close();
      Assert.assertEquals(status, 200);

      response = client.target(TARGET_URI)
            .path(PATH_PREFIX + "collections/" + COLLECTION_NAME + "/documents/")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .buildPost(Entity.json(new DataDocument("attr1", 5).append("attr2", "hello, how are, you 1")))
            .invoke();

      final String documentId = response.readEntity(String.class);
      response.close();

      Assert.assertTrue(documentId != null && documentId.length() > 5);

      response = client.target(TARGET_URI)
            .path(PATH_PREFIX + "collections/" + COLLECTION_NAME + "/documents/")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .buildPost(Entity.json(new DataDocument("attr1", 5).append("attr2", "hello, how are, you 2")))
            .invoke();
      response.close();
      response = client.target(TARGET_URI)
            .path(PATH_PREFIX + "collections/" + COLLECTION_NAME + "/documents/")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .buildPost(Entity.json(new DataDocument("attr1", 5).append("attr2", "hello, how are, you 3")))
            .invoke();
      response.close();

      response = client.target(TARGET_URI)
            .path(PATH_PREFIX + "batch/split/")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .buildPost(Entity.json(new SplitBatch(COLLECTION_NAME, "attr2", ",", true, Arrays.asList("attr2a", "attr2b", "attr2c", "attr2d"), false)))
            .invoke();
      response.close();

      response = client.target(TARGET_URI)
            .path(PATH_PREFIX + "query/")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .buildPost(Entity.json(new Query(new DataDocument("attr1", "5"))))
            .invoke();
      final List<LinkedHashMap> result = response.readEntity(List.class);
      response.close();

      result.forEach(System.out::println);


      //Thread.sleep(5 * 60 * 1000L);
   }

}
