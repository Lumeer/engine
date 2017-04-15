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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.rest.dao.LinkDao;
import io.lumeer.engine.rest.dao.LinkTypeDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

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
@RunWith(Arquillian.class)
public class LinkingFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Test
   public void testGetLinkTypes() throws Exception {
      final String collectionGetLinkTypesI = "collectionGetLinkTypesI";
      final String collectionGetLinkTypesII = "collectionGetLinkTypesII";
      final String collectionGetLinkTypesIII = "collectionGetLinkTypesIII";
      List<String> collections = Arrays.asList(collectionGetLinkTypesI, collectionGetLinkTypesII, collectionGetLinkTypesIII);
      Map<String, List<String>> ids = createTestData(collections, 2);

      String col1Id1 = ids.get(collectionGetLinkTypesI).get(0);
      String col1Id2 = ids.get(collectionGetLinkTypesI).get(1);
      String col2Id1 = ids.get(collectionGetLinkTypesII).get(0);
      String col2Id2 = ids.get(collectionGetLinkTypesII).get(1);
      String col3Id1 = ids.get(collectionGetLinkTypesIII).get(0);
      String col3Id2 = ids.get(collectionGetLinkTypesIII).get(1);

      String role1 = "role1";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocLink(collectionGetLinkTypesI, col1Id1, collectionGetLinkTypesII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionGetLinkTypesI, col1Id2, collectionGetLinkTypesII, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionGetLinkTypesI, col1Id1, collectionGetLinkTypesIII, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionGetLinkTypesI, col1Id2, collectionGetLinkTypesIII, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<LinkTypeDao> linkTypes = linkingFacade.readLinkTypes(collectionGetLinkTypesI, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(2);

      linkTypes = linkingFacade.readLinkTypes(collectionGetLinkTypesII, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(1);

      linkTypes = linkingFacade.readLinkTypes(collectionGetLinkTypesIII, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkTypes).hasSize(1);
   }

   @Test
   public void testGetLinks() throws Exception {
      final String collectionGetLinksI = "collectionGetLinksI";
      final String collectionGetLinksII = "collectionGetLinksII";
      final String collectionGetLinksIII = "collectionGetLinksIII";
      List<String> collections = Arrays.asList(collectionGetLinksI, collectionGetLinksII, collectionGetLinksIII);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(collectionGetLinksI).get(0);
      String col1Id2 = ids.get(collectionGetLinksI).get(1);
      String col2Id1 = ids.get(collectionGetLinksII).get(0);
      String col2Id2 = ids.get(collectionGetLinksII).get(1);
      String col3Id1 = ids.get(collectionGetLinksIII).get(0);
      String col3Id2 = ids.get(collectionGetLinksIII).get(1);
      String col3Id3 = ids.get(collectionGetLinksIII).get(2);

      String role1 = "role1";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocsLinks(collectionGetLinksI, col1Id1, collectionGetLinksII, Arrays.asList(col2Id1, col2Id2), Arrays.asList(new DataDocument(), new DataDocument()), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionGetLinksI, col1Id2, collectionGetLinksII, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocsLinks(collectionGetLinksI, col1Id1, collectionGetLinksIII, Arrays.asList(col3Id1, col3Id2, col3Id3), Arrays.asList(new DataDocument(), new DataDocument(), new DataDocument()), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionGetLinksI, col1Id2, collectionGetLinksIII, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<LinkDao> linkTypes = linkingFacade.readLinks(collectionGetLinksI, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(5);

      linkTypes = linkingFacade.readLinks(collectionGetLinksII, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(1);

      linkTypes = linkingFacade.readLinks(collectionGetLinksIII, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkTypes).hasSize(3);
   }

   @Test
   public void testGetDocumentsLinks() throws Exception {
      final String collectionGetDocumentsLinksI = "collectionGetDocumentsLinksI";
      final String collectionGetDocumentsLinksII = "collectionGetDocumentsLinksII";
      List<String> collections = Arrays.asList(collectionGetDocumentsLinksI, collectionGetDocumentsLinksII);
      Map<String, List<String>> ids = createTestData(collections, 2);

      String col1Id1 = ids.get(collectionGetDocumentsLinksI).get(0);
      String col1Id2 = ids.get(collectionGetDocumentsLinksI).get(1);
      String col2Id1 = ids.get(collectionGetDocumentsLinksII).get(0);
      String col2Id2 = ids.get(collectionGetDocumentsLinksII).get(1);

      String role1 = "role1";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocLink(collectionGetDocumentsLinksI, col1Id1, collectionGetDocumentsLinksII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionGetDocumentsLinksI, col1Id1, collectionGetDocumentsLinksII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(collectionGetDocumentsLinksI, col1Id2, collectionGetDocumentsLinksII, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<LinkDao> linkTypes = linkingFacade.readDocByDocLinks(collectionGetDocumentsLinksI, col1Id1, collectionGetDocumentsLinksII, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(1);

      linkTypes = linkingFacade.readDocByDocLinks(collectionGetDocumentsLinksI, col1Id1, collectionGetDocumentsLinksII, col2Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkTypes).hasSize(1);

      linkTypes = linkingFacade.readDocByDocLinks(collectionGetDocumentsLinksI, col1Id2, collectionGetDocumentsLinksII, col2Id2, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(1);

      linkTypes = linkingFacade.readDocByDocLinks(collectionGetDocumentsLinksI, col1Id2, collectionGetDocumentsLinksII, col2Id2, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkTypes).isEmpty();
   }

   @Test
   public void testReadAndDropDocByDoc() throws Exception {
      final String collectionReadDropDocByDocI = "collectionReadDropDocByDocI";
      final String collectionReadDropDocByDocII = "collectionReadDropDocByDocII";
      final String collectionReadDropDocByDocIII = "collectionReadDropDocByDocIII";
      List<String> collections = Arrays.asList(collectionReadDropDocByDocI, collectionReadDropDocByDocII, collectionReadDropDocByDocIII);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(collectionReadDropDocByDocI).get(0);
      String col2Id1 = ids.get(collectionReadDropDocByDocII).get(0);
      String col3Id1 = ids.get(collectionReadDropDocByDocIII).get(2);
      String col3Id2 = ids.get(collectionReadDropDocByDocIII).get(1);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);

      // read link for specific role
      List<DataDocument> links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);
      linkingFacade.dropDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      // read link for all roles
      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);
      linkingFacade.dropDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      // read link for all roles, but other way
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(2);
      linkingFacade.dropDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocII, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();

      // read link for all roles
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(3);
      linkingFacade.dropDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      linkingFacade.createDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);
      linkingFacade.dropDocWithDocLink(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();

      links = linkingFacade.readDocByDocLinksDocs(collectionReadDropDocByDocI, col1Id1, collectionReadDropDocByDocIII, col3Id2, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

   }

   @Test
   public void testCreateDropCollections() throws Exception {
      final String collectionCreateDropCollectionsI = "collectionCreateDropCollectionsI";
      final String collectionCreateDropCollectionsII = "collectionCreateDropCollectionsII";
      final String collectionCreateDropCollectionsIII = "collectionCreateDropCollectionsIII";
      List<String> collections = Arrays.asList(collectionCreateDropCollectionsI, collectionCreateDropCollectionsII, collectionCreateDropCollectionsIII);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(collectionCreateDropCollectionsI).get(0);
      String col2Id1 = ids.get(collectionCreateDropCollectionsII).get(0);
      String col3Id1 = ids.get(collectionCreateDropCollectionsIII).get(0);
      String col3Id2 = ids.get(collectionCreateDropCollectionsIII).get(1);
      String col3Id3 = ids.get(collectionCreateDropCollectionsIII).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> links = linkingFacade.readDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);

      links = linkingFacade.readDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);

      linkingFacade.dropDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      links = linkingFacade.readDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(5);

      linkingFacade.dropDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);

      linkingFacade.dropDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(collectionCreateDropCollectionsI, col1Id1, collectionCreateDropCollectionsIII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();
   }

   @Test
   public void testDropWholeCollection() throws Exception {
      final String collectionDropWholeCollectionI = "collectionDropWholeCollectionI";
      final String collectionDropWholeCollectionII = "collectionDropWholeCollectionII";
      final String collectionDropWholeCollectionIII = "collectionDropWholeCollectionIII";
      List<String> collections = Arrays.asList(collectionDropWholeCollectionI, collectionDropWholeCollectionII, collectionDropWholeCollectionIII);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(collectionDropWholeCollectionI).get(0);
      String col2Id1 = ids.get(collectionDropWholeCollectionII).get(0);
      String col3Id1 = ids.get(collectionDropWholeCollectionIII).get(0);
      String col3Id2 = ids.get(collectionDropWholeCollectionIII).get(1);
      String col3Id3 = ids.get(collectionDropWholeCollectionIII).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocLink(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionII, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionIII, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionIII, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createDocWithDocLink(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionIII, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> links = linkingFacade.readDocWithCollectionLinks(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);

      links = linkingFacade.readDocWithCollectionLinks(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionIII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(3);

      linkingFacade.dropCollectionLinks(collectionDropWholeCollectionI, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readDocWithCollectionLinks(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      links = linkingFacade.readDocWithCollectionLinks(collectionDropWholeCollectionI, col1Id1, collectionDropWholeCollectionIII, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();
   }

   @Test
   public void testCreateDropAll() throws Exception {
      final String collectionCreateDropAllI = "collectionCreateDropAllI";
      final String collectionCreateDropAllII = "collectionCreateDropAllII";
      final String collectionCreateDropAllIII = "collectionCreateDropAllIII";
      List<String> collections = Arrays.asList(collectionCreateDropAllI, collectionCreateDropAllII, collectionCreateDropAllIII);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(collectionCreateDropAllI).get(0);
      String col2Id1 = ids.get(collectionCreateDropAllII).get(0);
      String col3Id1 = ids.get(collectionCreateDropAllIII).get(0);
      String col3Id2 = ids.get(collectionCreateDropAllIII).get(1);
      String col3Id3 = ids.get(collectionCreateDropAllIII).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllII, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllII, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<DataDocument> links = linkingFacade.readDocumentLinksDocs(collectionCreateDropAllI, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(4);
      links = linkingFacade.readDocumentLinksDocs(collectionCreateDropAllI, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(8);

      linkingFacade.dropAllDocumentLinks(collectionCreateDropAllI, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocumentLinksDocs(collectionCreateDropAllI, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();

      links = linkingFacade.readDocumentLinksDocs(collectionCreateDropAllI, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(4);

      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createDocWithDocLink(collectionCreateDropAllI, col1Id1, collectionCreateDropAllIII, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocumentLinksDocs(collectionCreateDropAllI, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(7);

      linkingFacade.dropAllDocumentLinks(collectionCreateDropAllI, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readDocumentLinksDocs(collectionCreateDropAllI, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();
   }

   @Test
   public void testProjectSwitching() throws DbException {
      final String collectionProjectSwitchingI = "collectionProjectSwitchingI";
      final String collectionProjectSwitchingII = "collectionProjectSwitchingII";
      List<String> collections = Arrays.asList(collectionProjectSwitchingI, collectionProjectSwitchingII);
      Map<String, List<String>> ids = createTestData(collections, 2);

      String col1Id1 = ids.get(collectionProjectSwitchingI).get(0);
      String col2Id1 = ids.get(collectionProjectSwitchingII).get(0);
      String col2Id2 = ids.get(collectionProjectSwitchingII).get(1);

      String role1 = "role1";
      String role2 = "role2";

      String project1 = "project1";
      String project2 = "project2";
      projectFacade.setCurrentProjectId(project1);
      linkingFacade.createDocWithDocLink(collectionProjectSwitchingI, col1Id1, collectionProjectSwitchingII, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      projectFacade.setCurrentProjectId(project2);
      linkingFacade.createDocWithDocsLinks(collectionProjectSwitchingI, col1Id1, collectionProjectSwitchingII, Arrays.asList(col2Id1, col2Id2), Arrays.asList(new DataDocument(), new DataDocument()), role2, LumeerConst.Linking.LinkDirection.FROM);

      projectFacade.setCurrentProjectId(project1);
      List<DataDocument> links = linkingFacade.readDocWithCollectionLinks(collectionProjectSwitchingI, col1Id1, collectionProjectSwitchingII, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);
      links = linkingFacade.readDocWithCollectionLinks(collectionProjectSwitchingI, col1Id1, collectionProjectSwitchingII, role2, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      projectFacade.setCurrentProjectId(project2);
      links = linkingFacade.readDocWithCollectionLinks(collectionProjectSwitchingI, col1Id1, collectionProjectSwitchingII, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();
      links = linkingFacade.readDocWithCollectionLinks(collectionProjectSwitchingI, col1Id1, collectionProjectSwitchingII, role2, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);
   }

   private Map<String, List<String>> createTestData(List<String> collections, int numDocuments) {
      Map<String, List<String>> ids = new HashMap<>();
      for (String col : collections) {
         dataStorage.dropCollection(col);
         dataStorage.createCollection(col);
         List<String> collIds = new ArrayList<>();
         for (int i = 0; i < numDocuments; i++) {
            String id = dataStorage.createDocument(col, new DataDocument());
            collIds.add(id);
         }
         ids.put(col, collIds);
      }
      return ids;
   }

   private String buildProjectLinkingCollectionName() {
      return LumeerConst.Linking.PREFIX + "_" + projectFacade.getCurrentProjectId();
   }

}
