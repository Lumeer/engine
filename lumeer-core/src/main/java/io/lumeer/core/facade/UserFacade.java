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

import io.lumeer.api.model.DefaultViewConfig;
import io.lumeer.api.model.DefaultWorkspace;
import io.lumeer.api.model.Feedback;
import io.lumeer.api.model.InitialUserData;
import io.lumeer.api.model.InvitationType;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.NotificationsSettings;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.ProductDemo;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserInvitation;
import io.lumeer.api.model.UserOnboarding;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.RoleUtils;
import io.lumeer.api.util.UserUtil;
import io.lumeer.core.auth.UserAuth0Utils;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateOrUpdateUser;
import io.lumeer.engine.api.event.ReloadGroups;
import io.lumeer.engine.api.event.RemoveUser;
import io.lumeer.engine.api.event.UpdateCurrentUser;
import io.lumeer.engine.api.event.UpdateDefaultWorkspace;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.storage.api.dao.DefaultViewConfigDao;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.InitialUserDataDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

@RequestScoped
public class UserFacade extends AbstractFacade {

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

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
   private FeedbackDao feedbackDao;

   @Inject
   private MailerService mailerService;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Inject
   private Event<CreateOrUpdateUser> createOrUpdateUserEvent;

   @Inject
   private Event<UpdateCurrentUser> updateCurrentUserEvent;

   @Inject
   private Event<UpdateDefaultWorkspace> updateDefaultWorkspaceEvent;

   @Inject
   private Event<RemoveUser> removeUserEvent;

   @Inject
   private Event<ReloadGroups> reloadGroupsEvent;

   @Inject
   private EmailFacade emailFacade;

   @Inject
   private UserNotificationFacade userNotificationFacade;

   @Inject
   private UserAuth0Utils userAuth0Utils;

   @Inject
   private EventLogFacade eventLogFacade;

   @Inject
   private InitialUserDataDao initialUserDataDao;

   @Inject
   private DefaultViewConfigDao defaultViewConfigDao;

   @Inject
   private HttpServletRequest request;

   public User createUser(String organizationId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkOrganizationPermissions(organizationId, RoleType.UserConfig);
      checkUsersCreate(organizationId, 1);

      user.setEmail(user.getEmail().toLowerCase());
      return createUsersWithDefaultData(organizationId, null, Collections.singletonList(user)).get(0);
   }

   public void checkUserTimeZone(final User user, final String timeZone) {
      if (StringUtils.isNotEmpty(timeZone)) {
         final TimeZone tz = TimeZone.getTimeZone(timeZone);

         if (user.getTimeZone() == null || !tz.getID().equals(user.getTimeZone())) {
            userDao.updateUserTimeZone(user.getId(), tz.getID());
         }
      }
   }

   public List<User> createUsersInWorkspace(final String organizationId, final String projectId, final List<UserInvitation> invitations) {
      // we need at least project management rights
      checkProjectPermissions(organizationId, projectId, RoleType.UserConfig);

      // check if the users are already in the organization
      final List<User> usersInOrganization = getUsers(organizationId);
      final List<String> orgUserEmails = usersInOrganization.stream().map(User::getEmail).collect(Collectors.toList());
      final List<UserInvitation> validInvitations = invitations.stream().filter(inv -> !inv.getEmail().isEmpty()).collect(Collectors.toList());
      final List<String> usersInRequest = validInvitations.stream().map(UserInvitation::getEmail).collect(Collectors.toList());
      final Map<String, InvitationType> invitationTypeMap = validInvitations.stream().collect(Collectors.toMap(UserInvitation::getEmail, UserInvitation::getInvitationType));

      final List<User> newUsers;
      final Organization organization;

      var userEmailsToAdd = new ArrayList<>(usersInRequest);
      userEmailsToAdd.removeAll(orgUserEmails);

      // we need to add new users in the organization
      if (userEmailsToAdd.size() > 0) {
         organization = checkOrganizationPermissions(organizationId, RoleType.UserConfig);
         checkUsersCreate(organizationId, userEmailsToAdd.size());

         newUsers = createUsersByInvitationsInOrganization(organizationId, projectId, validInvitations);
      } else { // we will just amend the rights at the project level
         organization = organizationFacade.getOrganizationById(organizationId);
         newUsers = usersInOrganization.stream().filter(user -> usersInRequest.contains(user.getEmail())).collect(Collectors.toList());
      }

      // we need to filter only users who can't read organization, otherwise organization permissions will be unnecessarily checked
      var organizationUsers = newUsers.stream()
                                      .filter(user -> !permissionsChecker.hasRole(organization, RoleType.Read, user.getId()))
                                      .collect(Collectors.toList());
      addUsersToOrganization(organization, organizationUsers);
      addUsersToProject(organization, projectId, newUsers, invitationTypeMap);

      logUsersInvitation(newUsers, organization);

      return newUsers;
   }

