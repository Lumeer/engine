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

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;
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
import java.util.Map;

public class MongoLinkInstanceDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa28a";

   private static final String LINK_TYPE_ID1 = "596e3b86d412bc5a3caaa29a";
   private static final String LINK_TYPE_ID2 = "596e3b86d412bc5a3caaa30a";
   private static final String LINK_TYPE_ID3 = "596e3b86d412bc5a3caaa31a";

   private static final String DOCUMENT_ID1 = "596e3b86d412bc5a3caaa32a";
   private static final String DOCUMENT_ID2 = "596e3b86d412bc5a3caaa33a";
   private static final String DOCUMENT_ID3 = "596e3b86d412bc5a3caaa34a";
   private static final String DOCUMENT_ID4 = "596e3b86d412bc5a3caaa35a";
   private static final String DOCUMENT_ID5 = "596e3b86d412bc5a3caaa36a";

   private static final Map<String, Object> DATA;

   static {
      DATA = Collections.singletonMap("entry", "value");
   }

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";
   private static final String USER = "user";

   private MongoLinkInstanceDao linkInstanceDao;

   private Project project;

   @Before
   public void initLinkInstanceDao() {
      project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      linkInstanceDao = new MongoLinkInstanceDao();
      linkInstanceDao.setDatabase(database);

      linkInstanceDao.setProject(project);
      linkInstanceDao.createLinkInstanceRepository(project);
      assertThat(database.listCollectionNames()).contains(linkInstanceDao.databaseCollectionName());
   }

   @Test
   public void testDeleteLinkInstanceRepository() {
      linkInstanceDao.deleteLinkInstanceRepository(project);
      assertThat(database.listCollectionNames()).doesNotContain(linkInstanceDao.databaseCollectionName());
   }

   @Test
   public void testCreateLinkInstance() {
      LinkInstance linkInstance = prepareLinkInstance();

      String id = linkInstanceDao.createLinkInstance(linkInstance).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      LinkInstance storedLinkInstance = linkInstanceDao.getLinkInstance(id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getLinkTypeId()).isEqualTo(LINK_TYPE_ID1);
      assertThat(storedLinkInstance.getDocumentIds()).containsOnlyElementsOf(Arrays.asList(DOCUMENT_ID1, DOCUMENT_ID2));
   }

   @Test
   public void testCreateLinkInstanceExistingLinkTypeId() {
      linkInstanceDao.createLinkInstance(prepareLinkInstance());

      assertThat(linkInstanceDao.createLinkInstance(prepareLinkInstance())).isNotNull();
   }

   @Test
   public void testUpdateLinkInstance() {
      LinkInstance linkInstance = prepareLinkInstance();
      String id = linkInstanceDao.createLinkInstance(linkInstance).getId();

      LinkInstance updateLinkedInstance = prepareLinkInstance();
      updateLinkedInstance.setLinkTypeId(LINK_TYPE_ID2);
      updateLinkedInstance.setDocumentIds(Arrays.asList(DOCUMENT_ID3, DOCUMENT_ID4));

      linkInstanceDao.updateLinkInstance(id, updateLinkedInstance);

      LinkInstance storedLinkInstance = linkInstanceDao.getLinkInstance(id);
      assertThat(storedLinkInstance).isNotNull();
      assertThat(storedLinkInstance.getLinkTypeId()).isEqualTo(LINK_TYPE_ID2);
      assertThat(storedLinkInstance.getDocumentIds()).containsOnlyElementsOf(Arrays.asList(DOCUMENT_ID3, DOCUMENT_ID4));
   }

   @Test
   public void testGetLinkType() {
      String id = linkInstanceDao.createLinkInstance(prepareLinkInstance()).getId();

      LinkInstance linkInstance = linkInstanceDao.getLinkInstance(id);
      assertThat(linkInstance).isNotNull();
      assertThat(linkInstance.getId()).isEqualTo(id);
   }

   @Test
   public void testGetLinkInstanceNotExisting() {
      assertThatThrownBy(() -> linkInstanceDao.getLinkInstance(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkInstance() {
      LinkInstance created = linkInstanceDao.createLinkInstance(prepareLinkInstance());
      assertThat(created.getId()).isNotNull();

      linkInstanceDao.deleteLinkInstance(created.getId());

      assertThatThrownBy(() -> linkInstanceDao.getLinkInstance(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkInstanceNotExisting() {
      assertThatThrownBy(() -> linkInstanceDao.deleteLinkInstance(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteLinkInstancesByDocumentIds() {
      linkInstanceDao.createLinkInstance(prepareLinkInstance());

      LinkInstance linkInstance2 = prepareLinkInstance();
      linkInstance2.setDocumentIds(Arrays.asList(DOCUMENT_ID2, DOCUMENT_ID3));
      linkInstanceDao.createLinkInstance(linkInstance2);

      LinkInstance linkInstance3 = prepareLinkInstance();
      linkInstance3.setDocumentIds(Arrays.asList(DOCUMENT_ID1, DOCUMENT_ID5));
      linkInstanceDao.createLinkInstance(linkInstance3);

      LinkInstance linkInstance4 = prepareLinkInstance();
      linkInstance4.setDocumentIds(Arrays.asList(DOCUMENT_ID2, DOCUMENT_ID4));
      linkInstanceDao.createLinkInstance(linkInstance4);

      assertThat(linkInstanceDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(4);

      linkInstanceDao.deleteLinkInstancesByDocumentsIds(Collections.singleton(DOCUMENT_ID1));
      assertThat(linkInstanceDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(2);

      linkInstanceDao.deleteLinkInstancesByDocumentsIds(Collections.singleton(DOCUMENT_ID2));
      assertThat(linkInstanceDao.databaseCollection().find().into(new ArrayList<>())).isEmpty();
   }

   @Test
   public void testDeleteLinkInstancesByLinkTypeIds() throws InterruptedException {
      linkInstanceDao.createLinkInstance(prepareLinkInstance());

      LinkInstance linkInstance2 = prepareLinkInstance();
      linkInstance2.setLinkTypeId(LINK_TYPE_ID2);
      linkInstanceDao.createLinkInstance(linkInstance2);

      LinkInstance linkInstance3 = prepareLinkInstance();
      linkInstance3.setLinkTypeId(LINK_TYPE_ID2);
      linkInstanceDao.createLinkInstance(linkInstance3);

      LinkInstance linkInstance4 = prepareLinkInstance();
      linkInstance4.setLinkTypeId(LINK_TYPE_ID3);
      String id4 = linkInstanceDao.createLinkInstance(linkInstance4).getId();

      assertThat(linkInstanceDao.databaseCollection().find().into(new ArrayList<>()).size()).isEqualTo(4);

      linkInstanceDao.deleteLinkInstancesByLinkTypesIds(new HashSet<>(Arrays.asList(LINK_TYPE_ID1, LINK_TYPE_ID2)));
      List<LinkInstance> linkInstances = linkInstanceDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Collections.singletonList(id4));
   }

   @Test
   public void testGetLinkInstancesByDocumentIds() {
      String id1 = linkInstanceDao.createLinkInstance(prepareLinkInstance()).getId();

      LinkInstance linkInstance2 = prepareLinkInstance();
      linkInstance2.setDocumentIds(Arrays.asList(DOCUMENT_ID2, DOCUMENT_ID3));
      String id2 = linkInstanceDao.createLinkInstance(linkInstance2).getId();

      LinkInstance linkInstance3 = prepareLinkInstance();
      linkInstance3.setDocumentIds(Arrays.asList(DOCUMENT_ID1, DOCUMENT_ID5));
      String id3 = linkInstanceDao.createLinkInstance(linkInstance3).getId();

      LinkInstance linkInstance4 = prepareLinkInstance();
      linkInstance4.setDocumentIds(Arrays.asList(DOCUMENT_ID2, DOCUMENT_ID4));
      String id4 = linkInstanceDao.createLinkInstance(linkInstance4).getId();

      SearchQueryStem stem1 = SearchQueryStem.createBuilder("Collection").documentIds(Collections.singleton(DOCUMENT_ID1)).build();
      SearchQuery query1 = SearchQuery.createBuilder(USER).stems(Collections.singletonList(stem1)).build();
      List<LinkInstance> linkInstances = linkInstanceDao.searchLinkInstances(query1);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3));

      SearchQueryStem stem2 = SearchQueryStem.createBuilder("Collection").documentIds(Collections.singleton(DOCUMENT_ID2)).build();
      SearchQuery query2 = SearchQuery.createBuilder(USER).stems(Collections.singletonList(stem2)).build();
      linkInstances = linkInstanceDao.searchLinkInstances(query2);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id2, id4));
   }

   @Test
   public void testGetLinkInstancesByLinkTypeIds() {
      String id1 = linkInstanceDao.createLinkInstance(prepareLinkInstance()).getId();

      LinkInstance linkInstance2 = prepareLinkInstance();
      linkInstance2.setLinkTypeId(LINK_TYPE_ID2);
      String id2 = linkInstanceDao.createLinkInstance(linkInstance2).getId();

      LinkInstance linkInstance3 = prepareLinkInstance();
      linkInstance3.setLinkTypeId(LINK_TYPE_ID2);
      String id3 = linkInstanceDao.createLinkInstance(linkInstance3).getId();

      LinkInstance linkInstance4 = prepareLinkInstance();
      linkInstance4.setLinkTypeId(LINK_TYPE_ID3);
      String id4 = linkInstanceDao.createLinkInstance(linkInstance4).getId();

      SearchQueryStem stem1 = SearchQueryStem.createBuilder("Collection").linkTypeIds(Arrays.asList(LINK_TYPE_ID1, LINK_TYPE_ID2)).build();
      SearchQuery query1 = SearchQuery.createBuilder(USER).stems(Collections.singletonList(stem1)).build();
      List<LinkInstance> linkInstances = linkInstanceDao.searchLinkInstances(query1);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id2, id3));

      SearchQueryStem stem2 = SearchQueryStem.createBuilder("Collection").linkTypeIds(Collections.singletonList(LINK_TYPE_ID3)).build();
      SearchQuery query2 = SearchQuery.createBuilder(USER).stems(Collections.singletonList(stem2)).build();
      linkInstances = linkInstanceDao.searchLinkInstances(query2);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Collections.singletonList(id4));
   }

   private LinkInstance prepareLinkInstance() {
      return new LinkInstance(LINK_TYPE_ID1, Arrays.asList(DOCUMENT_ID1, DOCUMENT_ID2));
   }
}
