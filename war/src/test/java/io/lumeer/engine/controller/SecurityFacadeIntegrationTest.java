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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.Role;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

   @Inject
   private UserGroupFacade userGroupFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   @Inject
   private CollectionFacade collectionFacade;

   private String user;
   private String org;
   private String project;
   private String roleManage;
   private String group1;
   private String group2;

   @Before
   public void setUp() throws Exception {
      sysDataStorage.dropManyDocuments(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropManyDocuments(LumeerConst.Security.ROLES_COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));

      user = userFacade.getUserEmail();
      org = organizationFacade.getOrganizationCode();
      project = projectFacade.getCurrentProjectCode();
      roleManage = LumeerConst.Security.ROLE_MANAGE;
      group1 = "group 1";
      group2 = "group 2";

      organizationFacade.dropOrganization(org);
      organizationFacade.createOrganization(new Organization(org, "name"));

      userGroupFacade.addGroups(org, group1, group2);
      userGroupFacade.addUser(org, user, group1, group2);

      projectFacade.dropProject(project);
      projectFacade.createProject(new Project(project, "project name"));
   }

   @Test
   public void addAndRemoveOrganizationUserRole() throws Exception {
      securityFacade.addOrganizationUsersRole(org, Collections.singletonList(user), roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isTrue();

      List<Role> roles = securityFacade.getOrganizationRoles(org);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).toArray(new String[LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getUsers).contains(new ArrayList<String>() {{
         add(user);
      }});

      securityFacade.removeOrganizationUsersRole(org, Collections.singletonList(user), roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isFalse();

      roles = securityFacade.getOrganizationRoles(org);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).toArray(new String[LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getUsers).doesNotContain(new ArrayList<String>() {{
         add(user);
      }});

   }

   @Test
   public void addAndRemoveOrganizationGroupRole() throws Exception {
      // we have to remove default role
      securityFacade.removeOrganizationUsersRole(org, Collections.singletonList(user), roleManage);

      // one group has role
      securityFacade.addOrganizationGroupsRole(org, Collections.singletonList(group1), roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isTrue();

      List<Role> roles = securityFacade.getOrganizationRoles(org);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).toArray(new String[LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).contains(new ArrayList<String>() {{
         add(group1);
      }});
      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).doesNotContain(new ArrayList<String>() {{
         add(group2);
      }});

      // both groups have role
      securityFacade.addOrganizationGroupsRole(org, Collections.singletonList(group2), roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isTrue();

      roles = securityFacade.getOrganizationRoles(org);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).toArray(new String[LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).contains(new ArrayList<String>() {{
         add(group1);
         add(group2);
      }});

      // again, one group has role
      securityFacade.removeOrganizationGroupsRole(org, Collections.singletonList(group1), roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isTrue();

      roles = securityFacade.getOrganizationRoles(org);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).toArray(new String[LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).contains(new ArrayList<String>() {{
         add(group2);
      }});
      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).doesNotContain(new ArrayList<String>() {{
         add(group1);
      }});

      // no group has role
      securityFacade.removeOrganizationGroupsRole(org, Collections.singletonList(group2), roleManage);
      assertThat(securityFacade.hasOrganizationRole(org, roleManage)).isFalse();

      roles = securityFacade.getOrganizationRoles(org);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).toArray(new String[LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.ORGANIZATION_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).doesNotContain(new ArrayList<String>() {{
         add(group1);
         add(group2);
      }});
   }

   @Test
   public void addAndRemoveProjectUserRole() throws Exception {
      securityFacade.addProjectUsersRole(project, Collections.singletonList(user), roleManage);
      assertThat(securityFacade.hasProjectRole(project, roleManage)).isTrue();

      List<Role> roles = securityFacade.getProjectRoles(project);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.PROJECT_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.PROJECT_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getUsers).contains(new ArrayList<String>() {{
         add(user);
      }});

      securityFacade.removeProjectUsersRole(project, Collections.singletonList(user), roleManage);
      assertThat(securityFacade.hasProjectRole(project, roleManage)).isFalse();

      roles = securityFacade.getProjectRoles(project);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.PROJECT_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.PROJECT_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getUsers).doesNotContain(new ArrayList<String>() {{
         add(user);
      }});
   }

   @Test
   public void addAndRemoveProjectGroupRole() throws Exception {
      // we have to remove default role
      securityFacade.removeProjectUsersRole(project, Collections.singletonList(user), roleManage);

      securityFacade.addProjectGroupsRole(project, Collections.singletonList(group1), roleManage);
      assertThat(securityFacade.hasProjectRole(project, roleManage)).isTrue();

      List<Role> roles = securityFacade.getProjectRoles(project);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.PROJECT_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.PROJECT_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).contains(new ArrayList<String>() {{
         add(group1);
      }});

      securityFacade.removeProjectGroupsRole(project, Collections.singletonList(group1), roleManage);
      assertThat(securityFacade.hasProjectRole(project, roleManage)).isFalse();

      roles = securityFacade.getProjectRoles(project);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.PROJECT_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.PROJECT_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).doesNotContain(new ArrayList<String>() {{
         add(group1);
      }});
   }

   @Test
   public void addAndRemoveViewUserRole() throws Exception {
      int viewId = 1;
      databaseInitializer.onViewCreated(project, viewId);

      securityFacade.addViewUsersRole(project, viewId, Collections.singletonList(user), roleManage);
      assertThat(securityFacade.hasViewRole(project, viewId, roleManage)).isTrue();

      List<Role> roles = securityFacade.getViewRoles(project, viewId);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.VIEW_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.VIEW_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getUsers).contains(new ArrayList<String>() {{
         add(user);
      }});

      securityFacade.removeViewUsersRole(project, viewId, Collections.singletonList(user), roleManage);
      assertThat(securityFacade.hasViewRole(project, viewId, roleManage)).isFalse();

      roles = securityFacade.getViewRoles(project, viewId);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.VIEW_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.VIEW_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getUsers).doesNotContain(new ArrayList<String>() {{
         add(user);
      }});
   }

   @Test
   public void addAndRemoveViewGroupRole() throws Exception {
      int viewId = 1;
      databaseInitializer.onViewCreated(project, viewId);

      securityFacade.addViewGroupsRole(project, viewId, Collections.singletonList(group1), roleManage);
      assertThat(securityFacade.hasViewRole(project, viewId, roleManage)).isTrue();

      List<Role> roles = securityFacade.getViewRoles(project, viewId);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.VIEW_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.VIEW_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).contains(new ArrayList<String>() {{
         add(group1);
      }});

      securityFacade.removeViewGroupsRole(project, viewId, Collections.singletonList(group1), roleManage);
      assertThat(securityFacade.hasViewRole(project, viewId, roleManage)).isFalse();

      roles = securityFacade.getViewRoles(project, viewId);
      assertThat(roles).extracting(Role::getName)
                       .containsOnly(LumeerConst.Security.RESOURCE_ROLES.get(
                             LumeerConst.Security.VIEW_RESOURCE).toArray(
                             new String[LumeerConst.Security.RESOURCE_ROLES.get(
                                   LumeerConst.Security.VIEW_RESOURCE).size()]));

      assertThat(roles).filteredOn(role -> role.getName().equals(roleManage))
                       .extracting(Role::getGroups).doesNotContain(new ArrayList<String>() {{
         add(group1);
      }});
   }
}