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
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private ViewFacade viewFacade;

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

      workspaceKeeper.setWorkspace(organization.getId(), project.getId());
   }

   @Test
   @SuppressWarnings("unchecked")
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
      assertThat(linkTypes).hasSize(3);
      var objectiveKeyResultsLinkType = linkTypes.stream().filter(linkType -> linkType.getName().equals("Objectives Key Results")).findFirst().orElse(null);
      assertThat(objectiveKeyResultsLinkType).isNotNull();

      assertThat(a3.getFunction().getJs().indexOf("getLinkedDocuments(thisDocument, '" + objectiveKeyResultsLinkType.getId() + "')")).isGreaterThanOrEqualTo(0);

      var employeesCollection = collections.stream().filter(collection -> collection.getName().equals("Employees")).findFirst().orElse(null);
      var allEmployees = searchFacade.searchDocuments(new Query(new QueryStem(employeesCollection.getId())));

      // verify documents hierarchy
      var natosha = allEmployees.stream().filter(doc -> doc.getData().getString("a1").equals("Natosha")).findFirst().orElse(null);
      assertThat(natosha).isNotNull();
      var parent1 = natosha.getMetaData().getString(Document.META_PARENT_ID);
      assertThat(parent1).isNotNull();
      var jasmin = allEmployees.stream().filter(doc -> doc.getData().getId().equals(parent1)).findFirst().orElse(null);
      assertThat(jasmin.getData()).contains(Map.entry("a1", "Jasmin"));
      var parent2 = jasmin.getMetaData().getString(Document.META_PARENT_ID);
      assertThat(parent2).isNotNull();
      var marta = allEmployees.stream().filter(doc -> doc.getData().getId().equals(parent2)).findFirst().orElse(null);
      assertThat(marta.getData()).contains(Map.entry("a1", "Marta"));
      var parent3 = marta.getMetaData().getString(Document.META_PARENT_ID);
      assertThat(parent3).isNotNull();
      var macie = allEmployees.stream().filter(doc -> doc.getData().getId().equals(parent3)).findFirst().orElse(null);
      assertThat(macie.getData()).contains(Map.entry("a1", "Macie"));

      var keyResultsCollection = collections.stream().filter(collection -> collection.getName().equals("Key Results")).findFirst().orElse(null);
      var initiativesCollection = collections.stream().filter(collection -> collection.getName().equals("Initiatives")).findFirst().orElse(null);
      assertThat(keyResultsCollection).isNotNull();
      assertThat(initiativesCollection).isNotNull();
      assertThat(objectiveKeyResultsLinkType.getCollectionIds()).containsExactly(objectivesCollection.getId(), keyResultsCollection.getId());

      var allKeyResults = searchFacade.searchDocuments(new Query(new QueryStem(keyResultsCollection.getId())));
      var allInitiatives = searchFacade.searchDocuments(new Query(new QueryStem(initiativesCollection.getId())));

      var conversionDoc = allKeyResults.stream().filter(doc -> doc.getData().getString("a1").equals("15% conversion")).findFirst().orElse(null);
      var abTestingDoc = allInitiatives.stream().filter(doc -> doc.getData().getString("a1").equals("A/B testing the CTA")).findFirst().orElse(null);
      assertThat(conversionDoc).isNotNull();
      assertThat(abTestingDoc).isNotNull();

      var keyResultsInitiativesLinkType = linkTypes.stream().filter(linkType -> linkType.getName().equals("Key Results Initiatives")).findFirst().orElse(null);
      assertThat(keyResultsInitiativesLinkType).isNotNull();

      var links = searchFacade.getLinkInstances(new Query(new QueryStem(keyResultsCollection.getId(), List.of(keyResultsInitiativesLinkType.getId()), Set.of(conversionDoc.getId()), Collections.emptySet(), Collections.emptySet())));
      assertThat(links).isNotNull();

      var allDocuments = searchFacade.searchDocuments(new Query());
      assertThat(allDocuments).hasSize(78);

      var views = viewFacade.getViews();
      assertThat(views).hasSize(6);

      var okrInitiatives = views.stream().filter(view -> view.getName().equals("OKR Initiatives")).findFirst().orElse(null);
      assertThat(okrInitiatives).isNotNull();
      org.bson.Document config = (org.bson.Document) okrInitiatives.getConfig();
      assertThat(config.containsKey("table")).isTrue();
      var table = (org.bson.Document) config.get("table");
      assertThat(table.containsKey("parts"));
      var parts = (List<org.bson.Document>) table.get("parts");
      var collectionConfig = parts.stream().filter(doc -> doc.containsKey("collectionId") && doc.getString("collectionId").equals(keyResultsCollection.getId())).findFirst().orElse(null);
      assertThat(collectionConfig).isNotNull();
      var columns = (List<org.bson.Document>) collectionConfig.get("columns");
      assertThat(columns).isNotNull();
      var hiddenPart = columns.stream().filter(doc -> doc.containsKey("type") && doc.getString("type").equals("hidden")).findFirst().orElse(null);
      assertThat(hiddenPart).isNotNull();
      assertThat(hiddenPart.containsKey("attributeIds")).isTrue();
      assertThat((List<String>) hiddenPart.get("attributeIds")).containsExactly("a3", "a4", "a5", "a2");

      assertThat(okrInitiatives.getQuery().getStems()).hasSize(1);
      assertThat(okrInitiatives.getQuery().getStems().get(0).getCollectionId()).isEqualTo(keyResultsCollection.getId());
   }

   private Attribute getAttribute(final Collection<Attribute> attributes, final String id) {
      return attributes.stream().filter(attribute -> attribute.getId().equals(id)).findFirst().orElse(null);
   }
}
