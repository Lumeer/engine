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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.core.exception.BadFormatException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UserFacade extends AbstractFacade {

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   public User createUser(String organizationId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkPermissions(organizationId, Role.MANAGE);

      User storedUser = userDao.getUserByEmail(user.getEmail());

      if (storedUser == null) {
         return userDao.createUser(user);
      }

      return updateStoredUserGroups(organizationId, storedUser, user);
   }

   public User updateUser(String organizationId, String userId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkPermissions(organizationId, Role.MANAGE);

      User storedUser = userDao.getUserById(userId);

      return updateStoredUserGroups(organizationId, storedUser, user);
   }

   private User updateStoredUserGroups(String organizationId, User storedUser, User user) {
      Map<String, Set<String>> groups = storedUser.getGroups();
      if (groups == null) {
         groups = user.getGroups();
      } else if (user.getGroups() != null) {
         groups.putAll(user.getGroups());
      }

      user.setGroups(groups);

      User returnedUser = userDao.updateUser(storedUser.getId(), user);
      return keepOnlyOrganizationGroups(returnedUser, organizationId);
   }

   public void deleteUser(String organizationId, String userId) {
      checkPermissions(organizationId, Role.MANAGE);

      userDao.deleteUserGroups(organizationId, userId);
   }

   public List<User> getUsers(String organizationId) {
      checkPermissions(organizationId, Role.READ);

      return userDao.getAllUsers(organizationId).stream()
                    .map(user -> keepOnlyOrganizationGroups(user, organizationId))
                    .collect(Collectors.toList());
   }

   private User keepOnlyOrganizationGroups(User user, String organizationId) {
      if (user.getGroups().containsKey(organizationId)) {
         Set<String> groups = user.getGroups().get(organizationId);
         user.setGroups(Collections.singletonMap(organizationId, groups));
         return user;
      }
      user.setGroups(new HashMap<>());
      return user;
   }

   private void checkPermissions(final String organizationId, final Role role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);
   }

   private void checkOrganizationInUser(String organizationId, User user) {
      if (user.getGroups() == null || user.getGroups().isEmpty()) {
         return;
      }
      if (user.getGroups().entrySet().size() != 1 || !user.getGroups().containsKey(organizationId)) {
         throw new BadFormatException("User " + user + " is in incorrect format");
      }
   }

}
