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

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.SuggestionType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class SuggestionFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "LMR";
   private static final String PROJECT_CODE = "PROJ";

   private static final int SUGGESTIONS_LIMIT = 12;
   private static final int TYPES_COUNT = 4;

   private static final List<String> COLLECTION_NAMES = Arrays.asList("Collection1", "Collection2", "Collection3");
   private static final List<String> COLLECTION_NAMES_NO_RIGHTS = Arrays.asList("Collection4", "Collection5", "Collection6");
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private List<String> collectionIds = new ArrayList<>();
   private List<String> collectionIdsNoRights = new ArrayList<>();

   private static final String PERSPECTIVE = "postit";
   private static final Object CONFIG = "configuration object";
   private static final JsonQuery QUERY;
   private static final Permission USER_PERMISSION;
   private static final Permission GROUP_PERMISSION;


   static {
      QUERY = new JsonQuery(Collections.singleton("testAttribute=42"), Collections.singleton("testCollection"), null, null, "test", 0, Integer.MAX_VALUE);

      USER_PERMISSION = new SimplePermission(USER, View.ROLES);
      GROUP_PERMISSION = new SimplePermission(GROUP, Collections.singleton(Role.READ));
   }

   @Inject
   private SuggestionFacade suggestionFacade;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private ViewDao viewDao;

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

   @Before
   public void configure() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      userDao.createUser(user);

      JsonProject project = new JsonProject();
      project.setPermissions(new JsonPermissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(storedProject);

      linkTypeDao.setProject(storedProject);
      linkTypeDao.createLinkTypeRepository(storedProject);

      viewDao.setProject(storedProject);
      viewDao.createViewsRepository(storedProject);

      collectionIds.clear();

      for (String name : COLLECTION_NAMES) {
         JsonPermissions collectionPermissions = new JsonPermissions();
         collectionPermissions.updateUserPermissions(new JsonPermission(USER, Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
         JsonCollection jsonCollection = new JsonCollection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         jsonCollection.setDocumentsCount(0);
         collectionIds.add(collectionDao.createCollection(jsonCollection).getId());
      }

      for (String name : COLLECTION_NAMES_NO_RIGHTS) {
         JsonCollection jsonCollection = new JsonCollection(name, name, COLLECTION_ICON, COLLECTION_COLOR, new JsonPermissions());
         jsonCollection.setDocumentsCount(0);
         collectionIdsNoRights.add(collectionDao.createCollection(jsonCollection).getId());
      }
   }

   @Test
   public void testSuggestLinkTypes() {
      String lId1 = linkTypeDao.createLinkType(prepareLinkType("lalaLink1", collectionIds.get(0), collectionIds.get(1))).getId();
      String lId2 = linkTypeDao.createLinkType(prepareLinkType("lalalink2", collectionIds.get(0), collectionIds.get(1))).getId();
      String lId3 = linkTypeDao.createLinkType(prepareLinkType("other", collectionIds.get(0), collectionIds.get(2))).getId();
      String lId4 = linkTypeDao.createLinkType(prepareLinkType("other Link", collectionIds.get(0), collectionIds.get(0))).getId();
      String lId5 = linkTypeDao.createLinkType(prepareLinkType("linkkkkkk", collectionIds.get(2), collectionIds.get(1))).getId();
      linkTypeDao.createLinkType(prepareLinkType("nothing", collectionIds.get(1), collectionIds.get(1)));
      linkTypeDao.createLinkType(prepareLinkType("blabla", collectionIds.get(1), collectionIds.get(2)));

      List<LinkType> linkTypes = suggestionFacade.suggest("link", SuggestionType.LINK).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(lId1, lId2, lId4, lId5);

      linkTypes = suggestionFacade.suggest("other", SuggestionType.LINK).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(lId3, lId4);

      linkTypes = suggestionFacade.suggest("nothingg", SuggestionType.LINK).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).isEmpty();
   }

   @Test
   public void testSuggestLinkTypesNoRights() {
      linkTypeDao.createLinkType(prepareLinkType("lalalink1", collectionIds.get(0), collectionIdsNoRights.get(1)));
      linkTypeDao.createLinkType(prepareLinkType("lalalink2", collectionIdsNoRights.get(0), collectionIds.get(1)));
      String lId3 = linkTypeDao.createLinkType(prepareLinkType("lalalink3", collectionIds.get(0), collectionIds.get(1))).getId();
      linkTypeDao.createLinkType(prepareLinkType("lalalink4", collectionIdsNoRights.get(0), collectionIdsNoRights.get(1)));

      linkTypeDao.createLinkType(prepareLinkType("lalalink5", collectionIdsNoRights.get(2), collectionIdsNoRights.get(1)));
      String lId6 = linkTypeDao.createLinkType(prepareLinkType("lalalink6", collectionIds.get(1), collectionIds.get(1))).getId();
      linkTypeDao.createLinkType(prepareLinkType("lalalink7", collectionIdsNoRights.get(0), collectionIds.get(1)));
      String lId8 = linkTypeDao.createLinkType(prepareLinkType("lalalink8", collectionIds.get(0), collectionIds.get(1))).getId();

      List<LinkType> linkTypes = suggestionFacade.suggest("link", SuggestionType.LINK).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(lId3, lId6, lId8);
   }

   @Test
   public void testSuggestLinkTypesMaxSuggestions() {
      for (int i = 0; i < 20; i++) {
         linkTypeDao.createLinkType(prepareLinkType("lalalink" + i, collectionIds.get(0), collectionIds.get(1)));
      }

      List<LinkType> linkTypes = suggestionFacade.suggest("link", SuggestionType.LINK).getLinkTypes();
      assertThat(linkTypes).hasSize(SUGGESTIONS_LIMIT);

   }

   @Test
   public void testSuggestViews() {
      String vId1 = createView("some view").getId();
      String vId2 = createView("Viewlalala").getId();
      String vId3 = createView("another vie").getId();
      String vId4 = createView("lala ano").getId();
      String vId5 = createView("VIEW").getId();
      createView("something");

      List<JsonView> views = suggestionFacade.suggest("view", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).containsOnly(vId1, vId2, vId5);

      views = suggestionFacade.suggest("vie", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).containsOnly(vId1, vId2, vId3, vId5);

      views = suggestionFacade.suggest("viewko", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).isEmpty();

      views = suggestionFacade.suggest("ano", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).containsOnly(vId3, vId4);
   }

   @Test
   public void testSuggestViewsNoRights() {
      createViewWithoutPermissions("some view");
      String vId2 = createView("Viewlalala").getId();
      createViewWithoutPermissions("another vie");
      createViewWithoutPermissions("lala ano");
      String vId5 = createView("VIEW").getId();
      createView("something");

      List<JsonView> views = suggestionFacade.suggest("view", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).containsOnly(vId2, vId5);

      views = suggestionFacade.suggest("vie", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).containsOnly(vId2, vId5);

      views = suggestionFacade.suggest("ano", SuggestionType.VIEW).getViews();
      assertThat(views).extracting(JsonView::getId).isEmpty();
   }

   @Test
   public void testSuggestViewsMaxSuggestions() {
      for (int i = 0; i < 20; i++) {
         createView("someviewwwww"+i);
      }
      List<JsonView> views = suggestionFacade.suggest("view", SuggestionType.VIEW).getViews();
      assertThat(views).hasSize(SUGGESTIONS_LIMIT);
   }

   private LinkType prepareLinkType(String name, String collectionId1, String collectionId2) {
      return new LinkType(null, name, Arrays.asList(collectionId1, collectionId2), Collections.emptyList());
   }


   private View prepareView(String name) {
      return new JsonView(name, name, null, null, null,null,  QUERY, PERSPECTIVE, CONFIG);
   }

   private View createView(String name) {
      View view = prepareView(name);
      view.getPermissions().updateUserPermissions(USER_PERMISSION);
      view.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return viewDao.createView(view);
   }

   private View createViewWithoutPermissions(String name){
      View view = prepareView(name);
      return viewDao.createView(view);
   }

}
