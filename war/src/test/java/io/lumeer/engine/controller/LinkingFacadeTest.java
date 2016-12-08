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
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.LinkAlreadyExistsException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="kubedo8@gmail.com">Jakub Rodák</a>
 */
public class LinkingFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "LinkingFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-dev.properties");
   }

   private final String COLLECTION_SINGLE_LINKS_I = "collectionSingleLinksI";
   private final String COLLECTION_SINGLE_LINKS_II = "collectionSingleLinksII";
   private final String COLLECTION_SINGLE_LINKS_III = "collectionSingleLinksIII";
   private final String COLLECTION_COLL_LINKS_I = "collectionCollLinksI";
   private final String COLLECTION_COLL_LINKS_II = "collectionCollLinksII";
   private final String COLLECTION_COLL_LINKS_III = "collectionCollLinksIII";
   private final String COLLECTION_ALL_LINKS_I = "collectionAllLinksI";
   private final String COLLECTION_ALL_LINKS_II = "collectionAllLinksII";
   private final String COLLECTION_ALL_LINKS_III = "collectionAllLinksIII";

   private final String COLLECTION_EXCEPTION_LINKS_I = "collectionExceptionLinksI";
   private final String COLLECTION_EXCEPTION_LINKS_II = "collectionExceptionLinksII";

   private final int NUM_DOCUMENTS = 5;

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testSingleLinkCRD() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_SINGLE_LINKS_I, COLLECTION_SINGLE_LINKS_II, COLLECTION_SINGLE_LINKS_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_SINGLE_LINKS_I).get(2);
      String col2Id1 = ids.get(COLLECTION_SINGLE_LINKS_II).get(0);
      String col2Id2 = ids.get(COLLECTION_SINGLE_LINKS_II).get(1);
      String col3Id1 = ids.get(COLLECTION_SINGLE_LINKS_III).get(2);
      String col3Id2 = ids.get(COLLECTION_SINGLE_LINKS_III).get(3);

      linkingFacade.createDocWithDocLink(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id1);
      linkingFacade.createDocWithDocLink(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id2);
      linkingFacade.createDocWithDocLink(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id1);
      linkingFacade.createDocWithDocLink(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id2);
      Assert.assertTrue(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id1));
      Assert.assertTrue(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id2));
      Assert.assertTrue(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id1));
      Assert.assertTrue(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id2));
      Assert.assertFalse(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, ids.get(COLLECTION_SINGLE_LINKS_II).get(4)));
      Assert.assertFalse(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, ids.get(COLLECTION_SINGLE_LINKS_III).get(0)));

      linkingFacade.dropDocWithDocLink(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id1);
      linkingFacade.dropDocWithDocLink(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id1);
      Assert.assertFalse(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id1));
      Assert.assertTrue(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_II, col2Id2));
      Assert.assertFalse(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id1));
      Assert.assertTrue(linkingFacade.linkExistsBetweenDocuments(COLLECTION_SINGLE_LINKS_I, col1Id1, COLLECTION_SINGLE_LINKS_III, col3Id2));
   }

   @Test
   public void testCollectionLinkCRD() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_COLL_LINKS_I, COLLECTION_COLL_LINKS_II, COLLECTION_COLL_LINKS_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_COLL_LINKS_I).get(2);
      String col2Id1 = ids.get(COLLECTION_COLL_LINKS_II).get(0);
      String col2Id2 = ids.get(COLLECTION_COLL_LINKS_II).get(1);
      String col3Id1 = ids.get(COLLECTION_COLL_LINKS_III).get(2);
      String col3Id2 = ids.get(COLLECTION_COLL_LINKS_III).get(3);
      String col3Id3 = ids.get(COLLECTION_COLL_LINKS_III).get(4);

      linkingFacade.createDocWithColletionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_II, Arrays.asList(col2Id1, col2Id2));
      linkingFacade.createDocWithColletionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_III, Arrays.asList(col3Id1, col3Id2, col3Id3));
      Assert.assertEquals(2, linkingFacade.readDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_II).size());
      Assert.assertEquals(3, linkingFacade.readDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_III).size());

      linkingFacade.dropDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_II);
      Assert.assertTrue(linkingFacade.readDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_II).isEmpty());
      Assert.assertFalse(linkingFacade.readDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_III).isEmpty());
      linkingFacade.dropDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_III);
      Assert.assertTrue(linkingFacade.readDocWithCollectionLinks(COLLECTION_COLL_LINKS_I, col1Id1, COLLECTION_COLL_LINKS_III).isEmpty());
   }

   @Test
   public void testAllLinkCRD() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_ALL_LINKS_I, COLLECTION_ALL_LINKS_II, COLLECTION_ALL_LINKS_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_ALL_LINKS_I).get(2);
      String col2Id1 = ids.get(COLLECTION_ALL_LINKS_II).get(0);
      String col2Id2 = ids.get(COLLECTION_ALL_LINKS_II).get(1);
      String col3Id1 = ids.get(COLLECTION_ALL_LINKS_III).get(2);
      String col3Id2 = ids.get(COLLECTION_ALL_LINKS_III).get(3);
      String col3Id3 = ids.get(COLLECTION_ALL_LINKS_III).get(4);

      linkingFacade.createDocWithColletionLinks(COLLECTION_ALL_LINKS_I, col1Id1, COLLECTION_ALL_LINKS_II, Arrays.asList(col2Id1, col2Id2));
      linkingFacade.createDocWithColletionLinks(COLLECTION_ALL_LINKS_I, col1Id1, COLLECTION_ALL_LINKS_III, Arrays.asList(col3Id1, col3Id2, col3Id3));
      Map<String, List<DataDocument>> links = linkingFacade.readAllDocumentLinks(COLLECTION_ALL_LINKS_I, col1Id1);
      Assert.assertFalse(links.isEmpty());
      Assert.assertEquals(2, links.get(COLLECTION_ALL_LINKS_II).size());
      Assert.assertEquals(3, links.get(COLLECTION_ALL_LINKS_III).size());

      linkingFacade.dropAllDocumentLinks(COLLECTION_ALL_LINKS_I, col1Id1);
      links = linkingFacade.readAllDocumentLinks(COLLECTION_ALL_LINKS_I, col1Id1);
      Assert.assertTrue(links.isEmpty());
   }

   @Test(expectedExceptions = LinkAlreadyExistsException.class)
   public void testLinkException() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_EXCEPTION_LINKS_I, COLLECTION_EXCEPTION_LINKS_II);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_EXCEPTION_LINKS_I).get(2);
      String col2Id1 = ids.get(COLLECTION_EXCEPTION_LINKS_II).get(0);
      linkingFacade.createDocWithDocLink(COLLECTION_EXCEPTION_LINKS_I, col1Id1, COLLECTION_EXCEPTION_LINKS_II, col2Id1);
      linkingFacade.createDocWithDocLink(COLLECTION_EXCEPTION_LINKS_I, col1Id1, COLLECTION_EXCEPTION_LINKS_II, col2Id1);
   }

   private Map<String, List<String>> createTestData(List<String> collections) {
      Map<String, List<String>> ids = new HashMap<>();
      for (String col : collections) {
         dataStorage.dropCollection(col);
         dataStorage.createCollection(col);
         List<String> collIds = new ArrayList<>();
         for (int i = 0; i < NUM_DOCUMENTS; i++) {
            String id = dataStorage.createDocument(col, new DataDocument());
            collIds.add(id);
         }
         ids.put(col, collIds);
      }
      return ids;
   }

}
