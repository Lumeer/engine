/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * @author <a href="mailto:alica.kacengova@gmail.com>Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class SecurityFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   @SystemDataStorage
   private DataStorage sysDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   // we use methods for roles initialization just for test purposes, so we do not have to
   // create organizations, projects, collections and views here
   @Inject
   private DatabaseInitializer databaseInitializer;

   private String user;
   private String org;
   private String project;
   private String roleManage;

   @Before
   public void setUp() throws Exception {
      sysDataStorage.dropManyDocuments(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropManyDocuments(LumeerConst.Security.ROLES_COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      user = userFacade.getUserEmail();
      org = organizationFacade.getOrganizationId();
      project = projectFacade.getCurrentProjectId();
      roleManage = LumeerConst.Security.ROLE_MANAGE;
   }

   @Test
   public void addAndRemoveOrganizationUserRole() throws Exception {
      databaseInitializer.initializeOrganizationRoles(org);

      securityFacade.addOrganizationUserRole(org, user, roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isTrue();

      securityFacade.removeOrganizationUserRole(org, user, roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isFalse();
   }

   @Test
   public void addAndRemoveOrganizationGroupRole() throws Exception {

   }

   @Test
   public void addAndRemoveProjectUserRole() throws Exception {
      databaseInitializer.initializeProjectRoles(project);

      securityFacade.addProjectUserRole(project, user, roleManage);
      assertThat(securityFacade.hasProjectRole(project, roleManage)).isTrue();

      securityFacade.removeProjectUserRole(project, user, roleManage);
      assertThat(securityFacade.hasProjectRole(project, roleManage)).isFalse();
   }

   @Test
   public void addAndRemoveProjectGroupRole() throws Exception {

   }

   @Test
   public void addAndRemoveCollectionUserRole() throws Exception {
      String collection = "test collection";
      databaseInitializer.initializeCollectionRoles(project, collection);

      securityFacade.addCollectionUserRole(project, collection, user, roleManage);
      assertThat(securityFacade.hasCollectionRole(project, collection, roleManage)).isTrue();

      securityFacade.removeCollectionUserRole(project, collection, user, roleManage);
      assertThat(securityFacade.hasCollectionRole(project, collection, roleManage)).isFalse();
   }

   @Test
   public void addAndRemoveCollectionGroupRole() throws Exception {

   }

   @Test
   public void addAndRemoveViewUserRole() throws Exception {
      int viewId = 1;
      databaseInitializer.initializeViewRoles(project, viewId);

      securityFacade.addViewUserRole(project, viewId, user, roleManage);
      assertThat(securityFacade.hasViewRole(project, viewId, roleManage)).isTrue();

      securityFacade.removeViewUserRole(project, viewId, user, roleManage);
      assertThat(securityFacade.hasViewRole(project, viewId, roleManage)).isFalse();
   }

   @Test
   public void addAndRemoveViewGroupRole() throws Exception {

   }
}