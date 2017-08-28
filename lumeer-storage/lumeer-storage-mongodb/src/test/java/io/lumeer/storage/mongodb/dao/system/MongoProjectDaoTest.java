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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.DatabaseQuery;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoProject;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MongoProjectDaoTest extends MongoDbTestBase {

   private static final String ORGANIZATION_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USER = "testUser";
   private static final String USER2 = "testUser2";

   private static final String GROUP = "testGroup";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";
   private static final String NAME = "Testing project";
   private static final String COLOR = "#cccccc";
   private static final String ICON = "fa-search";
   private static final MongoPermissions PERMISSIONS;

   private static final MongoPermission GROUP_PERMISSION = new MongoPermission();

   static {
      MongoPermission userPermission = new MongoPermission();
      userPermission.setName(USER);
      userPermission.setRoles(Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));

      PERMISSIONS = new MongoPermissions();
      PERMISSIONS.updateUserPermissions(userPermission);

      GROUP_PERMISSION.setName(GROUP);
      GROUP_PERMISSION.setRoles(Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);
   }

   private MongoProjectDao projectDao;

   @Before
   public void initProjectDao() {
      Organization organization = Mockito.mock(Organization.class);
      Mockito.when(organization.getId()).thenReturn(ORGANIZATION_ID);

      projectDao = new MongoProjectDao();
      projectDao.setDatabase(database);
      projectDao.setDatastore(datastore);

      projectDao.ensureIndexes();
      projectDao.setOrganization(organization);
   }

   private MongoProject prepareProject(String code) {
      MongoProject project = new MongoProject();
      project.setCode(code);
      project.setName(NAME);
      project.setColor(COLOR);
      project.setIcon(ICON);
      project.setPermissions(new MongoPermissions(PERMISSIONS));
      return project;
   }

   @Test
   public void testCreateProject() {
      Project project = prepareProject(CODE1);

      String id = projectDao.createProject(project).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      MongoProject storedProject = datastore.get(projectDao.projectCollection(), MongoProject.class, new ObjectId(id));
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
      assertions.assertThat(storedProject.getPermissions()).isEqualTo(PERMISSIONS);
      assertions.assertAll();
   }

   @Test
   public void testCreateProjectExistingCode() {
      Project project = prepareProject(CODE1);
      datastore.save(projectDao.projectCollection(), project);

      Project project2 = prepareProject(CODE1);
      assertThatThrownBy(() -> projectDao.createProject(project2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testGetProjectByCode() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project);

      MongoProject storedProject = (MongoProject) projectDao.getProjectByCode(CODE1);
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getId()).isNotNull().isNotEmpty();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
      assertions.assertThat(storedProject.getPermissions()).isEqualTo(PERMISSIONS);
      assertions.assertAll();
   }

   @Test
   public void testGetProjectByCodeNotExisting() {
      assertThatThrownBy(() -> projectDao.getProjectByCode("notExistingCode"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasFieldOrPropertyWithValue("resourceType", ResourceType.PROJECT);
   }

   @Test
   public void testGetProjects() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project);

      MongoProject project2 = prepareProject(CODE2);
      project2.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project2);

      DatabaseQuery query = new DatabaseQuery.Builder(USER).build();
      List<MongoProject> projects = (List<MongoProject>)(List<?>) projectDao.getProjects(query);
      assertThat(projects).extracting(Project::getCode).containsOnly(CODE1, CODE2);
      assertThat(projects).extracting(MongoProject::getOrganizationId).containsOnly(ORGANIZATION_ID);
   }

   @Test
   public void testGetProjectsNoReadRole() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      Permission userPermission = new MongoPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      project.getPermissions().updateUserPermissions(userPermission);
      datastore.save(projectDao.projectCollection(), project);

      MongoProject project2 = prepareProject(CODE2);
      project.setOrganizationId(ORGANIZATION_ID);
      Permission groupPermission = new MongoPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      project2.getPermissions().updateGroupPermissions(groupPermission);
      datastore.save(projectDao.projectCollection(), project2);

      DatabaseQuery query = new DatabaseQuery.Builder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<Project> projects = projectDao.getProjects(query);
      assertThat(projects).isEmpty();
   }

   @Test
   public void testGetProjectsGroupRole() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project);

      MongoProject project2 = prepareProject(CODE2);
      project2.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project2);

      DatabaseQuery query = new DatabaseQuery.Builder(USER2).groups(Collections.singleton(GROUP)).build();
      List<MongoProject> projects = (List<MongoProject>)(List<?>) projectDao.getProjects(query);
      assertThat(projects).extracting(Project::getCode).containsOnly(CODE1, CODE2);
      assertThat(projects).extracting(MongoProject::getOrganizationId).containsOnly(ORGANIZATION_ID);
   }

   @Test
   public void testDeleteProject() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project);
      assertThat(project.getId()).isNotNull();

      projectDao.deleteProject(project.getId());

      Project storedProject = datastore.get(projectDao.projectCollection(), MongoProject.class, new ObjectId(project.getId()));
      assertThat(storedProject).isNull();
   }

   @Test
   public void testDeleteProjectNotExisting() {
      assertThatThrownBy(() -> projectDao.deleteProject(NOT_EXISTING_ID))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testUpdateProjectCode() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      String id = datastore.save(projectDao.projectCollection(), project).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      project.setCode(CODE2);
      projectDao.updateProject(id, project);

      MongoProject storedProject = datastore.get(projectDao.projectCollection(), MongoProject.class, new ObjectId(id));
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getCode()).isEqualTo(CODE2);
      assertThat(storedProject.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
   }

   @Test
   public void testUpdateProjectPermissions() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      String id = datastore.save(projectDao.projectCollection(), project).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      project.getPermissions().removeUserPermission(USER);
      project.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      projectDao.updateProject(id, project);

      MongoProject storedProject = datastore.get(projectDao.projectCollection(), MongoProject.class, new ObjectId(id));
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedProject.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateProjectExistingCode() {
      MongoProject project = prepareProject(CODE1);
      project.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project);

      MongoProject project2 = prepareProject(CODE2);
      project.setOrganizationId(ORGANIZATION_ID);
      datastore.save(projectDao.projectCollection(), project2);

      project2.setCode(CODE1);
      assertThatThrownBy(() -> projectDao.updateProject(project2.getId(), project2))
            .isInstanceOf(DuplicateKeyException.class);
   }

}
