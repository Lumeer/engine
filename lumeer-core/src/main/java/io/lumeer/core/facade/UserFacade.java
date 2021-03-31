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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.DefaultWorkspace;
import io.lumeer.api.model.Feedback;
import io.lumeer.api.model.InvitationType;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.UserUtil;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateOrUpdateUser;
import io.lumeer.engine.api.event.RemoveUser;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class UserFacade extends AbstractFacade {

   @Inject
   private UserDao userDao;

   @Inject
   private UserLoginDao userLoginDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private FeedbackDao feedbackDao;

   @Inject
   private MailerService mailerService;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Inject
   private Event<CreateOrUpdateUser> createOrUpdateUserEvent;

   @Inject
   private Event<RemoveUser> removeUserEvent;

   @Inject
   private EmailFacade emailFacade;

   @Inject
   private UserNotificationFacade userNotificationFacade;

   public User createUser(String organizationId, User user) {
      user.setEmail(user.getEmail().toLowerCase());
      checkOrganizationInUser(organizationId, user);
      checkOrganizationPermissions(organizationId, Role.MANAGE);
      checkUsersCreate(organizationId, 1);

      User storedUser = userDao.getUserByEmail(user.getEmail());

      if (storedUser == null) {
         return createUserAndSendNotification(organizationId, user);
      }

      User updatedUser = updateExistingUser(organizationId, storedUser, user);

      return keepOnlyOrganizationGroups(updatedUser, organizationId);
   }

   public List<User> createUsersInWorkspace(final String organizationId, final String projectId, final List<User> users, final InvitationType invitationType) {
      // we need at least project management rights
      checkProjectPermissions(organizationId, projectId, Role.MANAGE);

      users.forEach(u -> u.setEmail(u.getEmail().toLowerCase()));

      // check if the users are already in the organization
      final List<User> usersInOrganization = getUsers(organizationId);
      final List<String> orgUserEmails = usersInOrganization.stream().map(User::getEmail).collect(Collectors.toList());
      final List<String> usersInRequest = users.stream().map(User::getEmail).collect(Collectors.toList());

      final List<User> newUsers;
      final Organization organization;

      // we need to add new users in the organization
      if (!orgUserEmails.containsAll(usersInRequest)) {
         organization = checkOrganizationPermissions(organizationId, Role.MANAGE);

         users.forEach(user -> {
            if (!checkOrganizationInUser(organizationId, user)) {
               installOrganizationInUser(organizationId, user);
            }
         });

         checkUsersCreate(organizationId, users.size());

         newUsers = createUsersInOrganization(organizationId, users);
         addUsersToOrganization(organization, newUsers);
      } else { // we will just amend the rights at the project level
         organization = organizationFacade.getOrganizationById(organizationId);
         newUsers = usersInOrganization.stream().filter(user -> usersInRequest.contains(user.getEmail())).collect(Collectors.toList());
         addUsersToOrganization(organization, newUsers);
      }

      addUsersToProject(organization, projectId, newUsers, invitationType);

      // in case of manage rights, we add them at the project level only
      if (invitationType != null && invitationType != InvitationType.JOIN_ONLY && invitationType != InvitationType.MANAGE) {
         shareResources(organization, projectDao.getProjectById(projectId), newUsers, invitationType);
      }

      return newUsers;
   }

   private Set<Role> getInvitationRoles(final InvitationType invitationType) {
      return getInvitationRoles(invitationType, new HashSet<>());
   }

   private Set<Role> getInvitationRoles(final InvitationType invitationType, Set<Role> minimalSet) {
      final Set<Role> roles = new HashSet<>(minimalSet);

      // do not wonder - there isn't the return statement on purpose so that we collect the roles on the way
      switch (invitationType) {
         case MANAGE:
            roles.add(Role.MANAGE);
         case READ_WRITE:
            roles.add(Role.WRITE);
         case READ_ONLY:
            roles.add(Role.READ);
      }

      return roles;
   }

   private void shareResources(final Organization organization, final Project project, final List<User> users, final InvitationType invitationType) {
      workspaceKeeper.setWorkspaceIds(organization.getId(), project.getId());

      final Set<Role> roles = getInvitationRoles(invitationType);

      final Set<Permission> permissionSet = new HashSet<>();
      users.forEach(user -> {
         permissionSet.add(Permission.buildWithRoles(user.getId(), roles));
      });

      final List<Collection> collections = collectionFacade.getCollections();
      collections.forEach(c -> collectionFacade.updateUserPermissions(c.getId(), permissionSet));

      final List<View> views = viewFacade.getViews();
      views.forEach(v -> viewFacade.updateUserPermissions(v.getId(), permissionSet));
   }

   private List<User> createUsersInOrganization(String organizationId, List<User> users) {
      return users.stream().map(user -> {
         user.setEmail(user.getEmail().toLowerCase());
         User storedUser = userDao.getUserByEmail(user.getEmail());

         if (storedUser == null) {
            return createUserAndSendNotification(organizationId, user);
         }

         User updatedUser = updateExistingUser(organizationId, storedUser, user);

         return keepOnlyOrganizationGroups(updatedUser, organizationId);
      }).collect(Collectors.toList());
   }

   private void addUsersToOrganization(Organization organization, List<User> users) {
      var newPermissions = buildUserPermission(organization, users, InvitationType.JOIN_ONLY);
      organizationFacade.updateUserPermissions(organization.getId(), newPermissions);
   }

   private Set<Permission> buildUserPermission(final Resource resource, final List<User> users, final InvitationType invitationType) {
      return users.stream()
                  .map(user -> {
                     var existingPermissions = resource.getPermissions().getUserPermissions().stream().filter(permission -> permission.getId().equals(user.getId())).findFirst();
                     var minimalSet = new HashSet<>(Set.of(Role.READ));
                     existingPermissions.ifPresent(permission -> minimalSet.addAll(permission.getRoles()));
                     return Permission.buildWithRoles(user.getId(), getInvitationRoles(invitationType, minimalSet));
                  })
                  .collect(Collectors.toSet());
   }

   private void addUsersToProject(Organization organization, final String projectId, final List<User> users, final InvitationType invitationType) {
      workspaceKeeper.setOrganizationId(organization.getId());
      var project = projectDao.getProjectById(projectId);
      var newPermissions = buildUserPermission(project, users, invitationType);
      projectFacade.updateUserPermissions(projectId, newPermissions);
   }

   private User createUserAndSendNotification(String organizationId, User user) {
      user.setEmail(user.getEmail().toLowerCase());
      User created = userDao.createUser(user);
      if (this.createOrUpdateUserEvent != null) {
         this.createOrUpdateUserEvent.fire(new CreateOrUpdateUser(organizationId, created));
      }
      if (authenticatedUser.getUserEmail() == null || !authenticatedUser.getUserEmail().equals(user.getEmail())) {
         emailFacade.sendInvitation(user.getEmail());
         userNotificationFacade.muteUpdateResourceNotifications(created.getId()); // do not send further org/proj shared notifications for this HTTP request
      }

      return created;
   }

   public User updateUser(String organizationId, String userId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkOrganizationPermissions(organizationId, Role.MANAGE);

      User storedUser = userDao.getUserById(userId);
      User updatedUser = updateExistingUser(organizationId, storedUser, user);

      return keepOnlyOrganizationGroups(updatedUser, organizationId);
   }

   public DataDocument updateHints(final DataDocument hints) {
      final User currentUser = getCurrentUser();
      currentUser.setHints(hints);

      User updatedUser = userDao.updateUser(currentUser.getId(), currentUser);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser.getHints();
   }

   private User updateExistingUser(String organizationId, User storedUser, User user) {
      final var mergedUser = UserUtil.mergeUsers(storedUser, user);
      final var updatedUser = updateUserAndSendNotification(organizationId, storedUser.getId(), mergedUser);

      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser;
   }

   private User updateUserAndSendNotification(String organizationId, String userId, User user) {
      User updated = userDao.updateUser(userId, user);
      if (createOrUpdateUserEvent != null) {
         this.createOrUpdateUserEvent.fire(new CreateOrUpdateUser(organizationId, updated));
      }
      return updated;
   }

   public void deleteUser(String organizationId, String userId) {
      checkOrganizationPermissions(organizationId, Role.MANAGE);

      userDao.deleteUserGroups(organizationId, userId);
      User storedUser = userDao.getUserById(userId);

      if (removeUserEvent != null) {
         removeUserEvent.fire(new RemoveUser(organizationId, storedUser));
      }

      userCache.updateUser(storedUser.getEmail(), storedUser);
   }

   public List<User> getUsers(String organizationId) {
      checkOrganizationPermissions(organizationId, Role.READ);

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

   public User getCurrentUserWithLastLogin() {
      final User user = getCurrentUser();
      user.setLastLoggedIn(userLoginDao.getPreviousLoginDate(user.getId()));

      return user;
   }

   public User patchCurrentUser(final User user, final String language) {
      User currentUser = authenticatedUser.getCurrentUser();

      if (user.hasNewsletter() != null) {
         currentUser.setNewsletter(user.hasNewsletter());
         mailerService.setUserSubscription(currentUser, !Language.fromString(language).equals(Language.CS)); // so that en is default
      }

      if (user.hasAgreement() != null) {
         currentUser.setAgreement(user.hasAgreement());
         if (user.hasAgreement()) {
            currentUser.setAgreementDate(ZonedDateTime.now());
         }
      }

      if (user.getWizardDismissed() != null) {
         currentUser.setWizardDismissed(user.getWizardDismissed());
      }

      if (user.getReferral() != null && (currentUser.getReferral() == null || "".equals(currentUser.getReferral())) && !user.getReferral().equals(Utils.strHexTo36(currentUser.getId()))) {
         currentUser.setReferral(user.getReferral());
      }

      if (user.getName() != null) {
         currentUser.setName(user.getName());
      }

      if (user.getNotifications() != null) {
         currentUser.setNotifications(user.getNotifications());
      }

      User updatedUser = userDao.updateUser(currentUser.getId(), currentUser);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser;
   }

   public void setDefaultWorkspace(DefaultWorkspace workspace) {
      Organization organization;
      if (workspace.getOrganizationId() != null) {
         organization = checkOrganizationPermissions(workspace.getOrganizationId(), Role.READ);
      } else {
         organization = checkOrganizationPermissionsByCode(workspace.getOrganizationCode(), Role.READ);
      }

      Project project;
      if (workspace.getProjectId() != null) {
         project = checkProjectPermissions(organization.getId(), workspace.getProjectId(), Role.READ);
      } else {
         project = checkProjectPermissionsByCode(organization.getId(), workspace.getProjectCode(), Role.READ);
      }

      DefaultWorkspace defaultWorkspace = new DefaultWorkspace(organization.getId(), project.getId());

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

   public boolean isUserAffiliate(final String userId) {
      return userDao.getUserById(userId).isAffiliatePartner();
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

   private Organization checkOrganizationPermissions(final String organizationId, final Role role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   private Organization checkOrganizationPermissionsByCode(final String organizationCode, final Role role) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   private Project checkProjectPermissions(final String organizationId, final String projectId, final Role role) {
      workspaceKeeper.setOrganizationId(organizationId);
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, role);

      return project;
   }

   private Project checkProjectPermissionsByCode(final String organizationId, final String projectCode, final Role role) {
      workspaceKeeper.setOrganizationId(organizationId);
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, role);

      return project;
   }

   private void installOrganizationInUser(final String organizationId, final User user) {
      if (user.getGroups() == null || user.getGroups().isEmpty()) {
         user.setGroups(Map.of(organizationId, Collections.emptySet()));
      }
   }

   private boolean checkOrganizationInUser(String organizationId, User user) {
      if (user.getGroups() == null || user.getGroups().isEmpty()) {
         return false;
      } else {
         if (user.getGroups().entrySet().size() != 1 || !user.getGroups().containsKey(organizationId)) {
            throw new BadFormatException("User " + user + " is in incorrect format");
         }
      }

      return true;
   }

   private void checkUsersCreate(final String organizationId, final int number) {
      permissionsChecker.checkUserCreationLimits(organizationId, userDao.getAllUsersCount(organizationId) + number);
   }

}
