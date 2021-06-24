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

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class GroupFacade extends AbstractFacade {

   @Inject
   private GroupDao groupDao;

   public Group createGroup(Group group) {
      checkPermissions();
      checkGroupName(group.getName());

      group.setId(null);
      if (group.getUsers() == null) {
         group.setUsers(new ArrayList<>());
      }
      group.setUsers(new ArrayList<>(new HashSet<>(group.getUsers())));
      return mapGroupData(groupDao.createGroup(group));
   }

   public Group updateGroup(String groupId, Group group) {
      checkPermissions();
      Group storedGroup = groupDao.getGroup(groupId);
      if (!storedGroup.getName().equals(group.getName())) {
         checkGroupName(group.getName());
      }

      group.setId(groupId);
      if (group.getUsers() == null) {
         group.setUsers(new ArrayList<>());
      }
      group.setUsers(new ArrayList<>(new HashSet<>(group.getUsers())));
      return mapGroupData(groupDao.updateGroup(groupId, group));
   }

   private void checkGroupName(String name) {
      if (groupDao.getGroupByName(name) != null) {
         throw new IllegalArgumentException("Group with name " + name + " already exists");
      }
   }

   public void deleteGroup(String groupId) {
      checkPermissions();

      groupDao.deleteGroup(groupId);
   }

   public List<Group> getGroups() {
      checkPermissions();

      return groupDao.getAllGroups().stream().peek(this::mapGroupData).collect(Collectors.toList());
   }

   private Group mapGroupData(Group group) {
      group.setOrganizationId(workspaceKeeper.getOrganizationId());
      return group;
   }

   private void checkPermissions() {
      permissionsChecker.checkGroupsHandle();

      if (workspaceKeeper.getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, RoleType.UserConfig);
   }

}
