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

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class GroupFacade extends AbstractFacade {

   @Inject
   private GroupDao groupDao;

   @Inject
   private UserDao userDao;

   public Group createGroup(Group group) {
      checkPermissions();

      return groupDao.createGroup(group);
   }

   public Group updateGroup(String groupId, Group group) {
      checkPermissions();

      return groupDao.updateGroup(groupId, group);
   }

   public void deleteGroup(String groupId) {
      checkPermissions();

      groupDao.deleteGroup(groupId);
      deleteGroupFromUsers(groupId);
   }

   public List<Group> getGroups() {
      checkPermissions();

      return groupDao.getAllGroups();
   }

   private void deleteGroupFromUsers(String groupId) {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      userDao.deleteGroupFromUsers(organization.getId(), groupId);

      userCache.clear();
   }

   private void checkPermissions() {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, Role.MANAGE);
   }

}
