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
package io.lumeer.storage.mongodb.dao.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchSuggestionQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MongoLinkTypeDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa28a";

   private static final String NAME = "Connection";
   private static final String COLLECTION_ID1 = "Cars";
   private static final String COLLECTION_ID2 = "Engine";
   private static final String ATTRIBUTE1_NAME = "Maxi";
   private static final String ATTRIBUTE2_NAME = "Light";
   private static final List<Attribute> ATTRIBUTES;

   private static final String NAME2 = "Linking";
   private static final String NAME3 = "Spoiler";
   private static final String NAME4 = "Lego";
   private static final String COLLECTION_ID3 = "Services";
   private static final String COLLECTION_ID4 = "Tires";

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";
   private static final String USER = "user";

   static {
      Attribute attribute1 = new Attribute(ATTRIBUTE1_NAME);
      Attribute attribute2 = new Attribute(ATTRIBUTE2_NAME);
      ATTRIBUTES = Arrays.asList(attribute1, attribute2);
   }

   private MongoLinkTypeDao linkTypeDao;

   private Project project;

   @Before
   public void initLinkTypeDao() {
      project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      linkTypeDao = new MongoLinkTypeDao();
      linkTypeDao.setDatabase(database);

      linkTypeDao.setProject(project);
      linkTypeDao.createLinkTypeRepository(project);
      assertThat(database.listCollectionNames()).contains(linkTypeDao.databaseCollectionName());
   }

   @Test
   public void testDeleteLinkTypeRepository() {
      linkTypeDao.deleteLinkTypeRepository(project);
      assertThat(database.listCollectionNames()).doesNotContain(linkTypeDao.databaseCollectionName());
   }

   @Test
   public void testCreateLinkType() {
      LinkType linkType = prepareLinkType();

      String id = linkTypeDao.createLinkType(linkType).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME);
      assertThat(storedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedLinkType.getCollectionIds()).containsOnlyElementsOf(Arrays.asList(COLLECTION_ID1, COLLECTION_ID2));
   }

   @Test
   public void testCreateLinkTypeExistingName() {
      linkTypeDao.createLinkType(prepareLinkType());

      assertThat(linkTypeDao.createLinkType(prepareLinkType())).isNotNull();
   }

   @Test
   public void testUpdateLinkType() {
      LinkType linkType = prepareLinkType();
      String id = linkTypeDao.createLinkType(linkType).getId();

      LinkType updateLinkedType = prepareLinkType();
      updateLinkedType.setName(NAME2);
      updateLinkedType.setCollectionIds(Arrays.asList(COLLECTION_ID3, COLLECTION_ID4));

      linkTypeDao.updateLinkType(id, updateLinkedType, null);

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME2);
      assertThat(storedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedLinkType.getCollectionIds()).containsOnlyElementsOf(Arrays.asList(COLLECTION_ID3, COLLECTION_ID4));
   }

   @Test
   public void testUpdateLinkTypeExistingName() {
      LinkType linkType = prepareLinkType();
      linkTypeDao.createLinkType(linkType);

      LinkType linkType2 = prepareLinkType();
      linkType2.setName(NAME2);
      String id = linkTypeDao.createLinkType(linkType2).getId();

      linkType2.setName(NAME);

      assertThat(linkTypeDao.updateLinkType(id, linkType2, null)).isNotNull();
   }

   @Test
   public void testGetLinkType() {
      String id = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType = linkTypeDao.getLinkType(id);
      assertThat(linkType).isNotNull();
      assertThat(linkType.getId()).isEqualTo(id);
   }

   @Test
   public void testGetLinkTypeNotExisting() {
      assertThatThrownBy(() -> linkTypeDao.getLinkType(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkType() {
      LinkType created = linkTypeDao.createLinkType(prepareLinkType());
      assertThat(created.getId()).isNotNull();

      linkTypeDao.deleteLinkType(created.getId());

      assertThatThrownBy(() -> linkTypeDao.getLinkType(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkTypeNotExisting() {
      assertThatThrownBy(() -> linkTypeDao.deleteLinkType(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkTypesByCollection() {
      linkTypeDao.createLinkType(prepareLinkType());

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionIds(Arrays.asList(COLLECTION_ID2, COLLECTION_ID3));
      linkTypeDao.createLinkType(linkType2);

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionIds(Arrays.asList(COLLECTION_ID2, COLLECTION_ID4));
      linkTypeDao.createLinkType(linkType3);

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionIds(Arrays.asList(COLLECTION_ID3, COLLECTION_ID4));
      linkTypeDao.createLinkType(linkType4);

      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(4);

      linkTypeDao.deleteLinkTypesByCollectionId(COLLECTION_ID2);
      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(1);

      linkTypeDao.deleteLinkTypesByCollectionId(COLLECTION_ID3);
      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>())).isEmpty();
   }

   @Test
   public void testGetLinkTypesByCollections() {
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionIds(Arrays.asList(COLLECTION_ID3, COLLECTION_ID4));
      String id2 = linkTypeDao.createLinkType(linkType2).getId();

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionIds(Arrays.asList(COLLECTION_ID1, COLLECTION_ID4));
      String id3 = linkTypeDao.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionIds(Arrays.asList(COLLECTION_ID2, COLLECTION_ID3));
      String id4 = linkTypeDao.createLinkType(linkType4).getId();

      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByCollectionId(COLLECTION_ID1);
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3));

      linkTypes = linkTypeDao.getLinkTypesByCollectionId(COLLECTION_ID3);
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id2, id4));
   }

   @Test
   public void testGetLinkTypesByIds() {
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setName(NAME2);
      String id2 = linkTypeDao.createLinkType(linkType2).getId();

      LinkType linkType3 = prepareLinkType();
      linkType3.setName(NAME3);
      linkTypeDao.createLinkType(linkType3);

      LinkType linkType4 = prepareLinkType();
      linkType4.setName(NAME4);
      String id4 = linkTypeDao.createLinkType(linkType4).getId();

      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByIds(new HashSet<>(Arrays.asList(id1, id4)));
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id4));

      linkTypes = linkTypeDao.getLinkTypesByIds(Collections.singleton(id2));
      assertThat(linkTypes).extracting("id").containsOnly(id2);
   }

   @Test
   public void testGetLinkTypesBySuggestions() {
      String id1 = createLinkType("spot", "c1", "c2").getId();
      String id2 = createLinkType("sport", "c2", "c3").getId();
      String id3 = createLinkType("porto", "c1", "c3").getId();
      String id4 = createLinkType("post", "c3", "c4").getId();
      createLinkType("shop", "c1", "c4");
      String id6 = createLinkType("point", "c2", "c4").getId();

      SearchSuggestionQuery query = SearchSuggestionQuery.createBuilder(USER)
                                                         .text("po")
                                                         .build();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypes(query);
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(id1, id2, id3, id4, id6);

      query = SearchSuggestionQuery.createBuilder(USER)
                                   .text("po")
                                   .collectionIds(new HashSet<>(Arrays.asList("c1", "c2", "c4")))
                                   .build();
      linkTypes = linkTypeDao.getLinkTypes(query);
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(id1, id6);
   }

   @Test
   public void testGetLinkTypesBySuggestionsPriority() {
      String id1 = createLinkType("spot", "c1", "c2").getId();
      String id2 = createLinkType("spot", "c2", "c3").getId();
      String id3 = createLinkType("spot", "c1", "c3").getId();
      String id4 = createLinkType("spot", "c3", "c4").getId();
      String id5 = createLinkType("spot", "c2", "c4").getId();

      SearchSuggestionQuery query = SearchSuggestionQuery.createBuilder(USER)
                                                         .text("po")
                                                         .page(0)
                                                         .pageSize(3)
                                                         .build();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypes(query);
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(id1, id2, id3);

      query = SearchSuggestionQuery.createBuilder(USER)
                                   .text("po")
                                   .page(0)
                                   .pageSize(3)
                                   .priorityCollectionIds(Collections.singleton("c4"))
                                   .build();
      linkTypes = linkTypeDao.getLinkTypes(query);
      assertThat(linkTypes).extracting(LinkType::getId).contains(id4, id5);
   }

   private LinkType createLinkType(String name, String collId1, String collId2) {
      LinkType linkType = new LinkType(name, Arrays.asList(collId1, collId2), ATTRIBUTES);
      return linkTypeDao.createLinkType(linkType);
   }

   private LinkType prepareLinkType() {
      return new LinkType(NAME, Arrays.asList(COLLECTION_ID1, COLLECTION_ID2), ATTRIBUTES);
   }
}