   private void logUsersInvitation(List<User> newUsers, Organization organization) {
      if (newUsers.size() > 0) {
         eventLogFacade.logEvent(
               authenticatedUser.getCurrentUser(),
               String.format(
                     "Added users to organization %s: %s",
                     organization.getCode(),
                     newUsers.stream().map(User::getEmail).collect(Collectors.joining(", "))
               )
         );
      }
   }

   private List<User> createUsersByInvitationsInOrganization(final String organizationId, final String projectId, final List<UserInvitation> invitations) {
      var users = invitations.stream().map(invitation -> new User(invitation.getEmail())).collect(Collectors.toList());
      return createUsersWithDefaultData(organizationId, projectId, users);
   }

   private List<User> createUsersWithDefaultData(final String organizationId, @Nullable final String projectId, final List<User> users) {
      List<InitialUserData> dataList = initialUserDataDao.get();
      Map<String, List<DefaultViewConfig>> defaultViewConfigsMap = new HashMap<>();
      List<Project> projects = projectDao.getAllProjects();

      List<User> createdUsers = users.stream().map(user -> {

         User storedUser = userDao.getUserByEmail(user.getEmail());
         if (storedUser == null) {
            user.setOrganization(organizationId);
            user.setDefaultWorkspace(new DefaultWorkspace(organizationId, projectId));
            patchNewUserDefaultData(user, projects, dataList, defaultViewConfigsMap);

            return createUserAndSendNotification(organizationId, user);
         }

         storedUser.setOrganizations(UserUtil.mergeOrganizations(storedUser.getOrganizations(), Set.of(organizationId)));
         if (storedUser.getDefaultWorkspace() == null) {
            storedUser.setDefaultWorkspace(new DefaultWorkspace(organizationId, projectId));
         }

         User updatedUser = updateExistingUser(organizationId, storedUser);

         return keepOnlyCurrentOrganization(updatedUser, organizationId);
      }).collect(Collectors.toList());

      Map<String, String> emailToIdMap = createdUsers.stream().collect(Collectors.toMap(User::getEmail, User::getId));

      for (Project project : projects) {
         List<DefaultViewConfig> configs = defaultViewConfigsMap.getOrDefault(project.getId(), new ArrayList<>())
               .stream().peek(config -> config.setUserId(emailToIdMap.get(config.getUserId())))
               .collect(Collectors.toList());

         if (configs.size() > 0) {
            defaultViewConfigDao.setProject(project);
            defaultViewConfigDao.insertConfigs(configs);
         }
      }

      return createdUsers;
   }

   private void addUsersToOrganization(Organization organization, List<User> users) {
      if (!users.isEmpty()) {
         var invitationTypeMap = users.stream().collect(Collectors.toMap(User::getEmail, user -> InvitationType.JOIN_ONLY));
         var newPermissions = buildUserPermission(organization, users, invitationTypeMap);
         organizationFacade.updateUserPermissions(organization.getId(), newPermissions);
      }
   }

   private Set<Permission> buildUserPermission(final Resource resource, final List<User> users, final Map<String, InvitationType> invitationTypeMap) {
      return users.stream()
                  .map(user -> {
                     var existingPermissions = resource.getPermissions().getUserPermissions().stream().filter(permission -> permission.getId().equals(user.getId())).findFirst();
                     var minimalSet = new HashSet<>(Set.of(new Role(RoleType.Read)));
                     existingPermissions.ifPresent(permission -> minimalSet.addAll(permission.getRoles()));
                     return Permission.buildWithRoles(user.getId(), RoleUtils.getInvitationRoles(invitationTypeMap.get(user.getEmail()), resource.getType(), minimalSet));
                  })
                  .collect(Collectors.toSet());
   }

