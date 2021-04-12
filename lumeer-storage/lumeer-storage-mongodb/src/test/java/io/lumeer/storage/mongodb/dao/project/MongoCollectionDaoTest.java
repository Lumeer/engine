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
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchSuggestionQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MongoCollectionDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String COLLECTION_ID = "59a4348a8eed1e53942d2d2b";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String COLOR = "#0000ff";
   private static final String ICON = "fa-eye";
   private static final Set<Attribute> ATTRIBUTES;
   private static final ZonedDateTime LAST_TIME_USED = ZonedDateTime.now().withNano(0);

   private static final Permissions PERMISSIONS = new Permissions();
   private static final Permissions MANAGE_PERMISSIONS = new Permissions();
   private static final Permission USER_PERMISSION;
   private static final Permission GROUP_PERMISSION;

   private static final String USER2 = "testUser2";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE2 = "TCOLL2";
   private static final String NAME2 = "Test collection 2";
   private static final String CODE3 = "FULLTEXT";
   private static final String NAME3 = "Test collection 3";
   private static final String CODE4 = "TCOLL4";
   private static final String NAME4 = "Test collection 4";

   private static final String NAME_FULLTEXT = "Fulltext name";
   private static final String NAME_SUGGESTION = "TESTING suggestions";

   private static final Set<Attribute> ATTRIBUTES_FULLTEXT;

   private static final String ATTRIBUTE1_NAME = "suggestion";
   private static final String ATTRIBUTE2_NAME = "fulltext";

   private static final String RULE_NAME = "ruleName";
   private static final String CONFIGURATION_KEY = "testKey";
   private static final String CONFIGURATION_VALUE = "testValue";

   static {
      Constraint constraint = new Constraint(ConstraintType.Boolean, new DataDocument());
      Function function = new Function("js", "xml", "error", 12345L, false);
      Attribute attribute = new Attribute("a1", ATTRIBUTE1_NAME, null, constraint, function, 0);
      ATTRIBUTES = Collections.singleton(attribute);

      USER_PERMISSION = Permission.buildWithRoles(USER, Collection.ROLES);
      PERMISSIONS.updateUserPermissions(USER_PERMISSION);

      Permission userManagePermission = Permission.buildWithRoles(USER, Collections.singleton(Role.MANAGE));
      MANAGE_PERMISSIONS.updateUserPermissions(userManagePermission);

      GROUP_PERMISSION = new Permission(GROUP, Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);

      Attribute attribute1 = new Attribute(ATTRIBUTE1_NAME);
      Attribute attribute2 = new Attribute(ATTRIBUTE2_NAME);
      ATTRIBUTES_FULLTEXT = new HashSet<>(Arrays.asList(attribute1, attribute2));
   }

   private MongoCollectionDao collectionDao;

   @Before
   public void initCollectionDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      collectionDao = new MongoCollectionDao();
      collectionDao.setDatabase(database);

      collectionDao.setProject(project);
      collectionDao.createRepository(project);
   }

   private Collection prepareManageCollection(String code, String name) {
      Collection collection = prepareCollection(code, name);
      collection.setPermissions(MANAGE_PERMISSIONS);
      return collection;
   }

   private Collection prepareCollection(String code, String name) {
      Collection collection = new Collection(code, name, ICON, COLOR, "", new Permissions(PERMISSIONS), ATTRIBUTES, new HashMap<>(), "", new CollectionPurpose(CollectionPurposeType.None, null));
      collection.setLastTimeUsed(LAST_TIME_USED);
      return collection;
   }

   private Collection createCollection(String code, String name) {
      Collection collection = prepareCollection(code, name);
      collectionDao.databaseCollection().insertOne(collection);
      return collection;
   }

   private Collection createCollection(String code, String name, Set<Attribute> attributes) {
      Collection collection = prepareCollection(code, name);
      collection.setAttributes(attributes);
      collectionDao.databaseCollection().insertOne(collection);
      return collection;
   }

   @Test
   public void testCreateCollection() {
      Collection collection = prepareCollection(CODE, NAME);

      String id = collectionDao.createCollection(collection).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Collection storedCollection = collectionDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedCollection).isNotNull();
      assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertThat(storedCollection.getPermissions()).isEqualTo(PERMISSIONS);
      assertThat(storedCollection.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedCollection.getLastTimeUsed()).isEqualTo(LAST_TIME_USED);
   }

   @Test
   public void testUpdateCollection() {
      String id = createCollection(CODE, NAME).getId();

      Collection collection = prepareCollection(CODE2, NAME);
      collection.setRules(Map.of(RULE_NAME, new Rule("rule1", Rule.RuleType.AUTO_LINK, Rule.RuleTiming.UPDATE, new DataDocument(CONFIGURATION_KEY, CONFIGURATION_VALUE))));
      Collection updatedCollection = collectionDao.updateCollection(id, collection, null);
      assertThat(updatedCollection).isNotNull();
      assertThat(updatedCollection.getCode()).isEqualTo(CODE2);

      Collection storedCollection = collectionDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedCollection).isNotNull();
      assertThat(updatedCollection).isEqualTo(storedCollection);
      assertThat(updatedCollection.getRules()).hasSize(1).hasEntrySatisfying(RULE_NAME, rule -> {
         assertThat(rule.getType()).isEqualTo(Rule.RuleType.AUTO_LINK);
         assertThat(rule.getConfiguration()).contains(Map.entry(CONFIGURATION_KEY, CONFIGURATION_VALUE));
      });
   }

   @Test
   public void testUpdateCollectionNotExisting() {
      Collection collection = prepareCollection(CODE, NAME);
      assertThatThrownBy(() -> collectionDao.updateCollection(COLLECTION_ID, collection, null))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteCollection() {
      String id = createCollection(CODE, NAME).getId();

      collectionDao.deleteCollection(id);

      Collection storedCollection = collectionDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedCollection).isNull();
   }

   @Test
   public void testDeleteCollectionNotExisting() {
      assertThatThrownBy(() -> collectionDao.deleteCollection(COLLECTION_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetCollectionByCode() {
      createCollection(CODE, NAME);

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
      Collection collection = prepareCollection(CODE, NAME);
      collectionDao.databaseCollection().insertOne(collection);

      Collection collection2 = prepareCollection(CODE2, NAME2);
      collectionDao.databaseCollection().insertOne(collection2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER).build();
      List<Collection> views = collectionDao.getCollections(query);
      assertThat(views).extracting(Collection::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetCollectionsByIds() {
      Collection collection = prepareCollection(CODE, NAME);
      String id = collectionDao.createCollection(collection).getId();

      Collection collection2 = prepareCollection(CODE2, NAME2);
      String id2 = collectionDao.createCollection(collection2).getId();

      Collection collection3 = prepareCollection(CODE3, NAME3);
      String id3 = collectionDao.createCollection(collection3).getId();

      Collection collection4 = prepareCollection(CODE4, NAME4);
      String id4 = collectionDao.createCollection(collection4).getId();

      List<Collection> collections = collectionDao.getCollectionsByIds(Arrays.asList(id, id3));
      assertThat(collections).hasSize(2).extracting("id").containsOnly(id, id3);

      collections = collectionDao.getCollectionsByIds(Arrays.asList(id, id2, id4));
      assertThat(collections).hasSize(3).extracting("id").containsOnly(id, id2, id4);
   }

   @Test
   public void testGetCollectionsNoReadRole() {
      Collection collection = prepareCollection(CODE, NAME);
      Permission userPermission = new Permission(USER2, Collections.singleton(Role.CLONE.toString()));
      collection.getPermissions().updateUserPermissions(userPermission);
      collectionDao.databaseCollection().insertOne(collection);

      Collection collection2 = prepareCollection(CODE2, NAME2);
      Permission groupPermission = new Permission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      collection2.getPermissions().updateGroupPermissions(groupPermission);
      collectionDao.databaseCollection().insertOne(collection2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<Collection> collections = collectionDao.getCollections(query);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetCollectionsGroupsRole() {
      Collection collection = prepareCollection(CODE, NAME);
      collectionDao.databaseCollection().insertOne(collection);

      Collection collection2 = prepareCollection(CODE2, NAME2);
      collectionDao.databaseCollection().insertOne(collection2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<Collection> collections = collectionDao.getCollections(query);
      assertThat(collections).extracting(Collection::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetCollectionsPagination() {
      createCollection(CODE, NAME);
      createCollection(CODE2, NAME2);
      createCollection(CODE3, NAME3);

      DatabaseQuery searchQuery = DatabaseQuery.createBuilder(USER)
                                               .page(1).pageSize(1)
                                               .build();
      List<Collection> collections = collectionDao.getCollections(searchQuery);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetCollectionsSuggestions() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME_SUGGESTION, ATTRIBUTES);

      SearchSuggestionQuery searchSuggestionQuery = SearchSuggestionQuery.createBuilder(USER)
                                                                         .text("TEST")
                                                                         .build();
      List<Collection> collections = collectionDao.getCollections(searchSuggestionQuery, false);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE, CODE3);
   }

   @Test
   public void testGetCollectionsSuggestionsWithPriority() {
      String id1 = createCollection(CODE, "TEST 1", ATTRIBUTES).getId();
      String id2 = createCollection(CODE2, "TEST 2", ATTRIBUTES).getId();
      String id3 = createCollection(CODE3, "TEST 3", ATTRIBUTES).getId();
      String id4 = createCollection(CODE4, "TEST 4", ATTRIBUTES).getId();
      String id5 = createCollection("CODE5", "TEST 5", ATTRIBUTES).getId();

      SearchSuggestionQuery searchSuggestionQuery = SearchSuggestionQuery.createBuilder(USER)
                                                                         .text("TEST")
                                                                         .page(0)
                                                                         .pageSize(3)
                                                                         .build();
      List<Collection> collections = collectionDao.getCollections(searchSuggestionQuery, false);
      assertThat(collections).extracting(Resource::getId).containsOnly(id1, id2, id3);

      searchSuggestionQuery = SearchSuggestionQuery.createBuilder(USER)
                                                   .text("TEST")
                                                   .priorityCollectionIds(new HashSet<>(Arrays.asList(id4, id5)))
                                                   .page(0)
                                                   .pageSize(3)
                                                   .build();
      collections = collectionDao.getCollections(searchSuggestionQuery, false);
      assertThat(collections).extracting(Resource::getId).contains(id4, id5);
   }

   @Test
   public void testGetCollectionsSuggestionsDifferentUser() {
      createCollection(CODE, NAME, ATTRIBUTES);
      createCollection(CODE2, NAME_FULLTEXT, ATTRIBUTES);
      createCollection(CODE3, NAME_SUGGESTION, ATTRIBUTES);

      SearchSuggestionQuery searchSuggestionQuery = SearchSuggestionQuery.createBuilder(USER2)
                                                                         .text("TEST")
                                                                         .build();
      List<Collection> collections = collectionDao.getCollections(searchSuggestionQuery, false);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetCollectionsByAttributesSuggestions() {
      createCollection(CODE, NAME, Collections.emptySet());
      createCollection(CODE2, NAME2, ATTRIBUTES);
      createCollection(CODE3, NAME3, ATTRIBUTES_FULLTEXT);

      SearchSuggestionQuery searchSuggestionQuery = SearchSuggestionQuery.createBuilder(USER)
                                                                         .text("sugg")
                                                                         .build();
      List<Collection> collections = collectionDao.getCollectionsByAttributes(searchSuggestionQuery, false);
      assertThat(collections).extracting(Resource::getCode).containsOnly(CODE2, CODE3);
   }

   @Test
   public void testGetCollectionsByAttributesSuggestionsDifferentUser() {
      createCollection(CODE, NAME, Collections.emptySet());
      createCollection(CODE2, NAME2, ATTRIBUTES);
      createCollection(CODE3, NAME3, ATTRIBUTES_FULLTEXT);

      SearchSuggestionQuery searchSuggestionQuery = SearchSuggestionQuery.createBuilder(USER2)
                                                                         .text("sugg")
                                                                         .build();
      List<Collection> collections = collectionDao.getCollectionsByAttributes(searchSuggestionQuery, false);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testGetAllCollectionsCodes() {
      assertThat(collectionDao.getAllCollectionCodes()).isEmpty();

      createCollection(CODE, NAME);
      createCollection(CODE2, NAME2);
      createCollection(CODE3, NAME3);

      assertThat(collectionDao.getAllCollectionCodes()).contains(CODE, CODE2, CODE3);
   }

   @Test
   public void testGetAllCollectionsNames() {
      assertThat(collectionDao.getAllCollectionCodes()).isEmpty();

      createCollection(CODE, NAME);
      createCollection(CODE2, NAME2);
      createCollection(CODE3, NAME3);

      assertThat(collectionDao.getAllCollectionNames()).contains(NAME, NAME2, NAME3);
   }

   @Test
   public void testGetCollectionsByManagePermission(){
      collectionDao.createCollection(prepareManageCollection(CODE, NAME));
      collectionDao.createCollection(prepareCollection(CODE2, NAME));
      collectionDao.createCollection(prepareManageCollection(CODE3, NAME));

      DatabaseQuery query = DatabaseQuery.createBuilder(USER).build();
      assertThat(collectionDao.getCollections(query)).extracting(Collection::getCode).containsOnly(CODE, CODE2, CODE3);
   }

}
