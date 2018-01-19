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
package io.lumeer.storage.mongodb.dao.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

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
   private static final String COLLECTION_CODE1 = "Cars";
   private static final String COLLECTION_CODE2 = "Engine";
   private static final String ATTRIBUTE1_NAME = "Maxi";
   private static final String ATTRIBUTE2_NAME = "Light";
   private static final List<JsonAttribute> ATTRIBUTES;

   private static final String NAME2 = "Linking";
   private static final String NAME3 = "Spoiler";
   private static final String NAME4 = "Lego";
   private static final String COLLECTION_CODE3 = "Services";
   private static final String COLLECTION_CODE4 = "Tires";

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";
   private static final String USER = "lumeer@lumeer.io";

   static {
      JsonAttribute attribute1 = new JsonAttribute(ATTRIBUTE1_NAME);
      JsonAttribute attribute2 = new JsonAttribute(ATTRIBUTE2_NAME);
      ATTRIBUTES = Arrays.asList(attribute1, attribute2);
   }

   private MongoLinkTypeDao linkTypeDao;

   private Project project;

   @Before
   public void initViewDao() {
      project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      linkTypeDao = new MongoLinkTypeDao();
      linkTypeDao.setDatabase(database);
      linkTypeDao.setDatastore(datastore);

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

      LinkType storedLinkType = linkTypeDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME);
      assertThat(storedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedLinkType.getCollectionCodes()).containsOnlyElementsOf(Arrays.asList(COLLECTION_CODE1, COLLECTION_CODE2));
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
      updateLinkedType.setCollectionCodes(Arrays.asList(COLLECTION_CODE3, COLLECTION_CODE4));

      linkTypeDao.updateLinkType(id, updateLinkedType);

      LinkType storedLinkType = linkTypeDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME2);
      assertThat(storedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedLinkType.getCollectionCodes()).containsOnlyElementsOf(Arrays.asList(COLLECTION_CODE3, COLLECTION_CODE4));
   }

   @Test
   public void testUpdateLinkTypeExistingName() {
      LinkType linkType = prepareLinkType();
      linkTypeDao.createLinkType(linkType);

      LinkType linkType2 = prepareLinkType();
      linkType2.setName(NAME2);
      String id = linkTypeDao.createLinkType(linkType2).getId();

      linkType2.setName(NAME);

      assertThat(linkTypeDao.updateLinkType(id, linkType2)).isNotNull();
   }

   @Test
   public void testDeleteLinkType() {
      LinkType created = linkTypeDao.createLinkType(prepareLinkType());
      assertThat(created.getId()).isNotNull();

      linkTypeDao.deleteLinkType(created.getId());

      LinkType stored = linkTypeDao.databaseCollection().find(MongoFilters.idFilter(created.getId())).first();
      assertThat(stored).isNull();
   }

   @Test
   public void testDeleteLinkTypeNotExisting() {
      assertThatThrownBy(() -> linkTypeDao.deleteLinkType(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkTypesByCollections(){
      linkTypeDao.createLinkType(prepareLinkType());

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionCodes(Arrays.asList(COLLECTION_CODE2, COLLECTION_CODE3));
      linkTypeDao.createLinkType(linkType2);

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionCodes(Arrays.asList(COLLECTION_CODE2, COLLECTION_CODE4));
      linkTypeDao.createLinkType(linkType3);

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionCodes(Arrays.asList(COLLECTION_CODE3, COLLECTION_CODE4));
      linkTypeDao.createLinkType(linkType4);

      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(4);

      SearchQuery query1 = SearchQuery.createBuilder(USER).collectionCodes(Collections.singleton(COLLECTION_CODE2)).build();
      linkTypeDao.deleteLinkTypes(query1);
      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(1);

      SearchQuery query2 = SearchQuery.createBuilder(USER).collectionCodes(Collections.singleton(COLLECTION_CODE3)).build();
      linkTypeDao.deleteLinkTypes(query2);
      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>())).isEmpty();

   }

   @Test
   public void testDeleteLinkTypesByIds() throws InterruptedException {
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setName(NAME2);
      String id2 = linkTypeDao.createLinkType(linkType2).getId();

      LinkType linkType3 = prepareLinkType();
      linkType3.setName(NAME3);
      String id3 = linkTypeDao.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setName(NAME4);
      String id4 = linkTypeDao.createLinkType(linkType4).getId();

      assertThat(linkTypeDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(4);

      SearchQuery query = SearchQuery.createBuilder(USER).linkTypeIds(new HashSet<>(Arrays.asList(id1, id4))).build();
      linkTypeDao.deleteLinkTypes(query);
      List<LinkType> linkTypes = linkTypeDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id2, id3));
   }

   @Test
   public void testGetLinkTypesByCollections(){
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionCodes(Arrays.asList(COLLECTION_CODE3, COLLECTION_CODE4));
      String id2 = linkTypeDao.createLinkType(linkType2).getId();

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionCodes(Arrays.asList(COLLECTION_CODE1, COLLECTION_CODE4));
      String id3 = linkTypeDao.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionCodes(Arrays.asList(COLLECTION_CODE2, COLLECTION_CODE3));
      String id4 = linkTypeDao.createLinkType(linkType4).getId();

      SearchQuery query1 = SearchQuery.createBuilder(USER).collectionCodes(new HashSet<>(Arrays.asList(COLLECTION_CODE1, COLLECTION_CODE2))).build();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypes(query1);
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3, id4));

      SearchQuery query2 = SearchQuery.createBuilder(USER).collectionCodes(new HashSet<>(Collections.singletonList(COLLECTION_CODE3))).build();
      linkTypes = linkTypeDao.getLinkTypes(query2);
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id2, id4));

   }

   @Test
   public void testGetLinkTypesByIds(){
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

      SearchQuery query1 = SearchQuery.createBuilder(USER).linkTypeIds(new HashSet<>(Arrays.asList(id1, id4))).build();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypes(query1);
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id4));

      SearchQuery query2 = SearchQuery.createBuilder(USER).linkTypeIds(new HashSet<>(Collections.singletonList(id2))).build();
      linkTypes = linkTypeDao.getLinkTypes(query2);
      assertThat(linkTypes).extracting("id").containsOnly(id2);

   }

   private LinkType prepareLinkType() {
      return new LinkType(NAME, Arrays.asList(COLLECTION_CODE1, COLLECTION_CODE2), ATTRIBUTES);
   }
}
