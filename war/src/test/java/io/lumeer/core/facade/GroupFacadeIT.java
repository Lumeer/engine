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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class GroupFacadeIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String GROUP1 = "group1";
   private static final String GROUP2 = "group2";
   private static final String GROUP3 = "group3";

   private static final String USER1 = "user1@gmail.com";
   private static final String USER2 = "user2@gmail.com";
   private static final String USER3 = "user3@gmail.com";

   private Organization organization;
   private User user;

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

   @Inject
   private PermissionsChecker permissionsChecker;

   @BeforeEach
   public void configure() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization1 = new Organization();
      organization1.setCode("LMR");
      organization1.setPermissions(new Permissions());
      organization1.getPermissions().updateUserPermissions(new Permission(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig))));
      organization = organizationDao.createOrganization(organization1);

      workspaceKeeper.setOrganizationId(organization.getId());
      groupDao.createRepository(organization);
      groupDao.setOrganization(organization);

      PermissionCheckerUtil.allowGroups();
      permissionsChecker.getPermissionAdapter().invalidateUserCache();
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
            .isInstanceOf(NoResourcePermissionException.class);
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
   public void testUpdateGroupDifferentId() {
      String groupId = groupFacade.createGroup(new Group(GROUP1)).getId();
      assertThat(getGroup(GROUP1)).isNotNull();

      String newGroupId = "5aedf1030b4e0ec3f46502d8";

      groupFacade.updateGroup(groupId, new Group(newGroupId, GROUP2));
      assertThat(getGroup(GROUP1)).isNull();
      assertThat(getGroup(GROUP2)).isNotNull();
      assertThat(getGroup(GROUP2).getId()).isEqualTo(groupId);
   }

   @Test
   public void testUpdateGroupNoPermission() {
      String groupId = groupFacade.createGroup(new Group(GROUP1)).getId();

      setOrganizationWithoutPermissions();
      assertThatThrownBy(() -> groupFacade.updateGroup(groupId, new Group(GROUP2)))
            .isInstanceOf(NoResourcePermissionException.class);
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
   public void testDeleteGroupNoPermission() {
      String id = groupFacade.createGroup(new Group(GROUP1)).getId();

      setOrganizationWithoutPermissions();
      assertThatThrownBy(() -> groupFacade.deleteGroup(id))
            .isInstanceOf(NoResourcePermissionException.class);
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
            .isInstanceOf(NoResourcePermissionException.class);
   }

   private Group getGroup(String group) {
      Optional<Group> groupOptional = groupDao.getAllGroups().stream().filter(g -> g.getName().equals(group)).findFirst();
      return groupOptional.orElse(null);
   }

   private void setOrganizationWithoutPermissions() {
      Organization organization3 = new Organization();
      organization3.setCode("RML");
      organization3.setPermissions(new Permissions());
      organization3.getPermissions().updateUserPermissions(new Permission(this.user.getId(), Set.of(new Role(RoleType.ProjectContribute))));
      Organization organizationNotPermission = organizationDao.createOrganization(organization3);

      workspaceKeeper.setOrganizationId(organizationNotPermission.getId());
      groupDao.createRepository(organizationNotPermission);
      groupDao.setOrganization(organizationNotPermission);
   }
}
