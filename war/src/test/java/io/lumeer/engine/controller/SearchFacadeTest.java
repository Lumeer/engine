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

import org.bson.Document;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
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
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   private final String DUMMY_COLLECTION1 = "collection.testcollection1";
   private final String DUMMY_COLLECTION1_ORIGINAL_NAME = "testCollection1";

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Test
   public void testSearch() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(DUMMY_COLLECTION1, insertedDocument);
      }

      List<DataDocument> searchDocuments = searchFacade.search(DUMMY_COLLECTION1, null, null, 100, 100);

      Assert.assertEquals(searchDocuments.size(), 100);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testRawSearch() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(DUMMY_COLLECTION1, insertedDocument);
      }

      String query = "{find: \"" + DUMMY_COLLECTION1 + "\"}";

      List<DataDocument> searchDocuments = searchFacade.search(query);

      // search() method returns 101 entries due to it is a default value of "batchSize" query key
      Assert.assertEquals(searchDocuments.size(), 101);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

}
