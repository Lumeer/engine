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
package io.lumeer.storage.mongodb.dao.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.model.MongoProject;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;

public class MongoProjectDaoTest extends MongoDbTestBase {

   private static final String USER = "testUser";

   private static final String CODE = "TPROJ";
   private static final String NAME = "Testing project";
   private static final String COLOR = "#cccccc";
   private static final String ICON = "fa-search";
   private static final MongoPermissions PERMISSIONS;

   static {
      MongoPermission userPermission = new MongoPermission();
      userPermission.setName(USER);
      userPermission.setRoles(Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));

      PERMISSIONS = new MongoPermissions();
      PERMISSIONS.updateUserPermissions(userPermission);
   }

   private MongoProjectDao projectDao;

   @Before
   public void initProjectDao() {
      projectDao = new MongoProjectDao();
      projectDao.setDatabase(database);
      projectDao.setDatastore(datastore);

      projectDao.ensureIndexes();
   }

   private MongoProject prepareProject() {
      MongoProject project = new MongoProject();
      project.setCode(CODE);
      project.setName(NAME);
      project.setColor(COLOR);
      project.setIcon(ICON);
      project.setPermissions(new MongoPermissions(PERMISSIONS));
      return project;
   }

   @Test
   public void testCreateProject() {
      Project project = prepareProject();

      String id = projectDao.createProject(project).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Project storedProject = datastore.get(MongoProject.class, new ObjectId(id));
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions()).isEqualTo(PERMISSIONS);
      assertions.assertAll();
   }

   @Test
   public void testCreateProjectExistingCode() {
      Project project = prepareProject();
      datastore.save(project);

      Project project2 = prepareProject();
      assertThatThrownBy(() -> projectDao.createProject(project2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testGetProjectByCode() {
      Project project = prepareProject();
      datastore.save(project);

      Project storedProject = projectDao.getProjectByCode(CODE);
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getId()).isNotNull().isNotEmpty();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions()).isEqualTo(PERMISSIONS);
      assertions.assertAll();
   }

   @Test
   public void testGetProjectByCodeNotExisting() {
      assertThatThrownBy(() -> projectDao.getProjectByCode("notExistingCode"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasFieldOrPropertyWithValue("resourceType", ResourceType.PROJECT);
   }

}
