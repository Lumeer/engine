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

import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoView;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;
import io.lumeer.storage.mongodb.model.embedded.MongoQuery;

import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MongoViewDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String COLOR = "#000000";
   private static final String ICON = "fa-eye";
   private static final MongoQuery QUERY = new MongoQuery();
   private static final Perspective PERSPECTIVE = Perspective.DOCUMENT_POSTIT;

   private static final MongoPermissions PERMISSIONS = new MongoPermissions();
   private static final MongoPermission USER_PERMISSION = new MongoPermission();

   private static final MongoPermission GROUP_PERMISSION = new MongoPermission();

   private static final String CODE2 = "TVIEW2";

   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      USER_PERMISSION.setName(USER);
      USER_PERMISSION.setRoles(View.ROLES.stream()
                                         .map(Role::toString)
                                         .collect(Collectors.toSet()));
      PERMISSIONS.updateUserPermissions(USER_PERMISSION);

      GROUP_PERMISSION.setName(GROUP);
      GROUP_PERMISSION.setRoles(Collections.singleton(Role.READ.toString()));

      QUERY.setPage(0);
   }

   private MongoViewDao viewDao;

   @Before
   public void initViewDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      viewDao = new MongoViewDao();
      viewDao.setDatabase(database);
      viewDao.setDatastore(datastore);

      viewDao.setProject(project);
      viewDao.ensureIndexes();
   }

   private MongoView prepareView() {
      MongoView view = new MongoView();
      view.setCode(CODE);
      view.setName(NAME);
      view.setColor(COLOR);
      view.setIcon(ICON);
      view.setPermissions(PERMISSIONS);
      view.setQuery(QUERY);
      view.setPerspective(PERSPECTIVE.toString());
      return view;
   }

   @Test
   public void testCreateView() {
      MongoView view = prepareView();

      String id = viewDao.createView(view).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      MongoView storedView = datastore.get(viewDao.viewCollection(), MongoView.class, new ObjectId(id));
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
      MongoView view = prepareView();
      datastore.save(viewDao.viewCollection(), view);

      View view2 = prepareView();
      assertThatThrownBy(() -> viewDao.createView(view2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testUpdateViewCode() {
      MongoView view = prepareView();
      String id = datastore.save(viewDao.viewCollection(), view).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      view.setCode(CODE2);
      viewDao.updateView(id, view);

      MongoView storedView = datastore.get(viewDao.viewCollection(), MongoView.class, new ObjectId(id));
      assertThat(storedView).isNotNull();
      assertThat(storedView.getCode()).isEqualTo(CODE2);
   }

   @Test
   public void testUpdateViewPermissions() {
      MongoView view = prepareView();
      String id = datastore.save(viewDao.viewCollection(), view).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      view.getPermissions().removeUserPermission(USER);
      view.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      viewDao.updateView(id, view);

      MongoView storedView = datastore.get(viewDao.viewCollection(), MongoView.class, new ObjectId(id));
      assertThat(storedView).isNotNull();
      assertThat(storedView.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedView.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateViewExistingCode() {
      MongoView view = prepareView();
      datastore.save(viewDao.viewCollection(), view);

      MongoView view2 = prepareView();
      view2.setCode(CODE2);
      datastore.save(viewDao.viewCollection(), view2);

      view2.setCode(CODE);
      assertThatThrownBy(() -> viewDao.updateView(view2.getId(), view2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   @Ignore("Stored anyway in the current implementation")
   public void testUpdateViewNotExisting() {

   }

   @Test
   public void testDeleteView() {
      View view = prepareView();
      datastore.save(viewDao.viewCollection(), view);
      assertThat(view.getId()).isNotNull();

      viewDao.deleteView(view.getId());

      View storedView = datastore.get(viewDao.viewCollection(), MongoView.class, new ObjectId(view.getId()));
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
      datastore.save(viewDao.viewCollection(), view);

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
   public void testGetAllViews() {
      MongoView view = prepareView();
      datastore.save(viewDao.viewCollection(), view);

      MongoView view2 = prepareView();
      view2.setCode(CODE2);
      datastore.save(viewDao.viewCollection(), view2);

      List<View> views = viewDao.getAllViews();
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetAllViewsEmpty() {
      List<View> views = viewDao.getAllViews();
      assertThat(views).isEmpty();
   }

}
