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

import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MongoViewDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String COLOR = "#000000";
   private static final String ICON = "fa-eye";
   private static final JsonQuery QUERY = new JsonQuery();
   private static final String PERSPECTIVE = "postit";
   private static final Object CONFIG = "configuration object";

   private static final JsonPermissions PERMISSIONS = new JsonPermissions();
   private static final JsonPermission USER_PERMISSION;
   private static final JsonPermission GROUP_PERMISSION;

   private static final String CODE2 = "TVIEW2";
   private static final String NAME2 = "Testing fulltext view";
   private static final String USER2 = "testUser2";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE3 = "FULLTEXT";
   private static final String NAME3 = "Just FULLTEXT view";

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      USER_PERMISSION = new JsonPermission(USER, View.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));
      PERMISSIONS.updateUserPermissions(USER_PERMISSION);

      GROUP_PERMISSION = new JsonPermission(GROUP, Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);
   }

   private MongoViewDao viewDao;

   private Project project;

   @Before
   public void initViewDao() {
      project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      viewDao = new MongoViewDao();
      viewDao.setDatabase(database);

      viewDao.setProject(project);
      viewDao.createViewsRepository(project);
      assertThat(database.listCollectionNames()).contains(viewDao.databaseCollectionName());
   }

   private JsonView prepareView() {
      JsonView view = new JsonView();
      view.setCode(CODE);
      view.setName(NAME);
      view.setColor(COLOR);
      view.setIcon(ICON);
      view.setPermissions(new JsonPermissions(PERMISSIONS));
      view.setQuery(QUERY);
      view.setPerspective(PERSPECTIVE);
      view.setConfig(CONFIG);
      return view;
   }

   private JsonView createView(String code, String name) {
      return createView(code, name, null);
   }

   private JsonView createView(String code, String name, Set<String> collections) {
      JsonView jsonView = prepareView();
      jsonView.setCode(code);
      jsonView.setName(name);
      jsonView.setQuery(new JsonQuery(collections, null, null));

      viewDao.databaseCollection().insertOne(jsonView);
      return jsonView;
   }

   @Test
   public void testDeleteViewsRepository() {
      viewDao.deleteViewsRepository(project);
      assertThat(database.listCollectionNames()).doesNotContain(viewDao.databaseCollectionName());
   }

   @Test
   public void testCreateView() {
      JsonView view = prepareView();

      String id = viewDao.createView(view).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      View storedView = viewDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedView).isNotNull();
      assertThat(storedView.getCode()).isEqualTo(CODE);
      assertThat(storedView.getName()).isEqualTo(NAME);
      assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertThat(storedView.getPermissions()).isEqualTo(PERMISSIONS);
      assertThat(storedView.getQuery()).isEqualTo(QUERY);
      assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertThat(storedView.getConfig()).isEqualTo(CONFIG);
   }

   @Test
   public void testCreateViewExistingCode() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setName(NAME2);
      assertThatThrownBy(() -> viewDao.createView(view2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testCreateViewExistingName() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      assertThatThrownBy(() -> viewDao.createView(view2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateViewCode() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);
      String id = view.getId();
      assertThat(id).isNotNull().isNotEmpty();

      view.setCode(CODE2);
      viewDao.updateView(id, view);

      View storedView = viewDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedView).isNotNull();
      assertThat(storedView.getCode()).isEqualTo(CODE2);
   }

   @Test
   public void testUpdateViewPermissions() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);
      String id = view.getId();
      assertThat(id).isNotNull().isNotEmpty();

      view.getPermissions().removeUserPermission(USER);
      view.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      viewDao.updateView(id, view);

      View storedView = viewDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedView).isNotNull();
      assertThat(storedView.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedView.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateViewExistingCode() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      view2.setCode(CODE);
      assertThatThrownBy(() -> viewDao.updateView(view2.getId(), view2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateViewExistingName() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      view2.setName(NAME);
      assertThatThrownBy(() -> viewDao.updateView(view2.getId(), view2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   @Ignore("Stored anyway with the current implementation")
   public void testUpdateViewNotExisting() {

   }

   @Test
   public void testDeleteView() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);
      assertThat(view.getId()).isNotNull();

      viewDao.deleteView(view.getId());

      View storedView = viewDao.databaseCollection().find(MongoFilters.idFilter(view.getId())).first();
      assertThat(storedView).isNull();
   }

   @Test
   public void testDeleteViewNotExisting() {
      assertThatThrownBy(() -> viewDao.deleteView(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetViewByCode() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      View storedView = viewDao.getViewByCode(CODE);
      assertThat(storedView).isNotNull();
      assertThat(storedView.getCode()).isEqualTo(view.getCode());
   }

   @Test
   public void testGetViewByCodeNotExisting() {
      assertThatThrownBy(() -> viewDao.getViewByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetViews() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      SearchQuery query = SearchQuery.createBuilder(USER).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsNoReadRole() {
      JsonView view = prepareView();
      Permission userPermission = new JsonPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      view.getPermissions().updateUserPermissions(userPermission);
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      Permission groupPermission = new JsonPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      view2.getPermissions().updateGroupPermissions(groupPermission);
      viewDao.databaseCollection().insertOne(view2);

      SearchQuery query = SearchQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).isEmpty();
   }

   @Test
   public void testGetViewsGroupRole() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      SearchQuery query = SearchQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsPagination() {
      JsonView view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      JsonView view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      SearchQuery query = SearchQuery.createBuilder(USER).page(1).pageSize(1).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetViewsByFulltext() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchQuery query = SearchQuery.createBuilder(USER).fulltext("text").build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2, CODE3);
   }

   @Test
   public void testGetViewsByCollections() {
      createView(CODE, NAME, new HashSet<>(Arrays.asList("c1", "c2", "c3")));
      createView(CODE2, NAME2, new HashSet<>(Arrays.asList("c2", "c3", "c4")));
      createView(CODE3, NAME3, new HashSet<>(Arrays.asList("c1", "c3")));

      SearchQuery query = SearchQuery.createBuilder(USER).collectionIds(new HashSet<>(Arrays.asList("c1", "c5"))).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE3);

      query = SearchQuery.createBuilder(USER).collectionIds(new HashSet<>(Arrays.asList("c2", "c4"))).build();
      views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);

      query = SearchQuery.createBuilder(USER).collectionIds(new HashSet<>(Arrays.asList("c4", "c5", "c6"))).build();
      views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetViewsByFulltextAndPagination() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchQuery query = SearchQuery.createBuilder(USER).fulltext("text").page(1).pageSize(1).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetViewsByFulltextDifferentUser() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchQuery query = SearchQuery.createBuilder(USER2).fulltext("text").build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).isEmpty();
   }

   @Test
   public void testGetViewsBySuggestionText() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SuggestionQuery query = SuggestionQuery.createBuilder(USER).text("test").build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsBySuggestionTextDifferentUser() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SuggestionQuery query = SuggestionQuery.createBuilder(USER2).text("test").build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).isEmpty();
   }

   @Test
   public void testGetAllCollectionsCodes() {
      assertThat(viewDao.getAllViewCodes()).isEmpty();

      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      assertThat(viewDao.getAllViewCodes()).contains(CODE, CODE2, CODE3);
   }

}
