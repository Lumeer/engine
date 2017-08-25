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

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaView;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;
import io.lumeer.storage.mongodb.model.embedded.MorphiaQuery;

import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MorphiaViewDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String COLOR = "#000000";
   private static final String ICON = "fa-eye";
   private static final MorphiaQuery QUERY = new MorphiaQuery();
   private static final Perspective PERSPECTIVE = Perspective.DOCUMENT_POSTIT;

   private static final MorphiaPermissions PERMISSIONS = new MorphiaPermissions();
   private static final MorphiaPermission USER_PERMISSION;
   private static final MorphiaPermission GROUP_PERMISSION;

   private static final String CODE2 = "TVIEW2";
   private static final String NAME2 = "Testing fulltext view";
   private static final String USER2 = "testUser2";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE3 = "FULLTEXT";
   private static final String NAME3 = "Just test view";

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      USER_PERMISSION = new MorphiaPermission(USER, View.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));
      PERMISSIONS.updateUserPermissions(USER_PERMISSION);

      GROUP_PERMISSION = new MorphiaPermission(GROUP, Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);

      QUERY.setPage(0);
   }

   private MorphiaViewDao viewDao;

   private Project project;

   @Before
   public void initViewDao() {
      project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      viewDao = new MorphiaViewDao();
      viewDao.setDatabase(database);
      viewDao.setDatastore(datastore);

      viewDao.setProject(project);
      viewDao.createViewsRepository(project);
      assertThat(database.listCollectionNames()).contains(viewDao.databaseCollection());
   }

   private MorphiaView prepareView() {
      MorphiaView view = new MorphiaView();
      view.setCode(CODE);
      view.setName(NAME);
      view.setColor(COLOR);
      view.setIcon(ICON);
      view.setPermissions(new MorphiaPermissions(PERMISSIONS));
      view.setQuery(QUERY);
      view.setPerspective(PERSPECTIVE.toString());
      return view;
   }

   private MorphiaView createView(String code, String name) {
      MorphiaView morphiaView = prepareView();
      morphiaView.setCode(code);
      morphiaView.setName(name);

      datastore.insert(viewDao.databaseCollection(), morphiaView);
      return morphiaView;
   }

   @Test
   public void testDeleteViewsRepository() {
      viewDao.deleteViewsRepository(project);
      assertThat(database.listCollectionNames()).doesNotContain(viewDao.databaseCollection());
   }

   @Test
   public void testCreateView() {
      MorphiaView view = prepareView();

      String id = viewDao.createView(view).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      MorphiaView storedView = datastore.get(viewDao.databaseCollection(), MorphiaView.class, new ObjectId(id));
      assertThat(storedView).isNotNull();
      assertThat(storedView.getCode()).isEqualTo(CODE);
      assertThat(storedView.getName()).isEqualTo(NAME);
      assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertThat(storedView.getPermissions()).isEqualTo(PERMISSIONS);
      assertThat(storedView.getQuery()).isEqualTo(QUERY);
      assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
   }

   @Test
   public void testCreateViewExistingCode() {
      MorphiaView view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);

      View view2 = prepareView();
      assertThatThrownBy(() -> viewDao.createView(view2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testUpdateViewCode() {
      MorphiaView view = prepareView();
      String id = datastore.save(viewDao.databaseCollection(), view).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      view.setCode(CODE2);
      viewDao.updateView(id, view);

      MorphiaView storedView = datastore.get(viewDao.databaseCollection(), MorphiaView.class, new ObjectId(id));
      assertThat(storedView).isNotNull();
      assertThat(storedView.getCode()).isEqualTo(CODE2);
   }

   @Test
   public void testUpdateViewPermissions() {
      MorphiaView view = prepareView();
      String id = datastore.save(viewDao.databaseCollection(), view).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      view.getPermissions().removeUserPermission(USER);
      view.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      viewDao.updateView(id, view);

      MorphiaView storedView = datastore.get(viewDao.databaseCollection(), MorphiaView.class, new ObjectId(id));
      assertThat(storedView).isNotNull();
      assertThat(storedView.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedView.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateViewExistingCode() {
      MorphiaView view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);

      MorphiaView view2 = prepareView();
      view2.setCode(CODE2);
      datastore.save(viewDao.databaseCollection(), view2);

      view2.setCode(CODE);
      assertThatThrownBy(() -> viewDao.updateView(view2.getId(), view2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   @Ignore("Stored anyway with the current implementation")
   public void testUpdateViewNotExisting() {

   }

   @Test
   public void testDeleteView() {
      View view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);
      assertThat(view.getId()).isNotNull();

      viewDao.deleteView(view.getId());

      View storedView = datastore.get(viewDao.databaseCollection(), MorphiaView.class, new ObjectId(view.getId()));
      assertThat(storedView).isNull();
   }

   @Test
   public void testDeleteViewNotExisting() {
      assertThatThrownBy(() -> viewDao.deleteView(NOT_EXISTING_ID))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testGetViewByCode() {
      View view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);

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
      MorphiaView view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);

      MorphiaView view2 = prepareView();
      view2.setCode(CODE2);
      datastore.save(viewDao.databaseCollection(), view2);

      SearchQuery query = SearchQuery.createBuilder(USER).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsNoReadRole() {
      MorphiaView view = prepareView();
      Permission userPermission = new MorphiaPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      view.getPermissions().updateUserPermissions(userPermission);
      datastore.save(viewDao.databaseCollection(), view);

      MorphiaView view2 = prepareView();
      view2.setCode(CODE2);
      Permission groupPermission = new MorphiaPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      view2.getPermissions().updateGroupPermissions(groupPermission);
      datastore.save(viewDao.databaseCollection(), view2);

      SearchQuery query = SearchQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).isEmpty();
   }

   @Test
   public void testGetViewsGroupRole() {
      MorphiaView view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);

      MorphiaView view2 = prepareView();
      view2.setCode(CODE2);
      datastore.save(viewDao.databaseCollection(), view2);

      SearchQuery query = SearchQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewsPagination() {
      MorphiaView view = prepareView();
      datastore.save(viewDao.databaseCollection(), view);

      MorphiaView view2 = prepareView();
      view2.setCode(CODE2);
      datastore.save(viewDao.databaseCollection(), view2);

      SearchQuery query = SearchQuery.createBuilder(USER).page(1).pageSize(1).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2);
   }

   @Test
   public void testGetViewsByFulltext() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchQuery query = SearchQuery.createBuilder(USER).fulltext("fulltext").build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE2, CODE3);
   }

   @Test
   public void testGetViewsByFulltextAndPagination() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchQuery query = SearchQuery.createBuilder(USER).fulltext("fulltext").page(1).pageSize(1).build();
      List<View> views = viewDao.getViews(query);
      assertThat(views).extracting(View::getCode).containsOnly(CODE3);
   }

   @Test
   public void testGetViewsByFulltextDifferentUser() {
      createView(CODE, NAME);
      createView(CODE2, NAME2);
      createView(CODE3, NAME3);

      SearchQuery query = SearchQuery.createBuilder(USER2).fulltext("fulltext").build();
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

}
