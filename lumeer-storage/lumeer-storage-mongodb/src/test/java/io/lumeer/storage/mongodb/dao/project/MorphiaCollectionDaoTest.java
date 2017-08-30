/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.mongodb.dao.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.model.MorphiaCollection;
import io.lumeer.storage.mongodb.model.embedded.MorphiaAttribute;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MorphiaCollectionDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String COLLECTION_ID = "59a4348a8eed1e53942d2d2b";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String COLOR = "#0000ff";
   private static final String ICON = "fa-eye";
   private static final Set<Attribute> ATTRIBUTES;
   private static final Integer DOCUMENTS_COUNT = 0;
   private static final LocalDateTime LAST_TIME_USED = LocalDateTime.now();

   private static final MorphiaPermissions PERMISSIONS = new MorphiaPermissions();
   private static final MorphiaPermission USER_PERMISSION;
   private static final MorphiaPermission GROUP_PERMISSION;

   private static final String USER2 = "testUser2";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE2 = "TCOLL2";
   private static final String CODE3 = "FULLTEXT";
   private static final String CODE4 = "TCOLL4";

   private static final String NAME_FULLTEXT = "Fulltext name";
   private static final String NAME_SUGGESTION = "TESTING suggestions";

   private static final Set<Attribute> ATTRIBUTES_FULLTEXT;

   private static final String ATTRIBUTE1_NAME = "suggestion";
   private static final String ATTRIBUTE2_NAME = "fulltext";

   static {
      Attribute attribute = new MorphiaAttribute();
      attribute.setName(ATTRIBUTE1_NAME);
      ATTRIBUTES = Collections.singleton(attribute);

      USER_PERMISSION = new MorphiaPermission(USER, View.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));
      PERMISSIONS.updateUserPermissions(USER_PERMISSION);

      GROUP_PERMISSION = new MorphiaPermission(GROUP, Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);

      Attribute attribute1 = new MorphiaAttribute();
      attribute1.setName(ATTRIBUTE1_NAME);
      Attribute attribute2 = new MorphiaAttribute();
      attribute2.setName(ATTRIBUTE2_NAME);
      ATTRIBUTES_FULLTEXT = new HashSet<>(Arrays.asList(attribute1, attribute2));
   }

   private MorphiaCollectionDao collectionDao;

   @Before
   public void initCollectionDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      collectionDao = new MorphiaCollectionDao();
      collectionDao.setDatabase(database);
      collectionDao.setDatastore(datastore);

      collectionDao.setProject(project);
      collectionDao.createCollectionsRepository(project);
   }

   private MorphiaCollection prepareCollection(String code) {
      MorphiaCollection collection = new MorphiaCollection();
      collection.setCode(code);
      collection.setName(NAME);
      collection.setColor(COLOR);
      collection.setIcon(ICON);
      collection.setPermissions(new MorphiaPermissions(PERMISSIONS));
      collection.setAttributes(ATTRIBUTES);
      collection.setDocumentsCount(DOCUMENTS_COUNT);
      collection.setLastTimeUsed(LAST_TIME_USED);
      return collection;
   }

   private MorphiaCollection createCollection(String code) {
      MorphiaCollection collection = prepareCollection(code);
      datastore.insert(collectionDao.databaseCollection(), collection);
      return collection;
   }

   private MorphiaCollection createCollection(String code, String name, Set<Attribute> attributes) {
      MorphiaCollection collection = prepareCollection(code);
      collection.setName(name);
      collection.setAttributes(attributes);
      datastore.insert(collectionDao.databaseCollection(), collection);
      return collection;
   }

   @Test
   public void testCreateCollection() {
      MorphiaCollection collection = prepareCollection(CODE);

      String id = collectionDao.createCollection(collection).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      MorphiaCollection storedCollection = datastore.get(collectionDao.databaseCollection(), MorphiaCollection.class, new ObjectId(id));
      assertThat(storedCollection).isNotNull();
      assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertThat(storedCollection.getPermissions()).isEqualTo(PERMISSIONS);
      assertThat(storedCollection.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedCollection.getDocumentsCount()).isEqualTo(DOCUMENTS_COUNT);
      assertThat(storedCollection.getLastTimeUsed()).isEqualTo(LAST_TIME_USED);
   }

   @Test
   public void testCreateCollectionAlreadyExists() {
      createCollection(CODE);

      MorphiaCollection collection = prepareCollection(CODE);
      assertThatThrownBy(() -> collectionDao.createCollection(collection))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testUpdateCollection() {
      String id = createCollection(CODE).getId();

      MorphiaCollection collection = prepareCollection(CODE2);
      Collection updatedCollection = collectionDao.updateCollection(id, collection);
      assertThat(updatedCollection).isNotNull();
      assertThat(updatedCollection.getCode()).isEqualTo(CODE2);

      Collection storedCollection = datastore.get(collectionDao.databaseCollection(), MorphiaCollection.class, new ObjectId(id));
      assertThat(storedCollection).isNotNull();
      assertThat(updatedCollection).isEqualTo(storedCollection);
   }

   @Test
   @Ignore("Stored anyway with the current implementation")
   public void testUpdateCollectionNotExisting() {
      MorphiaCollection collection = prepareCollection(CODE);
      assertThatThrownBy(() -> collectionDao.updateCollection(COLLECTION_ID, collection))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteCollection() {
      String id = createCollection(CODE).getId();

      collectionDao.deleteCollection(id);

      Collection storedCollection = datastore.get(collectionDao.databaseCollection(), MorphiaCollection.class, new ObjectId(id));
      assertThat(storedCollection).isNull();
   }

   @Test
   public void testDeleteCollectionNotExisting() {
      assertThatThrownBy(() -> collectionDao.deleteCollection(COLLECTION_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetCollectionByCode() {
      createCollection(CODE);

      Collection collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getCode()).isEqualTo(CODE);
   }

   @Test
   public void testGetCollectionByCodeNotExisting() {
      assertThatThrownBy(() -> collectionDao.getCollectionByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetCollections() {
      MorphiaCollection collection = prepareCollection(CODE);
      datastore.save(collectionDao.databaseCollection(), collection);

      MorphiaCollection collection2 = prepareCollection(CODE);
      collection2.setCode(CODE2);
      datastore.save(collectionDao.databaseCollection(), collection2);

      SearchQuery query = SearchQuery.createBuilder(USER).build();
      List<Collection> views = collectionDao.getCollections(query);
      assertThat(views).extracting(Collection::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetCollectionsNoReadRole() {
      MorphiaCollection collection = prepareCollection(CODE);
      Permission userPermission = new MorphiaPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      collection.getPermissions().updateUserPermissions(userPermission);
      datastore.save(collectionDao.databaseCollection(), collection);

      MorphiaCollection collection2 = prepareCollection(CODE);
      collection2.setCode(CODE2);
      Permission groupPermission = new MorphiaPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      collection2.getPermissions().updateGroupPermissions(groupPermission);
      datastore.save(collectionDao.databaseCollection(), collection2);

      SearchQuery query = SearchQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<Collection> collections = collectionDao.getCollections(query);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetCollectionsGroupsRole() {
      MorphiaCollection collection = prepareCollection(CODE);
      datastore.save(collectionDao.databaseCollection(), collection);

      MorphiaCollection collection2 = prepareCollection(CODE);
      collection2.setCode(CODE2);
      datastore.save(collectionDao.databaseCollection(), collection2);

      SearchQuery query = SearchQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<Collection> collections = collectionDao.getCollections(query);
      assertThat(collections).extracting(Collection::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetCollectionsPagination() {
      createCollection(CODE);
      createCollection(CODE2);
      createCollection(CODE3);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .page(1).pageSize(1)
                                           .build();
      List<Collection> collections = collectionDao.getCollections(searchQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetCollectionsByCollectionCodes() {
      MorphiaCollection collection = prepareCollection(CODE);
      collection.getPermissions().removeUserPermission(USER);
      Permission userPermission = new MorphiaPermission(USER2, Collections.singleton(Role.READ.toString()));
      collection.getPermissions().updateUserPermissions(userPermission);
      datastore.save(collectionDao.databaseCollection(), collection);

      MorphiaCollection collection2 = prepareCollection(CODE);
      collection2.setCode(CODE2);
      datastore.save(collectionDao.databaseCollection(), collection2);

      MorphiaCollection collection3 = prepareCollection(CODE);
      collection3.setCode(CODE3);
      datastore.save(collectionDao.databaseCollection(), collection3);

      SearchQuery query = SearchQuery.createBuilder(USER)
                                     .collectionCodes(new HashSet<>(Arrays.asList(CODE, CODE3)))
                                     .build();
      List<Collection> views = collectionDao.getCollections(query);
      assertThat(views).extracting(Collection::getCode).containsOnly(CODE3);
   }

   @Test
   public void testGetCollectionsByFulltext() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME, ATTRIBUTES);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .fulltext("fulltext")
                                           .build();
      List<Collection> collections = collectionDao.getCollections(searchQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2, CODE3);
   }

   @Test
   @Ignore("Fulltext index on attribute names does not work at the moment")
   public void testGetCollectionsByFulltextWithAttributes() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME, ATTRIBUTES);
      createCollection(CODE4, NAME, ATTRIBUTES_FULLTEXT);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .fulltext("fulltext")
                                           .build();
      List<Collection> collections = collectionDao.getCollections(searchQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2, CODE3, CODE4);
   }

   @Test
   public void testGetCollectionsByFulltextAndCollectionCodes() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME, ATTRIBUTES);
      createCollection(CODE4, NAME_FULLTEXT, ATTRIBUTES);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .collectionCodes(new HashSet<>(Arrays.asList(CODE, CODE2, CODE3)))
                                           .fulltext("fulltext")
                                           .build();
      List<Collection> collections = collectionDao.getCollections(searchQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2, CODE3);
   }

   @Test
   public void testGetCollectionsByFulltextDifferentUser() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME, ATTRIBUTES);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER2)
                                           .fulltext("fulltext")
                                           .build();
      List<Collection> collections = collectionDao.getCollections(searchQuery);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetCollectionsSuggestions() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME_SUGGESTION, ATTRIBUTES);

      SuggestionQuery suggestionQuery = SuggestionQuery.createBuilder(USER)
                                                       .text("TEST")
                                                       .build();
      List<Collection> collections = collectionDao.getCollections(suggestionQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE, CODE3);
   }

   @Test
   public void testGetCollectionsSuggestionsDifferentUser() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME_SUGGESTION, ATTRIBUTES);

      SuggestionQuery suggestionQuery = SuggestionQuery.createBuilder(USER2)
                                                       .text("TEST")
                                                       .build();
      List<Collection> collections = collectionDao.getCollections(suggestionQuery);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetCollectionsByAttributesSuggestions() {
      createCollection(CODE, NAME, Collections.emptySet());
      createCollection(CODE2, NAME, ATTRIBUTES);
      createCollection(CODE3, NAME, ATTRIBUTES_FULLTEXT);

      SuggestionQuery suggestionQuery = SuggestionQuery.createBuilder(USER)
                                                       .text("sugg")
                                                       .build();
      List<Collection> collections = collectionDao.getCollectionsByAttributes(suggestionQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2, CODE3);
   }

   @Test
   public void testGetCollectionsByAttributesSuggestionsDifferentUser() {
      createCollection(CODE, NAME, Collections.emptySet());
      createCollection(CODE2, NAME, ATTRIBUTES);
      createCollection(CODE3, NAME, ATTRIBUTES_FULLTEXT);

      SuggestionQuery suggestionQuery = SuggestionQuery.createBuilder(USER2)
                                                       .text("sugg")
                                                       .build();
      List<Collection> collections = collectionDao.getCollectionsByAttributes(suggestionQuery);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetAllCollectionsCodes() {
      assertThat(collectionDao.getAllCollectionCodes()).isEmpty();

      createCollection(CODE);
      createCollection(CODE2);
      createCollection(CODE3);

      assertThat(collectionDao.getAllCollectionCodes()).contains(CODE, CODE2, CODE3);
   }

}
