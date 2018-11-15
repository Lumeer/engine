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

import io.lumeer.api.model.DefaultWorkspace;
import io.lumeer.api.model.Feedback;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
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

   @Inject
   private ProjectDao projectDao;

   @Inject
   private FeedbackDao feedbackDao;

   @Inject
   private MailChimpFacade mailChimpFacade;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   public User createUser(String organizationId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkPermissions(organizationId, Role.MANAGE);
      checkUserCreate(organizationId);

      User storedUser = userDao.getUserByEmail(user.getEmail());

      if (storedUser == null) {
         return userDao.createUser(user);
      }

      User updatedUser = updateStoredUserGroups(storedUser, user);

      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return keepOnlyOrganizationGroups(updatedUser, organizationId);
   }

   public User updateUser(String organizationId, String userId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkPermissions(organizationId, Role.MANAGE);

      User storedUser = userDao.getUserById(userId);
      User updatedUser = updateStoredUserGroups(storedUser, user);

      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return keepOnlyOrganizationGroups(updatedUser, organizationId);
   }

   private User updateStoredUserGroups(User storedUser, User user) {
      Map<String, Set<String>> groups = storedUser.getGroups();
      if (groups == null) {
         groups = user.getGroups();
      } else if (user.getGroups() != null) {
         groups.putAll(user.getGroups());
      }

      user.setGroups(groups);

      return userDao.updateUser(storedUser.getId(), user);
   }

   public void deleteUser(String organizationId, String userId) {
      checkPermissions(organizationId, Role.MANAGE);

      userDao.deleteUserGroups(organizationId, userId);

      User storedUser = userDao.getUserById(userId);
      userCache.updateUser(storedUser.getEmail(), storedUser);
   }

   public List<User> getUsers(String organizationId) {
      checkPermissions(organizationId, Role.READ);

      return userDao.getAllUsers(organizationId).stream()
                    .map(user -> keepOnlyOrganizationGroups(user, organizationId))
                    .collect(Collectors.toList());
   }

   public User getCurrentUser() {
      User user = authenticatedUser.getCurrentUser();

      DefaultWorkspace defaultWorkspace = user.getDefaultWorkspace();
      if (defaultWorkspace == null || defaultWorkspace.getOrganizationId() == null || defaultWorkspace.getProjectId() == null) {
         return user;
      }

      try {
         Organization organization = organizationDao.getOrganizationById(defaultWorkspace.getOrganizationId());
         defaultWorkspace.setOrganizationCode(organization.getCode());

         projectDao.setOrganization(organization);
         Project project = projectDao.getProjectById(defaultWorkspace.getProjectId());
         defaultWorkspace.setProjectCode(project.getCode());
      } catch (ResourceNotFoundException e) {
         user.setDefaultWorkspace(null);
      }

      return user;
   }

   public User patchCurrentUser(final User user, final String language) {
      User currentUser = authenticatedUser.getCurrentUser();

      if (user.hasNewsletter() != null) {
         currentUser.setNewsletter(user.hasNewsletter());
         mailChimpFacade.setUserSubscription(user, language == null || !"cs".equals(language)); // so that en is default
      }

      if (user.hasAgreement() != null && user.hasAgreement()) {
         currentUser.setAgreement(user.hasAgreement());
         currentUser.setAgreementDate(ZonedDateTime.now());
      }

      User updatedUser = userDao.updateUser(currentUser.getId(), currentUser);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser;
   }

   public void setDefaultWorkspace(DefaultWorkspace defaultWorkspace) {
      Organization organization = checkPermissions(defaultWorkspace.getOrganizationId(), Role.READ);
      checkProjectPermissions(organization.getCode(), defaultWorkspace.getProjectId(), Role.READ);

      User currentUser = authenticatedUser.getCurrentUser();
      currentUser.setDefaultWorkspace(defaultWorkspace);
      User updatedUser = userDao.updateUser(currentUser.getId(), currentUser);

      userCache.updateUser(updatedUser.getEmail(), updatedUser);
   }

   public Feedback createFeedback(Feedback feedback) {
      User currentUser = authenticatedUser.getCurrentUser();
      feedback.setUserId(currentUser.getId());
      feedback.setCreationTime(ZonedDateTime.now());

      freshdeskFacade.logTicket(currentUser, "User " + currentUser.getEmail() + " sent feedback in app", feedback.getMessage());

      return feedbackDao.createFeedback(feedback);
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

   private Organization checkPermissions(final String organizationId, final Role role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   private void checkProjectPermissions(final String organizationCode, final String projectId, final Role role) {
      workspaceKeeper.setOrganization(organizationCode);
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, role);
   }

   private void checkOrganizationInUser(String organizationId, User user) {
      if (user.getGroups() == null || user.getGroups().isEmpty()) {
         return;
      }
      if (user.getGroups().entrySet().size() != 1 || !user.getGroups().containsKey(organizationId)) {
         throw new BadFormatException("User " + user + " is in incorrect format");
      }
   }

   private void checkUserCreate(final String organizationId) {
      permissionsChecker.checkUserCreationLimits(organizationId, userDao.getAllUsersCount(organizationId));
   }

}