   private void addUsersToProject(Organization organization, final String projectId, final List<User> users, final Map<String, InvitationType> invitationTypeMap) {
      if (!users.isEmpty()) {
         workspaceKeeper.setOrganizationId(organization.getId());
         var project = projectDao.getProjectById(projectId);
         var newPermissions = buildUserPermission(project, users, invitationTypeMap);
         projectFacade.updateUserPermissions(projectId, newPermissions);
      }
   }

   private User createUserAndSendNotification(String organizationId, User user) {
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

   private void patchNewUserDefaultData(User user, List<Project> projects, List<InitialUserData> dataList, Map<String, List<DefaultViewConfig>> configsMap) {
      // global settings
      InitialUserData data = dataList.stream().filter(datum -> datum.getProjectId() == null).findFirst().orElse(null);
      if (data != null) {
         var language = data.getLanguage() != null ? data.getLanguage().toString() : null;
         user.setLanguage(language);
         user.setNotifications(new NotificationsSettings(data.getNotifications(), language));
      }

      for (Project project : projects) {
         InitialUserData projectData = dataList.stream().filter(datum -> Objects.equals(datum.getProjectId(), project.getId())).findFirst().orElse(data);
         if (projectData != null && projectData.getDashboard() != null) {
            configsMap.computeIfAbsent(project.getId(), id -> new ArrayList<>());
            var defaultViewConfig = new DefaultViewConfig("default", Perspective.Search.getValue(), projectData.getDashboard(), ZonedDateTime.now());
            defaultViewConfig.setUserId(user.getEmail());
            configsMap.get(project.getId()).add(defaultViewConfig);
         }
      }
   }

   public User getUser(String organizationId, String userId) {
      checkOrganizationPermissions(organizationId, RoleType.Read);

      return keepOnlyCurrentOrganization(userDao.getUserById(userId), organizationId);
   }

   public User updateUser(String organizationId, String userId, User user) {
      checkOrganizationInUser(organizationId, user);
      checkOrganizationPermissions(organizationId, RoleType.UserConfig);

      User storedUser = userDao.getUserById(userId);
      User updatedUser = updateExistingUser(organizationId, storedUser, user);
      logUserVerified(storedUser, updatedUser);

      return keepOnlyCurrentOrganization(updatedUser, organizationId);
   }

   public DataDocument updateHints(final DataDocument hints) {
      final User currentUser = getCurrentUser();
      currentUser.setHints(hints);

      User updatedUser = updateUserAndSendNotification(null, currentUser.getId(), currentUser);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser.getHints();
   }

   public void logEvent(final String message) {
      if (message != null && !message.trim().isEmpty()) {
         eventLogFacade.logEvent(getCurrentUser(), message);
      }
   }

   public UserOnboarding updateOnboarding(final UserOnboarding onboarding) {
      final User currentUser = getCurrentUser();
      final UserOnboarding currentOnboarding = Objects.requireNonNullElse(currentUser.getOnboarding(), new UserOnboarding());
      currentUser.setOnboarding(onboarding);

      User updatedUser = updateUserAndSendNotification(null, currentUser.getId(), currentUser);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      if (Optional.ofNullable(currentOnboarding.getInvitedUsers()).orElse(0) == 0 && Optional.ofNullable(updatedUser.getOnboarding().getInvitedUsers()).orElse(0) > 0) {
         eventLogFacade.logEvent(updatedUser, "Invited " + updatedUser.getOnboarding().getInvitedUsers() + " users in onboarding process");
      }
      if (!currentOnboarding.isVideoPlayed() && updatedUser.getOnboarding().isVideoPlayed()) {
         eventLogFacade.logEvent(updatedUser, "Onboarding video played");
      }
      if (Optional.ofNullable(currentOnboarding.getVideoPlayedSeconds()).orElse(0) == 0 && Optional.ofNullable(updatedUser.getOnboarding().getVideoPlayedSeconds()).orElse(0) > 0) {
         eventLogFacade.logEvent(updatedUser, "Watched onboarding video for " + updatedUser.getOnboarding().getVideoPlayedSeconds() + " seconds");
      }
      if (!currentOnboarding.isHelpOpened() && updatedUser.getOnboarding().isHelpOpened()) {
         eventLogFacade.logEvent(updatedUser, "Opened help for the first time");
      }

      return updatedUser.getOnboarding();
   }

   private User updateExistingUser(String organizationId, User storedUser, User user) {
      final var mergedUser = UserUtil.mergeUsers(storedUser, user);
      final var updatedUser = updateUserAndSendNotification(organizationId, storedUser.getId(), mergedUser);

      logUserVerified(storedUser, updatedUser);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser;
   }

   private User updateExistingUser(String organizationId, User user) {
      final var updatedUser = updateUserAndSendNotification(organizationId, user.getId(), user);
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      return updatedUser;
   }

   private void logUserVerified(final User storedUser, final User updatedUser) {
      if (!storedUser.isEmailVerified() && updatedUser.isEmailVerified()) {
         eventLogFacade.logEvent(updatedUser, "Email verified.");
      }
   }

   private User updateUserAndSendNotification(String organizationId, String userId, User user) {
      User updated = userDao.updateUser(userId, user);
      if (createOrUpdateUserEvent != null) {
         if (organizationId == null && authenticatedUser.getCurrentUserId().equals(userId)) {
            this.updateCurrentUserEvent.fire(new UpdateCurrentUser(updated));
         } else {
            this.createOrUpdateUserEvent.fire(new CreateOrUpdateUser(organizationId, updated));
         }
      }
      return updated;
   }

   public void setUserGroups(String organizationId, String userId, Set<String> groups) {
      permissionsChecker.checkGroupsHandle();
      checkOrganizationPermissions(organizationId, RoleType.UserConfig);

      groupDao.deleteUserFromGroups(userId);
      groupDao.addUserToGroups(userId, groups);
      permissionsChecker.getPermissionAdapter().invalidateUserCache();

      if (reloadGroupsEvent != null) {
         reloadGroupsEvent.fire(new ReloadGroups(organizationId));
      }
   }

   public void deleteUser(String organizationId, String userId) {
      checkOrganizationPermissions(organizationId, RoleType.UserConfig);

      groupDao.setOrganization(getOrganization());
      groupDao.deleteUserFromGroups(userId);
      User storedUser = userDao.getUserById(userId);

      var organizations = new HashSet<>(storedUser.getOrganizations());
      organizations.remove(organizationId);
      storedUser.setOrganizations(organizations);
      storedUser = userDao.updateUser(userId, storedUser);

      if (removeUserEvent != null) {
         removeUserEvent.fire(new RemoveUser(organizationId, storedUser));
      }

      userCache.updateUser(storedUser.getEmail(), storedUser);
   }

   public List<User> getUsers(String organizationId) {
      checkOrganizationPermissions(organizationId, RoleType.Read);

      return userDao.getAllUsers(organizationId).stream()
                    .map(user -> keepOnlyCurrentOrganization(user, organizationId))
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

      user.setLastLoggedIn(userLoginDao.getPreviousLoginDate(user.getId()));

      return user;
   }

   public User patchCurrentUser(final User user, final String language) {
      final User currentUser = authenticatedUser.getCurrentUser();
      boolean sendPushNotification = false;

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

      if (user.getName() != null && StringUtils.compare(user.getName(), currentUser.getName()) != 0) {
         currentUser.setName(user.getName());
         try {
            userAuth0Utils.renameUser(user.getName());
         } catch (Auth0Exception e) {
            throw new UnsuccessfulOperationException("Unable to update user name: " + user.getName(), e);
         }
         sendPushNotification = true;
      }

      if (user.getNotifications() != null) {
         currentUser.setNotifications(user.getNotifications());
         sendPushNotification = true;
      }

      if (user.getLanguage() != null) {
         currentUser.setLanguage(user.getLanguage());
         sendPushNotification = true;
      }

      final User updatedUser;
      if (sendPushNotification) {
         updatedUser = updateUserAndSendNotification(null, currentUser.getId(), currentUser);
      } else {
         updatedUser = userDao.updateUser(currentUser.getId(), currentUser);
      }
      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      logUserVerified(currentUser, updatedUser);

      return updatedUser;
   }

   public void setDefaultWorkspace(DefaultWorkspace workspace) {
      Organization organization;
      if (workspace.getOrganizationId() != null) {
         organization = checkOrganizationPermissions(workspace.getOrganizationId(), RoleType.Read);
      } else {
         organization = checkOrganizationPermissionsByCode(workspace.getOrganizationCode(), RoleType.Read);
      }

      projectDao.setOrganization(organization);

      Project project;
      if (workspace.getProjectId() != null) {
         project = checkProjectPermissions(organization.getId(), workspace.getProjectId(), RoleType.Read);
      } else {
         project = checkProjectPermissionsByCode(organization.getId(), workspace.getProjectCode(), RoleType.Read);
      }

      DefaultWorkspace defaultWorkspace = new DefaultWorkspace(organization.getId(), project.getId());

      User currentUser = authenticatedUser.getCurrentUser();
      currentUser.setDefaultWorkspace(defaultWorkspace);
      User updatedUser = userDao.updateUser(currentUser.getId(), currentUser);

      userCache.updateUser(updatedUser.getEmail(), updatedUser);

      if (updateDefaultWorkspaceEvent != null) {
         updateDefaultWorkspaceEvent.fire(new UpdateDefaultWorkspace(organization, project));
      }
   }

   public Feedback createFeedback(Feedback feedback) {
      User currentUser = authenticatedUser.getCurrentUser();
      feedback.setUserId(currentUser.getId());
      feedback.setCreationTime(ZonedDateTime.now());

      freshdeskFacade.logTicket(currentUser, "User " + currentUser.getEmail() + " sent feedback in app", feedback.getMessage());

      return feedbackDao.createFeedback(feedback);
   }

   public void scheduleDemo(ProductDemo demo) {
      User currentUser = authenticatedUser.getCurrentUser();
      freshdeskFacade.logTicket(currentUser, "User " + currentUser.getEmail() + " wants to schedule demo", demo.getMessage());
   }

   public boolean isUserAffiliate(final String userId) {
      return userDao.getUserById(userId).isAffiliatePartner();
   }

   /**
    * Sets the current user's email as verified.
    */
   public void setUserEmailVerified() {
      permissionsChecker.checkSystemPermission();

      try {
         userAuth0Utils.setEmailVerified(authenticatedUser.getCurrentUser());
      } catch (Auth0Exception e) {
         throw new UnsuccessfulOperationException("Unable to set user email as verified.", e);
      }

   }

   /**
    * Removes current user from Auth0. The user token becomes invalid.
    */
   public void removeUser() {
      permissionsChecker.checkSystemPermission();

      try {
         userAuth0Utils.deleteUser(authenticatedUser.getCurrentUser());
      } catch (Auth0Exception e) {
         throw new UnsuccessfulOperationException("Unable to delete user.", e);
      }
   }

   /**
    * Obtains tokens for the given user. Allows API logins with username and password.
    * @param userName Username of a user to log in.
    * @param password User's password.
    * @return the token information.
    */
   public TokenHolder userLogin(final String userName, final String password) {
      if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
         try {
            Thread.sleep(1000); // always take your time so that this is not misused for quick password guessing
            String audience = request.getRequestURL().toString();
            audience = audience.substring(0, audience.length() - request.getRequestURI().length());
            audience += request.getContextPath();

            return userAuth0Utils.userLogin(userName, password, audience);
         } catch (Auth0Exception | InterruptedException e) {
            // do nothing, the login just fails
         }
      }

      return new TokenHolder();
   }

