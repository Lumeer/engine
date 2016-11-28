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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.data.DataDocument;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="kubedo8@gmail.com">Jakub Rodák</a>
 */
public class SearchFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "SearchFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-dev.properties");
   }

   private final String COLLECTION_SEARCH = "collectionSearch";
   private final String COLLECTION_SEARCH_INTERNAL = "collection.collectionSearch_0";
   private final String COLLECTION_SEARCH_RAW = "collectionSearchRaw";
   private final String COLLECTION_SEARCH_RAW_INTERNAL = "collection.collectionSearchRaw";

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @BeforeMethod
   public void setUp() throws Exception {
      collectionFacade.dropCollection(COLLECTION_SEARCH_INTERNAL);
      collectionFacade.dropCollection(COLLECTION_SEARCH_RAW_INTERNAL);
   }

   @Test
   public void testSearch() throws Exception {
      collectionFacade.createCollection(COLLECTION_SEARCH);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(COLLECTION_SEARCH_INTERNAL, insertedDocument);
      }

      List<DataDocument> searchDocuments = searchFacade.search(COLLECTION_SEARCH_INTERNAL, null, null, 100, 100);

      Assert.assertEquals(searchDocuments.size(), 100);
   }

   @Test
   public void testRawSearch() throws Exception {
      collectionFacade.createCollection(COLLECTION_SEARCH_RAW);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(COLLECTION_SEARCH_RAW_INTERNAL, insertedDocument);
      }

      String query = "{find: \"" + COLLECTION_SEARCH_RAW_INTERNAL + "\"}";

      List<DataDocument> searchDocuments = searchFacade.search(query);

      // search() method returns 101 entries due to it is a default value of "batchSize" query key
      Assert.assertEquals(searchDocuments.size(), 101);
   }

}
