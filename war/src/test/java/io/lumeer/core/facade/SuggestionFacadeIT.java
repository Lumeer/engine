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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.RoleOld;
import io.lumeer.api.model.SuggestionQuery;
import io.lumeer.api.model.SuggestionType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
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
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class SuggestionFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "LMR";
   private static final String PROJECT_CODE = "PROJ";

   private static final int SUGGESTIONS_LIMIT = 15;

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
   private static final Query QUERY = new Query();
   private Permission userPermission;
   private Permission groupPermission;
   private User user;

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
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);
      this.user = createdUser;

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      userPermission = Permission.buildWithRoles(createdUser.getId(), View.ROLES);
      groupPermission = Permission.buildWithRoles(GROUP, Collections.singleton(RoleOld.READ));

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      collectionDao.setProject(storedProject);
      collectionDao.createRepository(storedProject);

      linkTypeDao.setProject(storedProject);
      linkTypeDao.createRepository(storedProject);

      viewDao.setProject(storedProject);
      viewDao.createRepository(storedProject);

      collectionIds.clear();

      for (String name : COLLECTION_NAMES) {
         Permissions collectionPermissions = new Permissions();
         collectionPermissions.updateUserPermissions(new Permission(createdUser.getId(), Project.ROLES.stream().map(RoleOld::toString).collect(Collectors.toSet())));
         Collection jsonCollection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         collectionIds.add(collectionDao.createCollection(jsonCollection).getId());
      }

      for (String name : COLLECTION_NAMES_NO_RIGHTS) {
         Collection jsonCollection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, new Permissions());
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

      List<LinkType> linkTypes = suggestionFacade.suggest(new SuggestionQuery("link", SuggestionType.LINK)).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(lId1, lId2, lId4, lId5);

      linkTypes = suggestionFacade.suggest(new SuggestionQuery("other", SuggestionType.LINK)).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(lId3, lId4);

      linkTypes = suggestionFacade.suggest(new SuggestionQuery("nothingg", SuggestionType.LINK)).getLinkTypes();
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

      List<LinkType> linkTypes = suggestionFacade.suggest(new SuggestionQuery("link", SuggestionType.LINK)).getLinkTypes();
      assertThat(linkTypes).extracting(LinkType::getId).containsOnly(lId3, lId6, lId8);
   }

   @Test
   public void testSuggestLinkTypesMaxSuggestions() {
      for (int i = 0; i < 20; i++) {
         linkTypeDao.createLinkType(prepareLinkType("lalalink" + i, collectionIds.get(0), collectionIds.get(1)));
      }

      List<LinkType> linkTypes = suggestionFacade.suggest(new SuggestionQuery("link", SuggestionType.LINK)).getLinkTypes();
      assertThat(linkTypes).hasSize(SUGGESTIONS_LIMIT);
   }

   @Test
   public void testSuggestCollections() {
      String id1 = updateCollectionName(collectionIds.get(0), "Lumeer").getId();
      String id2 = updateCollectionName(collectionIds.get(1), "Lmr").getId();
      String id3 = updateCollectionName(collectionIds.get(2), "Lum").getId();
      updateCollectionName(collectionIdsNoRights.get(0), "Lumeer");
      updateCollectionName(collectionIdsNoRights.get(1), "Lume");
      updateCollectionName(collectionIdsNoRights.get(2), "Lumeere");

      List<Collection> collections = suggestionFacade.suggest(new SuggestionQuery("lum", SuggestionType.COLLECTION)).getCollections();
      assertThat(collections).hasSize(2).extracting(Collection::getId).containsOnly(id1, id3);

      collections = suggestionFacade.suggest(new SuggestionQuery("lm", SuggestionType.COLLECTION)).getCollections();
      assertThat(collections).hasSize(1).extracting(Collection::getId).containsOnly(id2);
   }

   private Collection updateCollectionName(String id, String name) {
      Collection collection = collectionDao.getCollectionById(id);
      collection.setName(name);
      return collectionDao.updateCollection(id, collection, null);
   }

   @Test
   public void testSuggestAttributes() {
      String id1 = createAttributes(collectionIds.get(0), "lmr", "lumr", "lumeer", "lm").getId();
      String id2 = createAttributes(collectionIds.get(1), "lmr", "llr", "larm").getId();
      String id3 = createAttributes(collectionIds.get(2), "la", "lm", "lumm").getId();
      createAttributes(collectionIdsNoRights.get(0), "lmr", "lumeer");
      createAttributes(collectionIdsNoRights.get(2), "lmr", "lumeer");

      List<Collection> collections = suggestionFacade.suggest(new SuggestionQuery("lm", SuggestionType.ATTRIBUTE)).getAttributes();
      assertThat(collections).hasSize(3).extracting(Collection::getId).containsOnly(id1, id2, id3);
      assertThat(getCollectionById(collections, id1).getAttributes()).hasSize(2).extracting(Attribute::getName).containsOnly("lmr", "lm");
      assertThat(getCollectionById(collections, id2).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("lmr");
      assertThat(getCollectionById(collections, id3).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("lm");

      collections = suggestionFacade.suggest(new SuggestionQuery("la", SuggestionType.ATTRIBUTE)).getAttributes();
      assertThat(collections).hasSize(2).extracting(Collection::getId).containsOnly(id2, id3);
      assertThat(getCollectionById(collections, id2).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("larm");
      assertThat(getCollectionById(collections, id3).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("la");
   }

   private Collection createAttributes(String id, String... attributeNames) {
      Collection collection = collectionDao.getCollectionById(id);
      Set<Attribute> attributes = Arrays.stream(attributeNames).map(Attribute::new).collect(Collectors.toSet());
      collection.setAttributes(attributes);
      return collectionDao.updateCollection(id, collection, null);
   }

   @Test
   public void testSuggestLinkAttributes() {
      String lId1 = linkTypeDao.createLinkType(prepareLinkType("l1", collectionIds.get(0), collectionIds.get(1), "lmr", "lumr", "lumeer", "lm")).getId();
      String lId2 = linkTypeDao.createLinkType(prepareLinkType("l2", collectionIds.get(1), collectionIds.get(2), "lmr", "llr", "larm")).getId();
      linkTypeDao.createLinkType(prepareLinkType("l3", collectionIds.get(0), collectionIdsNoRights.get(0), "lmr", "lumeer"));
      String lId4 = linkTypeDao.createLinkType(prepareLinkType("l4", collectionIds.get(2), collectionIds.get(0), "la", "lm", "lumm")).getId();
      linkTypeDao.createLinkType(prepareLinkType("l5", collectionIdsNoRights.get(2), collectionIds.get(1), "lmr", "lumeer"));

      List<LinkType> linkTypes = suggestionFacade.suggest(new SuggestionQuery("lm", SuggestionType.LINK_ATTRIBUTE)).getLinkAttributes();
      assertThat(linkTypes).hasSize(3).extracting(LinkType::getId).containsOnly(lId1, lId2, lId4);
      assertThat(getLinkTypeById(linkTypes, lId1).getAttributes()).hasSize(2).extracting(Attribute::getName).containsOnly("lmr", "lm");
      assertThat(getLinkTypeById(linkTypes, lId2).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("lmr");
      assertThat(getLinkTypeById(linkTypes, lId4).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("lm");

      linkTypes = suggestionFacade.suggest(new SuggestionQuery("la", SuggestionType.LINK_ATTRIBUTE)).getLinkAttributes();
      assertThat(linkTypes).hasSize(2).extracting(LinkType::getId).containsOnly(lId2, lId4);
      assertThat(getLinkTypeById(linkTypes, lId2).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("larm");
      assertThat(getLinkTypeById(linkTypes, lId4).getAttributes()).hasSize(1).extracting(Attribute::getName).containsOnly("la");
   }

   private LinkType getLinkTypeById(List<LinkType> linkTypes, String id) {
      return linkTypes.stream().filter(lt -> lt.getId().equals(id)).findFirst().get();
   }

   private Collection getCollectionById(List<Collection> collections, String id) {
      return collections.stream().filter(col -> col.getId().equals(id)).findFirst().get();
   }

   @Test
   public void testSuggestViews() {
      String vId1 = createView("some view").getId();
      String vId2 = createView("Viewlalala").getId();
      String vId3 = createView("another vie").getId();
      String vId4 = createView("lala ano").getId();
      String vId5 = createView("VIEW").getId();
      createView("something");

      List<View> views = suggestionFacade.suggest(new SuggestionQuery("view", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).containsOnly(vId1, vId2, vId5);

      views = suggestionFacade.suggest(new SuggestionQuery("vie", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).containsOnly(vId1, vId2, vId3, vId5);

      views = suggestionFacade.suggest(new SuggestionQuery("viewko", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).isEmpty();

      views = suggestionFacade.suggest(new SuggestionQuery("ano", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).containsOnly(vId3, vId4);
   }

   @Test
   public void testSuggestViewsNoRights() {
      createViewWithoutPermissions("some view");
      String vId2 = createView("Viewlalala").getId();
      createViewWithoutPermissions("another vie");
      createViewWithoutPermissions("lala ano");
      String vId5 = createView("VIEW").getId();
      createView("something");

      List<View> views = suggestionFacade.suggest(new SuggestionQuery("view", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).containsOnly(vId2, vId5);

      views = suggestionFacade.suggest(new SuggestionQuery("vie", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).containsOnly(vId2, vId5);

      views = suggestionFacade.suggest(new SuggestionQuery("ano", SuggestionType.VIEW)).getViews();
      assertThat(views).extracting(View::getId).isEmpty();
   }

   @Test
   public void testSuggestViewsMaxSuggestions() {
      for (int i = 0; i < 20; i++) {
         createView("someviewwwww" + i);
      }
      List<View> views = suggestionFacade.suggest(new SuggestionQuery("view", SuggestionType.VIEW)).getViews();
      assertThat(views).hasSize(SUGGESTIONS_LIMIT);
   }

   private LinkType prepareLinkType(String name, String collectionId1, String collectionId2) {
      return new LinkType(name, Arrays.asList(collectionId1, collectionId2), Collections.emptyList(), null);
   }

   private LinkType prepareLinkType(String name, String collectionId1, String collectionId2, String... attributeNames) {
      List<Attribute> attributes = Arrays.stream(attributeNames).map(Attribute::new).collect(Collectors.toList());
      return new LinkType(name, Arrays.asList(collectionId1, collectionId2), attributes, null);
   }

   private View prepareView(String name) {
      return new View(name, name, null, null, null, null, null, QUERY, PERSPECTIVE, CONFIG, null, this.user.getId(), Collections.emptyList());
   }

   private View createView(String name) {
      View view = prepareView(name);
      view.getPermissions().updateUserPermissions(userPermission);
      view.getPermissions().updateGroupPermissions(groupPermission);
      return viewDao.createView(view);
   }

   private View createViewWithoutPermissions(String name) {
      View view = prepareView(name);
      return viewDao.createView(view);
   }

}
