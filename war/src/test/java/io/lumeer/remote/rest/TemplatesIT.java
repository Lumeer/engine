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
package io.lumeer.remote.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.SelectionList;
import io.lumeer.api.model.User;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.dao.SequenceDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class TemplatesIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String ORGANIZATION_CODE = "TORG";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";
   private static final String CODE3 = "TPROJ3";
   private static final String CODE4 = "TPROJ4";

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Set<Role> USER_ROLES = Project.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(new Role(RoleType.Read));
   private Permission userPermission;
   private Permission groupPermission;

   private User user;

   private String projectUrl;

   private Organization organization;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private SequenceDao sequenceDao;

   @Inject
   private SelectionListDao selectionListDao;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      userPermission = Permission.buildWithRoles(this.user.getId(), USER_ROLES);
      groupPermission = Permission.buildWithRoles(GROUP, GROUP_ROLES);

      organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      organization.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Organization.ROLES));
      organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(organization);

      projectUrl = organizationPath(organization) + "projects/";

      PermissionCheckerUtil.allowGroups();
   }

   private Project createProject(String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      project.getPermissions().updateUserPermissions(userPermission);
      project.getPermissions().updateGroupPermissions(groupPermission);
      return projectDao.createProject(project);
   }

   @Test
   public void testTemplatesImportExport() throws InterruptedException {
      var p1 = createProject(CODE1);
      var p2= createProject(CODE2);

      byte[] templateContent = new byte[0];

      try (
         final InputStream input = TemplatesIT.class.getResourceAsStream("/test.json")
      ) {
         templateContent = input.readAllBytes();
      } catch (IOException | NullPointerException e) {
         Assert.fail("Could not read test template.");
      }

      Entity entity = Entity.json(templateContent);
      Response response = client.target(projectUrl + p1.getId() + "/raw")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Thread.sleep(500); // allow functions to be executed

      response = client.target(projectUrl + p1.getId() + "/raw")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      var exportedTemplate = response.readEntity(String.class);

      entity = Entity.json(exportedTemplate);
      response = client.target(projectUrl + p2.getId() + "/raw")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Thread.sleep(500); // allow functions to be executed

      workspaceKeeper.setOrganization(organization);
      workspaceKeeper.setProject(p2);
      projectDao.switchOrganization();
      collectionDao.setProject(p2);
      linkTypeDao.setProject(p2);
      sequenceDao.setProject(p2);
      selectionListDao.setOrganization(organization);

      var collectionsList = collectionDao.getAllCollections();
      assertThat(collectionsList).extracting(Collection::getName).containsOnly("Data", "Tasks");

      var tasksCollection = collectionsList.get(0).getName().equals("Tasks") ? collectionsList.get(0) : collectionsList.get(1);

      assertThat(tasksCollection.getRules().size()).isEqualTo(1);
      tasksCollection.getRules().forEach((ruleId, rule) -> {
         assertThat(rule.getType()).isEqualTo(Rule.RuleType.BLOCKLY);
         assertThat(rule.getConfiguration()).containsKeys(BlocklyRule.BLOCKLY_XML, BlocklyRule.BLOCKLY_JS);
         assertThat(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_XML).length()).isGreaterThan(100);
         assertThat(rule.getConfiguration().getString(BlocklyRule.BLOCKLY_JS).length()).isGreaterThan(100);
      });

      assertThat(tasksCollection.getAttributes().size()).isEqualTo(5);

      var dataCollection = collectionsList.get(0).getName().equals("Data") ? collectionsList.get(0) : collectionsList.get(1);

      assertThat(dataCollection.getAttributes().size()).isEqualTo(14);

      var linkTypes = linkTypeDao.getAllLinkTypes();

      assertThat(linkTypes.size()).isEqualTo(1);
      assertThat(linkTypes.get(0).getAttributes().size()).isEqualTo(1);

      var function = linkTypes.get(0).getAttributes().get(0).getFunction();
      assertThat(function).isNotNull();
      assertThat(function.getJs().length()).isGreaterThan(100);
      assertThat(function.getXml().length()).isGreaterThan(100);

      var sequences = sequenceDao.getAllSequences();
      assertThat(sequences.size()).isEqualTo(1);
      assertThat(sequences.get(0).getSeq()).isEqualTo(1);

      var selectionLists = selectionListDao.getAllLists(List.of(p1.getId()));

      assertThat(selectionLists).extracting(SelectionList::getName).contains("3 states");

      assertThat(selectionLists.size()).isEqualTo(6);
   }

}
