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
package io.lumeer.core.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.AppId;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkPermissionsType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.marvec.pusher.data.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

@RunWith(Arquillian.class)
public class PusherAdapterIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";
   private static final String GROUP = "testGroup";

   private static final Set<Role> EMPTY_ROLES = Set.of();
   private static final Set<Role> READ_ROLES = Set.of(new Role(RoleType.Read));

   private Organization organization;
   private Project project;
   private User user;
   private User otherUser;
   private Group group;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   private PusherAdapter pusherAdapter;

   @Before
   public void configureProject() {
      user = userDao.createUser(new User(USER));
      otherUser = userDao.createUser(new User("otherUser"));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      final Permission userPermission = Permission.buildWithRoles(this.user.getId(), READ_ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      group = groupDao.createGroup(new Group(GROUP, List.of(user.getId(), otherUser.getId())));
      user.setOrganizations(Collections.singleton(this.organization.getId()));
      user = userDao.updateUser(user.getId(), user);
      otherUser.setOrganizations(Collections.singleton(this.organization.getId()));
      otherUser = userDao.updateUser(otherUser.getId(), otherUser);

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), READ_ROLES));
      project.setPermissions(projectPermissions);
      this.project = projectDao.createProject(project);

      collectionDao.setProject(project);
      viewDao.setProject(project);
      linkTypeDao.setProject(project);

      FacadeAdapter facadeAdapter = new FacadeAdapter(permissionsChecker.getPermissionAdapter());
      ResourceAdapter resourceAdapter = new ResourceAdapter(permissionsChecker.getPermissionAdapter(), collectionDao, linkTypeDao, viewDao, userDao);
      pusherAdapter = new PusherAdapter(new AppId(""), facadeAdapter, resourceAdapter, permissionsChecker.getPermissionAdapter(), viewDao, linkTypeDao, collectionDao);

      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   @Test
   public void viewCollectionsLostOrGainedTest() {
      String aCollection = createCollection("A", EMPTY_ROLES, EMPTY_ROLES).getId();
      String bCollection = createCollection("B", EMPTY_ROLES, EMPTY_ROLES).getId();
      String cCollection = createCollection("C", EMPTY_ROLES, EMPTY_ROLES).getId();

      View view = createView("V1", new Query(Arrays.asList(new QueryStem(aCollection), new QueryStem(bCollection), new QueryStem(cCollection))), EMPTY_ROLES, EMPTY_ROLES);
      View viewWithUser = updateViewRoles(view.getId(), READ_ROLES, EMPTY_ROLES);
      View viewWithGroup = updateViewRoles(view.getId(), EMPTY_ROLES, READ_ROLES);

      // check gained by user roles
      List<Event> events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, view, viewWithUser);
      assertThat(events).hasSize(3);
      assertThat(events).extracting("name").containsOnly("Collection:update");
      assertThat(events).extracting("data").extracting("object").extracting("id").containsOnly(aCollection, bCollection, cCollection);

      // check gained by group roles
      events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, view, viewWithGroup);
      assertThat(events).hasSize(3);
      assertThat(events).extracting("name").containsOnly("Collection:update");
      assertThat(events).extracting("data").extracting("object").extracting("id").containsOnly(aCollection, bCollection, cCollection);

      // check lost by user roles
      events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, viewWithUser, view);
      assertThat(events).hasSize(4);
      assertThat(events).extracting("name").containsOnly("View:remove", "Collection:remove");
      assertThat(events).extracting("data").extracting("id").containsOnly(view.getId(), aCollection, bCollection, cCollection);

      // check lost by group roles
      events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, viewWithGroup, view);
      assertThat(events).hasSize(4);
      assertThat(events).extracting("name").containsOnly("View:remove", "Collection:remove");
      assertThat(events).extracting("data").extracting("id").containsOnly(view.getId(), aCollection, bCollection, cCollection);
   }

   @Test
   public void viewLinkTypesLostOrGainedTest() {
      String aCollection = createCollection("A", EMPTY_ROLES, EMPTY_ROLES).getId();
      String bCollection = createCollection("B", READ_ROLES, EMPTY_ROLES).getId();
      String cCollection = createCollection("C", READ_ROLES, EMPTY_ROLES).getId();
      String aLinkType = createLinkType("A", Arrays.asList(aCollection, bCollection), EMPTY_ROLES, EMPTY_ROLES).getId();
      String bLinkType = createLinkType("B", Arrays.asList(bCollection, cCollection), EMPTY_ROLES, READ_ROLES).getId();

      View view = createView("V1", new Query(Arrays.asList(new QueryStem(aCollection, Collections.singletonList(aLinkType)), new QueryStem(bCollection, Collections.singletonList(bLinkType)))), EMPTY_ROLES, EMPTY_ROLES);
      View viewWithUser = updateViewRoles(view.getId(), READ_ROLES, EMPTY_ROLES);
      View viewWithGroup = updateViewRoles(view.getId(), EMPTY_ROLES, READ_ROLES);

      // check gained by user roles
      List<Event> events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, view, viewWithUser);
      assertThat(events).hasSize(2);
      assertThat(events).extracting("name").containsOnly("Collection:update", "LinkType:update");
      assertThat(events).extracting("data").extracting("object").extracting("id").containsOnly(aCollection, aLinkType);

      // check gained by group roles
      events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, view, viewWithGroup);
      assertThat(events).hasSize(2);
      assertThat(events).extracting("name").containsOnly("Collection:update", "LinkType:update");
      assertThat(events).extracting("data").extracting("object").extracting("id").containsOnly(aCollection, aLinkType);

      // check lost by user roles
      events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, viewWithUser, view);
      assertThat(events).hasSize(3);
      assertThat(events).extracting("name").containsOnly("View:remove", "Collection:remove", "LinkType:remove");
      assertThat(events).extracting("data").extracting("id").containsOnly(view.getId(), aCollection, aLinkType);

      // check lost by group roles
      events = pusherAdapter.checkViewPermissionsChange(organization, project, otherUser, viewWithGroup, view);
      assertThat(events).hasSize(3);
      assertThat(events).extracting("name").containsOnly("View:remove", "Collection:remove", "LinkType:remove");
      assertThat(events).extracting("data").extracting("id").containsOnly(view.getId(), aCollection, aLinkType);
   }

   @Test
   public void collectionLinkTypesLostOrGainedTest() {
      String aCollection = createCollection("A", READ_ROLES, EMPTY_ROLES).getId();
      String bCollection = createCollection("B", EMPTY_ROLES, EMPTY_ROLES).getId();
      String cCollection = createCollection("C", READ_ROLES, EMPTY_ROLES).getId();
      String aLinkType = createLinkType("A", Arrays.asList(aCollection, bCollection)).getId();
      String bLinkType = createLinkType("B", Arrays.asList(bCollection, cCollection)).getId();

      Collection collection = collectionDao.getCollectionById(bCollection);
      Collection collectionWithRead = updateCollectionRoles(bCollection, EMPTY_ROLES, READ_ROLES);

      // check gained
      List<Event> events = pusherAdapter.checkCollectionsPermissionsChange(organization, project, otherUser, collection, collectionWithRead);
      assertThat(events).hasSize(2);
      assertThat(events).extracting("name").containsOnly("LinkType:update");
      assertThat(events).extracting("data").extracting("object").extracting("id").containsOnly(aLinkType, bLinkType);

      // check lost
      events = pusherAdapter.checkCollectionsPermissionsChange(organization, project, otherUser, collectionWithRead, collection);
      assertThat(events).hasSize(3);
      assertThat(events).extracting("name").containsOnly("Collection:remove", "LinkType:remove");
      assertThat(events).extracting("data").extracting("id").containsOnly(bCollection, aLinkType, bLinkType);
   }

   @Test
   public void linkTypeCollectionsLostOrGainedTest() {
      String aCollection = createCollection("A", EMPTY_ROLES, EMPTY_ROLES).getId();
      String bCollection = createCollection("B", EMPTY_ROLES, EMPTY_ROLES).getId();
      String cCollection = createCollection("C", EMPTY_ROLES, EMPTY_ROLES).getId();
      String aLinkTypeId = createLinkType("A", Arrays.asList(aCollection, bCollection), Set.of(), Set.of()).getId();
      String bLinkTypeId = createLinkType("B", Arrays.asList(bCollection, cCollection), Set.of(), Set.of()).getId();

      LinkType linkType = linkTypeDao.getLinkType(bLinkTypeId);
      LinkType linkTypeWithRead = updateLinkTypeRoles(bLinkTypeId, Set.of(), Set.of(new Role(RoleType.Read)));

      // check gained
      List<Event> events = pusherAdapter.checkLinkTypePermissionsChange(organization, project, otherUser, linkType, linkTypeWithRead);
      assertThat(events).hasSize(2);
      assertThat(events).extracting("name").containsOnly("Collection:update");
      assertThat(events).extracting("data").extracting("object").extracting("id").containsOnly(bCollection, cCollection);

      // check lost
      events = pusherAdapter.checkLinkTypePermissionsChange(organization, project, otherUser, linkTypeWithRead, linkType);
      assertThat(events).hasSize(3);
      assertThat(events).extracting("name").containsOnly("LinkType:remove", "Collection:remove");
      assertThat(events).extracting("data").extracting("id").containsOnly(bLinkTypeId, bCollection, cCollection);
   }

   private Collection createCollection(String name, Set<Role> userRoles, Set<Role> groupRoles) {
      Collection collection = new Collection(name, name, "", "", null);
      Permission userPermission = Permission.buildWithRoles(user.getId(), userRoles);
      collection.getPermissions().updateUserPermissions(userPermission);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), groupRoles);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      return collectionDao.createCollection(collection);
   }

   private Collection updateCollectionRoles(String id, Set<Role> userRoles, Set<Role> groupRoles) {
      Collection collection = collectionDao.getCollectionById(id);
      Permission userPermission = Permission.buildWithRoles(user.getId(), userRoles);
      collection.getPermissions().updateUserPermissions(userPermission);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), groupRoles);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      return collectionDao.updateCollection(id, collection, null);
   }

   private View createView(String name, Query query, Set<Role> userRoles, Set<Role> groupRoles) {
      View view = new View(name, name, "", "", "", 0L, new Permissions(), query, Collections.emptyList(), null, null, null, user.getId(), null);
      Permission userPermission = Permission.buildWithRoles(user.getId(), userRoles);
      view.getPermissions().updateUserPermissions(userPermission);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), groupRoles);
      view.getPermissions().updateGroupPermissions(groupPermission);
      return viewDao.createView(view);
   }

   private View updateViewRoles(String id, Set<Role> userRoles, Set<Role> groupRoles) {
      View view = viewDao.getViewById(id);
      Permission userPermission = Permission.buildWithRoles(user.getId(), userRoles);
      view.getPermissions().updateUserPermissions(userPermission);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), groupRoles);
      view.getPermissions().updateGroupPermissions(groupPermission);
      return viewDao.updateView(id, view, null);
   }

   private LinkType createLinkType(String name, List<String> collectionIds, Set<Role> userRoles, Set<Role> groupRoles) {
      LinkType linkType = new LinkType(name, collectionIds, null, null, new Permissions(), LinkPermissionsType.Custom);
      Permission userPermission = Permission.buildWithRoles(user.getId(), userRoles);
      linkType.getPermissions().updateUserPermissions(userPermission);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), groupRoles);
      linkType.getPermissions().updateGroupPermissions(groupPermission);
      return linkTypeDao.createLinkType(linkType);
   }

   private LinkType createLinkType(String name, List<String> collectionIds) {
      LinkType linkType = new LinkType(name, collectionIds, null, null, new Permissions(), LinkPermissionsType.Merge);
      return linkTypeDao.createLinkType(linkType);
   }

   private LinkType updateLinkTypeRoles(String id, Set<Role> userRoles, Set<Role> groupRoles) {
      LinkType linkType = linkTypeDao.getLinkType(id);
      Permission userPermission = Permission.buildWithRoles(user.getId(), userRoles);
      linkType.getPermissions().updateUserPermissions(userPermission);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), groupRoles);
      linkType.getPermissions().updateGroupPermissions(groupPermission);
      return linkTypeDao.updateLinkType(id, linkType, null);
   }

}
