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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class GroupFacadeIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String GROUP1 = "group1";
   private static final String GROUP2 = "group2";
   private static final String GROUP3 = "group3";

   private static final String USER1 = "user1@gmail.com";
   private static final String USER2 = "user2@gmail.com";
   private static final String USER3 = "user3@gmail.com";

   private Organization organization;

   @Inject
   private GroupFacade groupFacade;

   @Inject
   private GroupDao groupDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configure() {
      JsonOrganization organization1 = new JsonOrganization();
      organization1.setCode("LMR");
      organization1.setPermissions(new JsonPermissions());
      organization1.getPermissions().updateUserPermissions(new JsonPermission(USER, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      organization = organizationDao.createOrganization(organization1);

      workspaceKeeper.setOrganization(organization.getCode());
      groupDao.createGroupsRepository(organization);
      groupDao.setOrganization(organization);
   }

   @Test
   public void testCreateGroup() {
      Group group = groupFacade.createGroup(new Group(GROUP1));

      Group stored = getGroup(GROUP1);

      assertThat(stored).isNotNull();
      assertThat(stored.getId()).isEqualTo(group.getId());
      assertThat(stored.getName()).isEqualTo(GROUP1);
   }

   @Test
   public void testCreateGroupNotPermission() {
      setOrganizationWithoutPermissions();

      assertThatThrownBy(() -> groupFacade.createGroup(new Group(GROUP1)))
            .isInstanceOf(NoPermissionException.class);
   }

   @Test
   public void testUpdateGroup() {
      String groupId = groupFacade.createGroup(new Group(GROUP1)).getId();
      assertThat(getGroup(GROUP1)).isNotNull();

      groupFacade.updateGroup(groupId, new Group(GROUP2));
      assertThat(getGroup(GROUP1)).isNull();
      assertThat(getGroup(GROUP2)).isNotNull();
   }

   @Test
   public void testUpdateGroupNoPermission() {
      String groupId = groupFacade.createGroup(new Group(GROUP1)).getId();

      setOrganizationWithoutPermissions();
      assertThatThrownBy(() -> groupFacade.updateGroup(groupId, new Group(GROUP2)))
            .isInstanceOf(NoPermissionException.class);
   }

   @Test
   public void testDeleteGroup() {
      String id1 = groupFacade.createGroup(new Group(GROUP1)).getId();
      String id2 = groupFacade.createGroup(new Group(GROUP2)).getId();

      List<Group> groups = groupDao.getAllGroups();
      assertThat(groups).extracting(Group::getName).containsOnly(GROUP1, GROUP2);

      groupFacade.deleteGroup(id1);
      groups = groupDao.getAllGroups();
      assertThat(groups).extracting(Group::getName).containsOnly(GROUP2);

      groupFacade.deleteGroup(id2);
      groups = groupDao.getAllGroups();
      assertThat(groups).isEmpty();
   }

   @Test
   public void testDeleteGroupWithUsers() {
      String id1 = groupFacade.createGroup(new Group(GROUP1)).getId();
      String id2 = groupFacade.createGroup(new Group(GROUP2)).getId();
      String id3 = groupFacade.createGroup(new Group(GROUP3)).getId();

      userDao.createUser(prepareUser(USER1, new HashSet<>(Arrays.asList(id1, id2, id3))));
      userDao.createUser(prepareUser(USER2, new HashSet<>(Arrays.asList(id1, id2))));
      userDao.createUser(prepareUser(USER3, new HashSet<>(Arrays.asList(id2, id3))));

      assertThat(getUser(organization.getId(), USER1).getGroups().get(organization.getId())).containsOnly(id1, id2, id3);
      assertThat(getUser(organization.getId(), USER2).getGroups().get(organization.getId())).containsOnly(id1, id2);
      assertThat(getUser(organization.getId(), USER3).getGroups().get(organization.getId())).containsOnly(id2, id3);

      groupFacade.deleteGroup(id1);

      assertThat(getUser(organization.getId(), USER1).getGroups().get(organization.getId())).containsOnly(id2, id3);
      assertThat(getUser(organization.getId(), USER2).getGroups().get(organization.getId())).containsOnly(id2);
      assertThat(getUser(organization.getId(), USER3).getGroups().get(organization.getId())).containsOnly(id2, id3);

      groupFacade.deleteGroup(id2);

      assertThat(getUser(organization.getId(), USER1).getGroups().get(organization.getId())).containsOnly(id3);
      assertThat(getUser(organization.getId(), USER2).getGroups().get(organization.getId())).isEmpty();
      assertThat(getUser(organization.getId(), USER3).getGroups().get(organization.getId())).containsOnly(id3);

      groupFacade.deleteGroup(id3);

      assertThat(getUser(organization.getId(), USER1).getGroups().get(organization.getId())).isEmpty();
      assertThat(getUser(organization.getId(), USER2).getGroups().get(organization.getId())).isEmpty();
      assertThat(getUser(organization.getId(), USER3).getGroups().get(organization.getId())).isEmpty();
   }

   @Test
   public void testDeleteGroupNoPermission() {
      String id = groupFacade.createGroup(new Group(GROUP1)).getId();

      setOrganizationWithoutPermissions();
      assertThatThrownBy(() -> groupFacade.deleteGroup(id))
            .isInstanceOf(NoPermissionException.class);
   }

   @Test
   public void testGetAllGroups() {
      groupFacade.createGroup(new Group(GROUP1));
      groupFacade.createGroup(new Group(GROUP2));

      List<Group> groups = groupFacade.getGroups();
      assertThat(groups).extracting(Group::getName).containsOnly(GROUP1, GROUP2);
   }

   @Test
   public void testGetAllGroupsNoPermission() {
      setOrganizationWithoutPermissions();

      assertThatThrownBy(() -> groupFacade.getGroups())
            .isInstanceOf(NoPermissionException.class);
   }

   private Group getGroup(String group) {
      Optional<Group> groupOptional = groupDao.getAllGroups().stream().filter(g -> g.getName().equals(group)).findFirst();
      return groupOptional.orElse(null);
   }

   private User getUser(String organizationId, String user) {
      Optional<User> userOptional = userDao.getAllUsers(organizationId).stream().filter(u -> u.getEmail().equals(user)).findFirst();
      return userOptional.orElse(null);
   }

   private User prepareUser(String user, Set<String> groups) {
      User u = new User(user);
      u.setName(user);
      u.setGroups(Collections.singletonMap(organization.getId(), groups));
      return u;
   }

   private void setOrganizationWithoutPermissions() {
      JsonOrganization organization3 = new JsonOrganization();
      organization3.setCode("RML");
      organization3.setPermissions(new JsonPermissions());
      Organization organizationNotPermission = organizationDao.createOrganization(organization3);

      workspaceKeeper.setOrganization(organizationNotPermission.getCode());
      groupDao.createGroupsRepository(organizationNotPermission);
      groupDao.setOrganization(organizationNotPermission);
   }
}
