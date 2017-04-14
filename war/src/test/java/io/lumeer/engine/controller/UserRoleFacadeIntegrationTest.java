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

import static io.lumeer.engine.api.LumeerConst.UserRoles.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Tests for UserRoleFacade
 */
@RunWith(Arquillian.class)
public class UserRoleFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private UserRoleFacade userRoleFacade;

   @Before
   public void setUp() throws Exception {
      systemDataStorage.dropManyDocuments(COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void projectUserRolesTest() throws Exception {

      final String organization = "oganization1";
      final String project1 = "project1";
      final String project2 = "project2";

      final String userRole1 = "userRole1";
      final String userRole2 = "userRole2";
      final String userRole3 = "userRole3";

      final String coreRole1 = "coreRole1";
      final String coreRole2 = "coreRole2";
      final String coreRole3 = "coreRole3";

      Map<String, List<String>> rolesProject1 = userRoleFacade.readRoles(organization, project1);
      Map<String, List<String>> rolesProject2 = userRoleFacade.readRoles(organization, project2);
      assertThat(rolesProject1).isEmpty();
      assertThat(rolesProject2).isEmpty();

      // create test
      userRoleFacade.createRole(organization, project1, userRole1, Arrays.asList(coreRole1, coreRole2));
      userRoleFacade.createRole(organization, project1, userRole2, Arrays.asList(coreRole2, coreRole3));
      userRoleFacade.createRole(organization, project2, userRole2, Arrays.asList(coreRole2, coreRole3));
      userRoleFacade.createRole(organization, project2, userRole3, Collections.singletonList(coreRole3));
      rolesProject1 = userRoleFacade.readRoles(organization, project1);
      rolesProject2 = userRoleFacade.readRoles(organization, project2);
      assertThat(rolesProject1).containsOnlyKeys(userRole1, userRole2);
      assertThat(rolesProject1.get(userRole1)).containsExactly(coreRole1, coreRole2);
      assertThat(rolesProject1.get(userRole2)).containsExactly(coreRole2, coreRole3);
      assertThat(rolesProject2).containsOnlyKeys(userRole2, userRole3);
      assertThat(rolesProject2.get(userRole2)).containsExactly(coreRole2, coreRole3);
      assertThat(rolesProject2.get(userRole3)).containsExactly(coreRole3);

      // drop test
      userRoleFacade.dropRole(organization, project1, userRole2);
      userRoleFacade.dropRole(organization, project2, userRole2);
      rolesProject1 = userRoleFacade.readRoles(organization, project1);
      rolesProject2 = userRoleFacade.readRoles(organization, project2);
      assertThat(rolesProject1).containsOnlyKeys(userRole1);
      assertThat(rolesProject2).containsOnlyKeys(userRole3);

      // add and delete core roles test
      userRoleFacade.addCoreRolesToRole(organization, project1, userRole1, Collections.singletonList(coreRole3));
      rolesProject1 = userRoleFacade.readRoles(organization, project1);
      assertThat(rolesProject1.get(userRole1)).containsExactly(coreRole1, coreRole2, coreRole3);
      userRoleFacade.removeCoreRolesFromRole(organization, project1, userRole1, Arrays.asList(coreRole1, coreRole2));
      rolesProject1 = userRoleFacade.readRoles(organization, project1);
      assertThat(rolesProject1.get(userRole1)).containsExactly(coreRole3);
   }

   @Test
   public void organizationUserRolesTest() throws Exception {
      final String organization1 = "organization11";
      final String organization2 = "organization22";

      final String userRole1 = "userRole11";
      final String userRole2 = "userRole22";
      final String userRole3 = "userRole33";

      final String coreRole1 = "coreRole11";
      final String coreRole2 = "coreRole22";
      final String coreRole3 = "coreRole33";

      Map<String, List<String>> rolesOrganization1 = userRoleFacade.readRoles(organization1);
      Map<String, List<String>> rolesOrganization2 = userRoleFacade.readRoles(organization2);
      assertThat(rolesOrganization1).isEmpty();
      assertThat(rolesOrganization2).isEmpty();

      // create test
      userRoleFacade.createRole(organization1, userRole1, Arrays.asList(coreRole1, coreRole2));
      userRoleFacade.createRole(organization1, userRole2, Arrays.asList(coreRole2, coreRole3));
      userRoleFacade.createRole(organization2, userRole2, Arrays.asList(coreRole2, coreRole3));
      userRoleFacade.createRole(organization2, userRole3, Collections.singletonList(coreRole3));
      rolesOrganization1 = userRoleFacade.readRoles(organization1);
      rolesOrganization2 = userRoleFacade.readRoles(organization2);
      assertThat(rolesOrganization1).containsOnlyKeys(userRole1, userRole2);
      assertThat(rolesOrganization1.get(userRole1)).containsExactly(coreRole1, coreRole2);
      assertThat(rolesOrganization1.get(userRole2)).containsExactly(coreRole2, coreRole3);
      assertThat(rolesOrganization2).containsOnlyKeys(userRole2, userRole3);
      assertThat(rolesOrganization2.get(userRole2)).containsExactly(coreRole2, coreRole3);
      assertThat(rolesOrganization2.get(userRole3)).containsExactly(coreRole3);

      // drop test
      userRoleFacade.dropRole(organization1, userRole2);
      userRoleFacade.dropRole(organization2, userRole2);
      rolesOrganization1 = userRoleFacade.readRoles(organization1);
      rolesOrganization2 = userRoleFacade.readRoles(organization2);
      assertThat(rolesOrganization1).containsOnlyKeys(userRole1);
      assertThat(rolesOrganization2).containsOnlyKeys(userRole3);

      // add and delete core roles test
      userRoleFacade.addCoreRolesToRole(organization1, userRole1, Collections.singletonList(coreRole3));
      rolesOrganization1 = userRoleFacade.readRoles(organization1);
      assertThat(rolesOrganization1.get(userRole1)).containsExactly(coreRole1, coreRole2, coreRole3);
      userRoleFacade.removeCoreRolesFromRole(organization1, userRole1, Arrays.asList(coreRole1, coreRole2));
      rolesOrganization1 = userRoleFacade.readRoles(organization1);
      assertThat(rolesOrganization1.get(userRole1)).containsExactly(coreRole3);

   }

   @Test
   public void organizationAndProjectUserRolesTest() throws Exception {
      final String organization = "organization111";
      final String project = "project111";

      final String userRole = "userRole111";

      final String coreRole1 = "coreRole111";
      final String coreRole2 = "coreRole222";
      final String coreRole3 = "coreRole333";
      final String coreRole4 = "coreRole444";
      final String coreRole5 = "coreRole555";

      Map<String, List<String>> rolesOrganization = userRoleFacade.readRoles(organization);
      Map<String, List<String>> rolesProject = userRoleFacade.readRoles(organization, project);
      assertThat(rolesOrganization).isEmpty();
      assertThat(rolesProject).isEmpty();

      //creating same role in project and organization test
      userRoleFacade.createRole(organization, userRole, Arrays.asList(coreRole1, coreRole2, coreRole3));
      userRoleFacade.createRole(organization, project, userRole, Arrays.asList(coreRole1, coreRole2, coreRole3));
      rolesOrganization = userRoleFacade.readRoles(organization);
      rolesProject = userRoleFacade.readRoles(organization, project);
      assertThat(rolesOrganization).containsOnlyKeys(userRole);
      assertThat(rolesProject).containsOnlyKeys(userRole);
      assertThat(rolesOrganization.get(userRole)).containsExactly(coreRole1, coreRole2, coreRole3);
      assertThat(rolesProject.get(userRole)).containsExactly(coreRole1, coreRole2, coreRole3);

      //creating in same role in project and organization test
      userRoleFacade.addCoreRolesToRole(organization, userRole, Collections.singletonList(coreRole4));
      userRoleFacade.addCoreRolesToRole(organization, project, userRole, Collections.singletonList(coreRole5));
      rolesOrganization = userRoleFacade.readRoles(organization);
      rolesProject = userRoleFacade.readRoles(organization, project);
      assertThat(rolesOrganization.get(userRole)).containsExactly(coreRole1, coreRole2, coreRole3, coreRole4);
      assertThat(rolesProject.get(userRole)).containsExactly(coreRole1, coreRole2, coreRole3, coreRole5);

      //removing from same role in project and organization test
      userRoleFacade.removeCoreRolesFromRole(organization, userRole, Arrays.asList(coreRole1, coreRole2));
      userRoleFacade.removeCoreRolesFromRole(organization, project, userRole, Collections.singletonList(coreRole3));
      rolesOrganization = userRoleFacade.readRoles(organization);
      rolesProject = userRoleFacade.readRoles(organization, project);
      assertThat(rolesOrganization.get(userRole)).containsExactly(coreRole3, coreRole4);
      assertThat(rolesProject.get(userRole)).containsExactly(coreRole1, coreRole2, coreRole5);

      //dropping same role in project and organization test
      userRoleFacade.dropRole(organization, userRole);
      rolesOrganization = userRoleFacade.readRoles(organization);
      rolesProject = userRoleFacade.readRoles(organization, project);
      assertThat(rolesOrganization).isEmpty();
      assertThat(rolesProject).containsOnlyKeys(userRole);

      userRoleFacade.dropRole(organization, project, userRole);
      rolesProject = userRoleFacade.readRoles(organization, project);
      assertThat(rolesProject).isEmpty();
   }

}
