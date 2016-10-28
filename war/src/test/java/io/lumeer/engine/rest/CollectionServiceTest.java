/*
 * -----------------------------------------------------------------------\
 * Lummer
 *  
 * Copyright (C) 2016 the original author or authors.
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
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests the collection service while deployed on the application server.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CollectionServiceTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "test.war")
            .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   @Inject
   private CollectionService collectionService;

   @Test
   public void testRegister() throws Exception {
      Assert.assertNotNull(collectionService);
   }

   public void testRestClient() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target("http://localhost:8080").path("/lumeer-engine/rest/collections/").request(MediaType.APPLICATION_JSON_TYPE).buildGet().invoke();
   }
}
