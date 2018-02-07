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
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;

import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class GroupFacade extends AbstractFacade {

   @Inject
   private GroupDao groupDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   public Group createGroup(String organizationId, Group group){
      checkPermissions(organizationId);

      return groupDao.createGroup(organizationId, group);
   }

   public Group updateGroup(String organizationId, String groupId, Group group){
      checkPermissions(organizationId);

      return groupDao.updateGroup(groupId, group);
   }

   public void deleteGroup(String organizationId, String groupId){
      checkPermissions(organizationId);

      userDao.deleteGroupFromUsers(organizationId, groupId);
      groupDao.deleteGroup(groupId);
   }

   public List<Group> getGroups(String organizationId){
      checkPermissions(organizationId);

      return groupDao.getAllGroups(organizationId);
   }

   private void checkPermissions(String organizationId) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, Role.MANAGE);
   }

}