   private User keepOnlyCurrentOrganization(User user, String organizationId) {
      user.setOrganizations(Collections.singleton(organizationId));
      return user;
   }

   private Organization checkOrganizationPermissions(final String organizationId, final RoleType role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   private Organization checkOrganizationPermissionsByCode(final String organizationCode, final RoleType role) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   private Project checkProjectPermissions(final String organizationId, final String projectId, final RoleType role) {
      workspaceKeeper.setOrganizationId(organizationId);
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, role);

      return project;
   }

   private Project checkProjectPermissionsByCode(final String organizationId, final String projectCode, final RoleType role) {
      workspaceKeeper.setOrganizationId(organizationId);
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, role);

      return project;
   }

   private boolean checkOrganizationInUser(String organizationId, User user) {
      if (user.getOrganizations() == null || user.getOrganizations().isEmpty()) {
         user.setOrganization(organizationId);
         return false;
      } else {
         if (user.getOrganizations().size() != 1 || !user.getOrganizations().contains(organizationId)) {
            throw new BadFormatException("User " + user + " is in incorrect format");
         }
      }

      return true;
   }

   private void checkUsersCreate(final String organizationId, final int number) {
      permissionsChecker.checkUserCreationLimits(organizationId, userDao.getAllUsersCount(organizationId) + number);
   }

}
