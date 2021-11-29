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

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchSuggestionQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class MongoViewDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String COLOR = "#000000";
   private static final String ICON = "fa-eye";
   private static final Query QUERY = new Query();
   private static final Perspective PERSPECTIVE = Perspective.Kanban;
   private static final Object CONFIG = "configuration object";

   private static final Permissions PERMISSIONS = new Permissions();
   private static final Permission USER_PERMISSION;
   private static final Permission GROUP_PERMISSION;

   private static final String CODE2 = "TVIEW2";
   private static final String NAME2 = "Testing fulltext view";
   private static final String USER2 = "testUser2";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE3 = "FULLTEXT";
   private static final String NAME3 = "Just FULLTEXT view";

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      USER_PERMISSION = new Permission(USER, View.ROLES);
      PERMISSIONS.updateUserPermissions(USER_PERMISSION);

      GROUP_PERMISSION = new Permission(GROUP, Collections.singleton(new Role(RoleType.Read)));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);
   }

   private MongoViewDao viewDao;

   private MongoLinkTypeDao linkTypeDao;

   private Project project;

   @Before
   public void initViewDao() {
      project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      viewDao = new MongoViewDao();
      viewDao.setDatabase(database);

      viewDao.setProject(project);
      viewDao.createRepository(project);
      assertThat(database.listCollectionNames()).contains(viewDao.databaseCollectionName());

      linkTypeDao = new MongoLinkTypeDao();
      linkTypeDao.setDatabase(database);
      linkTypeDao.setProject(project);
      linkTypeDao.createRepository(project);

      assertThat(database.listCollectionNames()).contains(linkTypeDao.databaseCollectionName());
   }

   private View prepareView() {
      View view = new View();
      view.setCode(CODE);
      view.setName(NAME);
      view.setColor(COLOR);
      view.setIcon(ICON);
      view.setPermissions(new Permissions(PERMISSIONS));
      view.setQuery(QUERY);
      view.setPerspective(PERSPECTIVE);
      view.setConfig(CONFIG);
      return view;
   }

   private View createView(String code, String name) {
      View jsonView = prepareView();
      jsonView.setCode(code);
      jsonView.setName(name);

      viewDao.databaseCollection().insertOne(jsonView);
      return jsonView;
   }

   @Test
   public void testDeleteViewsRepository() {
      viewDao.deleteRepository(project);
      assertThat(database.listCollectionNames()).doesNotContain(viewDao.databaseCollectionName());
   }

   @Test
   public void testCreateView() {
      View view = prepareView();

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
      View view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      View view2 = prepareView();
      view2.setName(NAME2);
      assertThatThrownBy(() -> viewDao.createView(view2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateViewCode() {
      View view = prepareView();
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
      View view = prepareView();
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
      View view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      View view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      view2.setCode(CODE);
      assertThatThrownBy(() -> viewDao.updateView(view2.getId(), view2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   @Ignore("Stored anyway with the current implementation")
   public void testUpdateViewNotExisting() {

   }

   @Test
   public void testDeleteView() {
      View view = prepareView();
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
      View view = prepareView();
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
      View view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      View view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsNoReadRole() {
      View view = prepareView();
      Permission userPermission = new Permission(USER2, Collections.singleton(new Role(RoleType.Manage)));
      view.getPermissions().updateUserPermissions(userPermission);
      viewDao.databaseCollection().insertOne(view);

      View view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      Permission groupPermission = new Permission(GROUP2, Collections.singleton(new Role(RoleType.UserConfig)));
      view2.getPermissions().updateGroupPermissions(groupPermission);
      viewDao.databaseCollection().insertOne(view2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).isEmpty();
   }

   @Test
   public void testGetViewsGroupRole() {
      View view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      View view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsPagination() {
      View view = prepareView();
      viewDao.databaseCollection().insertOne(view);

      View view2 = prepareView();
      view2.setCode(CODE2);
      view2.setName(NAME2);
      viewDao.databaseCollection().insertOne(view2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER).page(1).pageSize(1).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetViewsBySuggestionText() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchSuggestionQuery query = SearchSuggestionQuery.createBuilder(USER).text("test").build();
      List<View> views = viewDao.getViews(query, false);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsBySuggestionTextDifferentUser() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchSuggestionQuery query = SearchSuggestionQuery.createBuilder(USER2).text("test").build();
      List<View> views = viewDao.getViews(query, false);
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
