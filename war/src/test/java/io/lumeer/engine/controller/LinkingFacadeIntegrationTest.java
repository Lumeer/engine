/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.LinkInstance;
import io.lumeer.engine.api.dto.LinkType;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.DbException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
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
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Test
   public void testReadLinkTypesForCollection() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection1"));
      final String col2 = collectionFacade.createCollection(new Collection("collection2"));
      final String col3 = collectionFacade.createCollection(new Collection("collection3"));
      List<String> collections = Arrays.asList(col1, col2, col3);
      Map<String, List<String>> ids = createTestData(collections, 2);

      String col1Id1 = ids.get(col1).get(0);
      String col1Id2 = ids.get(col1).get(1);
      String col2Id1 = ids.get(col2).get(0);
      String col2Id2 = ids.get(col2).get(1);
      String col3Id1 = ids.get(col3).get(0);
      String col3Id2 = ids.get(col3).get(1);

      String role1 = "role1";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id2, col2, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id2, col3, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<LinkType> linkTypes = linkingFacade.readLinkTypesForCollection(col1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(2);

      linkTypes = linkingFacade.readLinkTypesForCollection(col2, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkTypes).hasSize(1);

      linkTypes = linkingFacade.readLinkTypesForCollection(col3, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkTypes).hasSize(1);
   }

   @Test
   public void testReadLinkInstancesForCollection() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection11"));
      final String col2 = collectionFacade.createCollection(new Collection("collection12"));
      final String col3 = collectionFacade.createCollection(new Collection("collection13"));
      List<String> collections = Arrays.asList(col1, col2, col3);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(col1).get(0);
      String col1Id2 = ids.get(col1).get(1);
      String col2Id1 = ids.get(col2).get(0);
      String col2Id2 = ids.get(col2).get(1);
      String col3Id1 = ids.get(col3).get(0);
      String col3Id2 = ids.get(col3).get(1);
      String col3Id3 = ids.get(col3).get(2);

      String role1 = "role1";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstancesBetweenDocumentAndCollection(col1, col1Id1, col2, Arrays.asList(col2Id1, col2Id2), Arrays.asList(new DataDocument(), new DataDocument()), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id2, col2, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstancesBetweenDocumentAndCollection(col1, col1Id1, col3, Arrays.asList(col3Id1, col3Id2, col3Id3), Arrays.asList(new DataDocument(), new DataDocument(), new DataDocument()), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id2, col3, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<LinkInstance> linkInstances = linkingFacade.readLinkInstancesForCollection(col1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkInstances).hasSize(5);

      linkInstances = linkingFacade.readLinkInstancesForCollection(col2, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkInstances).hasSize(1);

      linkInstances = linkingFacade.readLinkInstancesForCollection(col3, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkInstances).hasSize(3);
   }

   @Test
   public void testReadLinkInstancesBetweenDocuments() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection21"));
      final String col2 = collectionFacade.createCollection(new Collection("collection22"));
      List<String> collections = Arrays.asList(col1, col2);
      Map<String, List<String>> ids = createTestData(collections, 2);

      String col1Id1 = ids.get(col1).get(0);
      String col1Id2 = ids.get(col1).get(1);
      String col2Id1 = ids.get(col2).get(0);
      String col2Id2 = ids.get(col2).get(1);

      String role1 = "role1";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id2, col2, col2Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<LinkInstance> linkInstances = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkInstances).hasSize(1);

      linkInstances = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkInstances).hasSize(1);

      linkInstances = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id2, col2, col2Id2, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(linkInstances).hasSize(1);

      linkInstances = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id2, col2, col2Id2, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(linkInstances).isEmpty();
   }

   @Test
   public void testReadAndDropBetweenDocuments() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection31"));
      final String col2 = collectionFacade.createCollection(new Collection("collection32"));
      final String col3 = collectionFacade.createCollection(new Collection("collection33"));
      List<String> collections = Arrays.asList(col1, col2, col3);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(col1).get(0);
      String col2Id1 = ids.get(col2).get(0);
      String col3Id1 = ids.get(col3).get(2);
      String col3Id2 = ids.get(col3).get(1);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);

      // read link for specific role
      List<LinkInstance> links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);
      linkingFacade.dropLinksBetweenDocuments(col1, col1Id1, col2, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      // read link for all roles
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);
      linkingFacade.dropLinksBetweenDocuments(col1, col1Id1, col2, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      // read link for all roles, but other way
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(2);
      linkingFacade.dropLinksBetweenDocuments(col1, col1Id1, col2, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col2, col2Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();

      // read link for all roles
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col3, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(3);
      linkingFacade.dropLinksBetweenDocuments(col1, col1Id1, col3, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col3, col3Id1, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col3, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);
      linkingFacade.dropLinksBetweenDocuments(col1, col1Id1, col3, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col3, col3Id1, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col3, col3Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();

      links = linkingFacade.readLinkInstancesBetweenDocuments(col1, col1Id1, col3, col3Id2, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

   }

   @Test
   public void testCreateDropCollectionLinks() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection41"));
      final String col2 = collectionFacade.createCollection(new Collection("collection42"));
      final String col3 = collectionFacade.createCollection(new Collection("collection43"));
      List<String> collections = Arrays.asList(col1, col2, col3);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(col1).get(0);
      String col2Id1 = ids.get(col2).get(0);
      String col3Id1 = ids.get(col3).get(0);
      String col3Id2 = ids.get(col3).get(1);
      String col3Id3 = ids.get(col3).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col2, role1, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);

      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col2, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);

      linkingFacade.dropLinksBetweenDocumentAndCollection(col1, col1Id1, col2, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col2, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col3, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(5);

      linkingFacade.dropLinksBetweenDocumentAndCollection(col1, col1Id1, col3, role1, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col3, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);

      linkingFacade.dropLinksBetweenDocumentAndCollection(col1, col1Id1, col3, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col3, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();
   }

   @Test
   public void testDropWholeCollectionLinks() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection51"));
      final String col2 = collectionFacade.createCollection(new Collection("collection52"));
      final String col3 = collectionFacade.createCollection(new Collection("collection53"));
      List<String> collections = Arrays.asList(col1, col2, col3);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(col1).get(0);
      String col2Id1 = ids.get(col2).get(0);
      String col3Id1 = ids.get(col3).get(0);
      String col3Id2 = ids.get(col3).get(1);
      String col3Id3 = ids.get(col3).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col2, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(2);

      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col3, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(3);

      linkingFacade.dropLinksForCollection(col1, null, LumeerConst.Linking.LinkDirection.FROM);
      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col2, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      links = linkingFacade.readLinkedDocumentsBetweenDocumentAndCollection(col1, col1Id1, col3, null, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();
   }

   @Test
   public void testCreateDropAllDocumentLinks() throws Exception {
      final String col1 = collectionFacade.createCollection(new Collection("collection61"));
      final String col2 = collectionFacade.createCollection(new Collection("collection62"));
      final String col3 = collectionFacade.createCollection(new Collection("collection63"));
      List<String> collections = Arrays.asList(col1, col2, col3);
      Map<String, List<String>> ids = createTestData(collections, 3);

      String col1Id1 = ids.get(col1).get(0);
      String col2Id1 = ids.get(col2).get(0);
      String col3Id1 = ids.get(col3).get(0);
      String col3Id2 = ids.get(col3).get(1);
      String col3Id3 = ids.get(col3).get(2);

      String role1 = "role1";
      String role2 = "role2";
      String role3 = "role3";

      dataStorage.dropCollection(buildProjectLinkingCollectionName());

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role2, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role3, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id3, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);

      List<DataDocument> links = linkingFacade.readLinkedDocumentsForDocument(col1, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(4);
      links = linkingFacade.readLinkedDocumentsForDocument(col1, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(8);

      linkingFacade.dropLinksForDocument(col1, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readLinkedDocumentsForDocument(col1, col1Id1, role1, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();

      links = linkingFacade.readLinkedDocumentsForDocument(col1, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(4);

      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id1, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col3, col3Id2, new DataDocument(), role1, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readLinkedDocumentsForDocument(col1, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).hasSize(7);

      linkingFacade.dropLinksForDocument(col1, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      links = linkingFacade.readLinkedDocumentsForDocument(col1, col1Id1, null, LumeerConst.Linking.LinkDirection.TO);
      assertThat(links).isEmpty();
   }

   @Test
   public void testProjectSwitching() throws DbException {
      String project1 = "project1";
      String project2 = "project2";
      projectFacade.createProject(new Project(project1, "p1"));
      projectFacade.setCurrentProjectCode(project1);
      final String col1 = collectionFacade.createCollection(new Collection("collection71"));
      final String col2 = collectionFacade.createCollection(new Collection("collection72"));

      List<String> collections = Arrays.asList(col1, col2);
      Map<String, List<String>> ids = createTestData(collections, 1);
      String col1Id1 = ids.get(col1).get(0);
      String col2Id1 = ids.get(col2).get(0);

      String role = "role";
      linkingFacade.createLinkInstanceBetweenDocuments(col1, col1Id1, col2, col2Id1, new DataDocument(), role, LumeerConst.Linking.LinkDirection.FROM);

      projectFacade.createProject(new Project(project2, "p2"));
      projectFacade.setCurrentProjectCode(project2);
      collectionFacade.createCollection(new Collection("collection71"));
      collectionFacade.createCollection(new Collection("collection72"));

      projectFacade.setCurrentProjectCode(project1);
      List<LinkInstance> links = linkingFacade.readLinkInstancesForCollection(collectionMetadataFacade.getCollectionCodeFromName("collection71"), role, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);

      projectFacade.setCurrentProjectCode(project2);
      links = linkingFacade.readLinkInstancesForCollection(collectionMetadataFacade.getCollectionCodeFromName("collection71"), role, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).isEmpty();

      projectFacade.setCurrentProjectCode(project1);
      links = linkingFacade.readLinkInstancesForCollection(collectionMetadataFacade.getCollectionCodeFromName("collection71"), role, LumeerConst.Linking.LinkDirection.FROM);
      assertThat(links).hasSize(1);
   }

   private Map<String, List<String>> createTestData(List<String> collections, int numDocuments) {
      Map<String, List<String>> ids = new HashMap<>();
      for (String col : collections) {
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
      return LumeerConst.Linking.PREFIX + "_" + projectFacade.getCurrentProjectCode();
   }

}
