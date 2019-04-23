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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.template.TemplateType;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class TemplateFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TEMPL";
   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private TemplateFacade templateFacade;

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;
   private Project project;
   private Organization organization;

   @Before
   public void setUp() {
      user = new User(USER);
      user = userDao.createUser(user);

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

      System.out.println(project);

      workspaceKeeper.setWorkspace(organization.getId(), project.getId());
   }

   @Test
   public void testTemplateImport() {
      templateFacade.installTemplate(organization, project, TemplateType.OKR, "en");

      var collections = collectionFacade.getCollections();
      assertThat(collections).hasSize(4);

      var objectivesCollection = collections.stream().filter(collection -> collection.getName().equals("Objectives")).findFirst().orElse(null);
      assertThat(objectivesCollection).isNotNull();
      assertThat(objectivesCollection.getIcon()).isEqualTo("fas fa-crosshairs");
      assertThat(objectivesCollection.getColor()).isEqualTo("#cc0000");
      assertThat(objectivesCollection.getAttributes()).hasSize(3);
      assertThat(objectivesCollection.getDefaultAttributeId()).isEqualTo("a1");
      var a3 = getAttribute(objectivesCollection.getAttributes(), "a3");
      assertThat(a3).isNotNull();
      assertThat(a3.getName()).isEqualTo("Progress");
      assertThat(a3.getConstraint().getType()).isEqualTo(ConstraintType.Percentage);
      assertThat(a3.getConstraint().getConfig()).isInstanceOf(org.bson.Document.class);
      assertThat(((org.bson.Document) a3.getConstraint().getConfig()).getLong("decimals")).isEqualTo(0L);
      assertThat(a3.getFunction()).isNotNull();

      var linkTypes = linkTypeFacade.getLinkTypes();
      var objectiveKeyResultsLinkType = linkTypes.stream().filter(linkType -> linkType.getName().equals("Objectives Key Results")).findFirst().orElse(null);
      assertThat(objectiveKeyResultsLinkType).isNotNull();

      System.out.println(a3.getFunction().getJs());
      assertThat(a3.getFunction().getJs().indexOf("getLinkedDocuments(thisDocument, '" + objectiveKeyResultsLinkType.getId() + "')")).isGreaterThanOrEqualTo(0);
   }

   private Attribute getAttribute(final Collection<Attribute> attributes, final String id) {
      return attributes.stream().filter(attribute -> attribute.getId().equals(id)).findFirst().orElse(null);
   }
}
