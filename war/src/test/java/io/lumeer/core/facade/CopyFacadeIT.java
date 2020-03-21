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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.template.type.Template;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class CopyFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TEMPL";
   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String TEMPLATE = "SUPPL";
   private static final Language language = Language.EN;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private CopyFacade copyFacade;

   private User user;
   private Project project;
   private Organization organization;

   @Before
   public void setUp() {
      user = new User(USER);
      user = userDao.createUser(user);

      var template = Template.create(TEMPLATE);
      if (template != null) {
         createWorkspaceByTemplate(template);
      }

      organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      organizationPermissions.updateUserPermissions(new Permission(user.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      organization.setPermissions(organizationPermissions);
      organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(organization);

      project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(new Permission(user.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      project.setPermissions(projectPermissions);
      project = projectDao.createProject(project);

      collectionDao.setProject(project);

      workspaceKeeper.setWorkspaceIds(organization.getId(), project.getId());
   }

   private void createWorkspaceByTemplate(Template template) {
      var organizationCode = template.getOrganizationCode(language);
      var organization = new Organization(organizationCode, organizationCode, "", "", "", new Permissions());
      organizationDao.createOrganization(organization);

      var projectCode = template.getProjectCode(language);
      var project = new Project(projectCode, projectCode, "", "", "", new Permissions());
      projectDao.setOrganization(organization);
      projectDao.createProject(project);

      for (int i = 0; i < 4; i++) {
         var collection = new Collection(TEMPLATE + i, TEMPLATE + i, "", "", new Permissions());
         collectionDao.setProject(project);
         collectionDao.createCollection(collection);
      }
   }

   @Test
   public void testTemplateImport() {
      copyFacade.deepCopyTemplate(project, TEMPLATE, language);

      var collections = collectionFacade.getCollections();
      var templateCollections = collections.stream().filter(collection -> collection.getName().startsWith(TEMPLATE));
      assertThat(templateCollections).hasSize(4);
   }
}
