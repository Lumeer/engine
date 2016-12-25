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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.rest.dao.LinkDao;
import io.lumeer.engine.rest.dao.LinkTypeDao;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String COLLECTION_GETLINKS_I = "collectionGetLinksI";
   private final String COLLECTION_GETLINKS_II = "collectionGetLinksII";
   private final String COLLECTION_GETLINKS_III = "collectionGetLinksIII";
   private final String COLLECTION_GETLINKTYPES_I = "collectionGetLinkTypesI";
   private final String COLLECTION_GETLINKTYPES_II = "collectionGetLinkTypesII";
   private final String COLLECTION_GETLINKTYPES_III = "collectionGetLinkTypesIII";
   private final String COLLECTION_GETDOCUMENTSLINKS_I = "collectionGetDocumentsLinksI";
   private final String COLLECTION_GETDOCUMENTSLINKS_II = "collectionGetDocumentsLinksII";
   private final String COLLECTION_READ_DROP_DOC_BY_DOC_I = "collectionReadDropDocByDocI";
   private final String COLLECTION_READ_DROP_DOC_BY_DOC_II = "collectionReadDropDocByDocII";
   private final String COLLECTION_READ_DROP_DOC_BY_DOC_III = "collectionReadDropDocByDocIII";
   private final String COLLECTION_READ_DROP_COLL_I = "collectionCreateDropCollectionsI";
   private final String COLLECTION_READ_DROP_COLL_II = "collectionCreateDropCollectionsII";
   private final String COLLECTION_READ_DROP_COLL_III = "collectionCreateDropCollectionsIII";
   private final String COLLECTION_READ_DROP_ALL_I = "collectionCreateDropAllI";
   private final String COLLECTION_READ_DROP_ALL_II = "collectionCreateDropAllII";
   private final String COLLECTION_READ_DROP_ALL_III = "collectionCreateDropAllIII";

   private final int NUM_DOCUMENTS = 3;

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testGetLinkTypes() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_GETLINKTYPES_I, COLLECTION_GETLINKTYPES_II, COLLECTION_GETLINKTYPES_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_GETLINKTYPES_I).get(0);
      String col1Id2 = ids.get(COLLECTION_GETLINKTYPES_I).get(1);
      String col2Id1 = ids.get(COLLECTION_GETLINKTYPES_II).get(0);
      String col2Id2 = ids.get(COLLECTION_GETLINKTYPES_II).get(1);
      String col3Id1 = ids.get(COLLECTION_GETLINKTYPES_III).get(0);
      String col3Id2 = ids.get(COLLECTION_GETLINKTYPES_III).get(1);

      String role1 = "role1";

      dropLinkingCollections(Collections.singletonList(role1), collections);

      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKTYPES_I, col1Id1, COLLECTION_GETLINKTYPES_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKTYPES_I, col1Id2, COLLECTION_GETLINKTYPES_II, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKTYPES_I, col1Id1, COLLECTION_GETLINKTYPES_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKTYPES_I, col1Id2, COLLECTION_GETLINKTYPES_III, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<LinkTypeDao> linkTypes = linkingFacade.readLinkTypes(COLLECTION_GETLINKTYPES_I, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(linkTypes.size(), 2);

   }

   @Test
   public void testGetLinks() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_GETLINKS_I, COLLECTION_GETLINKS_II, COLLECTION_GETLINKS_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_GETLINKS_I).get(0);
      String col1Id2 = ids.get(COLLECTION_GETLINKS_I).get(1);
      String col2Id1 = ids.get(COLLECTION_GETLINKS_II).get(0);
      String col2Id2 = ids.get(COLLECTION_GETLINKS_II).get(1);
      String col3Id1 = ids.get(COLLECTION_GETLINKS_III).get(0);
      String col3Id2 = ids.get(COLLECTION_GETLINKS_III).get(1);

      String role1 = "role1";

      dropLinkingCollections(Collections.singletonList(role1), collections);

      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKS_I, col1Id1, COLLECTION_GETLINKS_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKS_I, col1Id2, COLLECTION_GETLINKS_II, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKS_I, col1Id1, COLLECTION_GETLINKS_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_GETLINKS_I, col1Id2, COLLECTION_GETLINKS_III, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<LinkDao> linkTypes = linkingFacade.readLinks(COLLECTION_GETLINKS_I, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(linkTypes.size(), 2);

   }

   @Test
   public void testGetDocumentsLinks() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_GETDOCUMENTSLINKS_I, COLLECTION_GETDOCUMENTSLINKS_II);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_GETDOCUMENTSLINKS_I).get(0);
      String col1Id2 = ids.get(COLLECTION_GETDOCUMENTSLINKS_I).get(1);
      String col2Id1 = ids.get(COLLECTION_GETDOCUMENTSLINKS_II).get(0);
      String col2Id2 = ids.get(COLLECTION_GETDOCUMENTSLINKS_II).get(1);

      String role1 = "role1";

      dropLinkingCollections(Collections.singletonList(role1), collections);

      linkingFacade.createDocWithDocLink(COLLECTION_GETDOCUMENTSLINKS_I, col1Id1, COLLECTION_GETDOCUMENTSLINKS_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_GETDOCUMENTSLINKS_I, col1Id1, COLLECTION_GETDOCUMENTSLINKS_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(COLLECTION_GETDOCUMENTSLINKS_I, col1Id2, COLLECTION_GETDOCUMENTSLINKS_II, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<LinkDao> linkTypes = linkingFacade.readDocByDocLinks(COLLECTION_GETDOCUMENTSLINKS_I, col1Id1, COLLECTION_GETDOCUMENTSLINKS_II, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(linkTypes.size(), 1);

      linkTypes = linkingFacade.readDocByDocLinks(COLLECTION_GETDOCUMENTSLINKS_I, col1Id1, COLLECTION_GETDOCUMENTSLINKS_II, col2Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(linkTypes.size(), 1);

      linkTypes = linkingFacade.readDocByDocLinks(COLLECTION_GETDOCUMENTSLINKS_I, col1Id2, COLLECTION_GETDOCUMENTSLINKS_II, col2Id2, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(linkTypes.size(), 1);

      linkTypes = linkingFacade.readDocByDocLinks(COLLECTION_GETDOCUMENTSLINKS_I, col1Id2, COLLECTION_GETDOCUMENTSLINKS_II, col2Id2, role1, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(linkTypes.size(), 0);
   }

   @Test
   public void testReadAndDropDocByDoc() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_READ_DROP_DOC_BY_DOC_I, COLLECTION_READ_DROP_DOC_BY_DOC_II, COLLECTION_READ_DROP_DOC_BY_DOC_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_READ_DROP_DOC_BY_DOC_I).get(0);
      String col2Id1 = ids.get(COLLECTION_READ_DROP_DOC_BY_DOC_II).get(0);
      String col3Id1 = ids.get(COLLECTION_READ_DROP_DOC_BY_DOC_III).get(2);
      String col3Id2 = ids.get(COLLECTION_READ_DROP_DOC_BY_DOC_III).get(1);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dropLinkingCollections(Arrays.asList(role1, role2, role3), collections);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 1);
      linkingFacade.dropDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 2);
      linkingFacade.dropDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(links.size(), 2);
      linkingFacade.dropDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_II, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertTrue(links.isEmpty());

      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 3);
      linkingFacade.dropDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 1);
      linkingFacade.dropDocWithDocLink(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertTrue(links.isEmpty());

      links = linkingFacade.readDocByDocLinksDocs(COLLECTION_READ_DROP_DOC_BY_DOC_I, col1Id1, COLLECTION_READ_DROP_DOC_BY_DOC_III, col3Id2, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

   }

   @Test
   public void testCreateDropCollections() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_READ_DROP_COLL_I, COLLECTION_READ_DROP_COLL_II, COLLECTION_READ_DROP_COLL_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_READ_DROP_COLL_I).get(0);
      String col2Id1 = ids.get(COLLECTION_READ_DROP_COLL_II).get(0);
      String col3Id1 = ids.get(COLLECTION_READ_DROP_COLL_III).get(0);
      String col3Id2 = ids.get(COLLECTION_READ_DROP_COLL_III).get(1);
      String col3Id3 = ids.get(COLLECTION_READ_DROP_COLL_III).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dropLinkingCollections(Arrays.asList(role1, role2, role3), collections);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, role1, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 1);

      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 2);

      linkingFacade.dropDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 5);

      linkingFacade.dropDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 2);

      linkingFacade.dropDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 2);
      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertEquals(links.size(), 3);
      linkingFacade.dropCollectionLinks(COLLECTION_READ_DROP_COLL_I, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_II, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());
      links = linkingFacade.readDocWithCollectionLinks(COLLECTION_READ_DROP_COLL_I, col1Id1, COLLECTION_READ_DROP_COLL_III, null, LumeerConst.Linking.LinkDirection.FROM);
      Assert.assertTrue(links.isEmpty());
   }

   @Test
   public void testCreateDropAll() throws Exception {
      List<String> collections = Arrays.asList(COLLECTION_READ_DROP_ALL_I, COLLECTION_READ_DROP_ALL_II, COLLECTION_READ_DROP_ALL_III);
      Map<String, List<String>> ids = createTestData(collections);

      String col1Id1 = ids.get(COLLECTION_READ_DROP_ALL_I).get(0);
      String col2Id1 = ids.get(COLLECTION_READ_DROP_ALL_II).get(0);
      String col3Id1 = ids.get(COLLECTION_READ_DROP_ALL_III).get(0);
      String col3Id2 = ids.get(COLLECTION_READ_DROP_ALL_III).get(1);
      String col3Id3 = ids.get(COLLECTION_READ_DROP_ALL_III).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dropLinkingCollections(Arrays.asList(role1, role2, role3), collections);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_II, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_II, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<DataDocument> links = linkingFacade.readDocumentLinksDocs(COLLECTION_READ_DROP_ALL_I, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(links.size(), 4);
      links = linkingFacade.readDocumentLinksDocs(COLLECTION_READ_DROP_ALL_I, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(links.size(), 8);

      linkingFacade.dropAllDocumentLinks(COLLECTION_READ_DROP_ALL_I, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocumentLinksDocs(COLLECTION_READ_DROP_ALL_I, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertTrue(links.isEmpty());

      links = linkingFacade.readDocumentLinksDocs(COLLECTION_READ_DROP_ALL_I, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(links.size(), 4);

      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_II, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(COLLECTION_READ_DROP_ALL_I, col1Id1, COLLECTION_READ_DROP_ALL_III, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocumentLinksDocs(COLLECTION_READ_DROP_ALL_I, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertEquals(links.size(), 7);

      linkingFacade.dropAllDocumentLinks(COLLECTION_READ_DROP_ALL_I, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocumentLinksDocs(COLLECTION_READ_DROP_ALL_I, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      Assert.assertTrue(links.isEmpty());
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

   private void dropLinkingCollections(final List<String> roles, final List<String> collections) {
      for (String role : roles) {
         for (String col1 : collections) {
            for (String col2 : collections) {
               if (col1.equals(col2)) {
                  continue;
               }
               dataStorage.dropCollection(buildCollectionName(col1, col2, role));
            }
         }
      }
   }

   private String buildCollectionName(final String firstCollectionName, final String secondCollectionName, final String role) {
      return LumeerConst.Linking.PREFIX + "_" + firstCollectionName + "_" + secondCollectionName + "_" + role;
   }

}
