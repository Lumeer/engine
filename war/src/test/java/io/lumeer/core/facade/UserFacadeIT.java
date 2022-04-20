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

import static org.assertj.core.api.Assertions.*;

import io.lumeer.api.model.DefaultWorkspace;
import io.lumeer.api.model.Feedback;
import io.lumeer.api.model.InvitationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserInvitation;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.InitialUserDataDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class UserFacadeIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String USER1 = "user1@gmail.com";
   private static final String USER2 = "user2@gmail.com";
   private static final String USER3 = "user3@gmail.com";
   private static final String USER4 = "user4@gmail.com";

   private static final String USER1_NAME = "User 1";

   private Organization organization;
   private String organizationId1;

   private String organizationId2;
   private String organizationIdNotPermission;

   private Project project;

   @Inject
   private UserFacade userFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private FeedbackDao feedbackDao;

   @Inject
   private InitialUserDataDao initialUserDataDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Before
   public void configure() {
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      Organization organization1 = new Organization();
      organization1.setCode("LMR");
      organization1.setPermissions(new Permissions());
      organization1.getPermissions().updateUserPermissions(new Permission(createdUser.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig))));
      organization = organizationDao.createOrganization(organization1);
      organizationId1 = organization.getId();

      Organization organization2 = new Organization();
      organization2.setCode("MRL");
      organization2.setPermissions(new Permissions());
      organization2.getPermissions().updateUserPermissions(new Permission(createdUser.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig))));
      organizationId2 = organizationDao.createOrganization(organization2).getId();

      Organization organization3 = new Organization();
      organization3.setCode("RML");
      organization3.setPermissions(new Permissions());
      organizationIdNotPermission = organizationDao.createOrganization(organization3).getId();

      initialUserDataDao.setOrganization(organization);
      projectDao.setOrganization(organization);
      Project project = new Project();
      project.setCode("Lalala");
      project.setPermissions(new Permissions());
      project.getPermissions().updateUserPermissions(new Permission(createdUser.getId(), Project.ROLES));
      this.project = projectDao.createProject(project);

      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   @Test
   public void testCreateUser() {
      userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1));

      User stored = getUser(organizationId1, USER1);

      assertThat(stored).isNotNull();
      assertThat(stored.getName()).isEqualTo(USER1);
      assertThat(stored.getEmail()).isEqualTo(USER1);
      assertThat(stored.getOrganizations()).containsOnly(organizationId1);
   }

   @Test
   public void testCreateUsersToWorkspace() {
      List<UserInvitation> users = Arrays.asList(new UserInvitation(USER1, InvitationType.JOIN_ONLY), new UserInvitation(USER2, InvitationType.JOIN_ONLY));
      userFacade.createUsersInWorkspace(organizationId1, project.getId(), users);

      Arrays.asList(USER1, USER2).forEach(user -> {
         User stored = getUser(organizationId1, user);

         assertThat(stored).isNotNull();
         assertThat(stored.getEmail()).isEqualTo(user);
         assertThat(stored.getOrganizations()).containsOnly(organizationId1);
      });

      var organization = organizationDao.getOrganizationById(organizationId1);
      assertThat(organization.getPermissions().getUserPermissions().size()).isEqualTo(3);

      var project = projectDao.getProjectById(this.project.getId());
      assertThat(project.getPermissions().getUserPermissions().size()).isEqualTo(3);
   }

   @Test
   public void testCreateUserMultipleOrganizations() {
      User user11 = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1));
      User user21 = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER2));
      User user12 = userFacade.createUser(organizationId2, prepareUser(organizationId2, USER1));
      User user32 = userFacade.createUser(organizationId2, prepareUser(organizationId2, USER3));

      assertThat(user11.getId()).isEqualTo(user12.getId());
      assertThat(user21.getId()).isNotEqualTo(user32.getId());
      assertThat(user21.getId()).isNotEqualTo(user11.getId());
   }

   @Test
   public void testCreateUserExistingOrganization() {
      User user1 = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1));
      User user2 = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1));
      User user3 = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1));

      assertThat(user1.getId()).isEqualTo(user2.getId()).isEqualTo(user3.getId());
   }

   @Test
   public void testCreateExistingUserInNewOrganization() {
      final var user = new User(null, USER1_NAME, USER1, Collections.singleton(organizationId1));
      final var createdUser = userFacade.createUser(organizationId1, user);
      assertThat(createdUser).isNotNull();
      assertThat(createdUser.getId()).isNotNull();

      final var updatedUser = new User(null, null, USER1, Collections.singleton(organizationId2));
      final var returnedUser = userFacade.createUser(organizationId2, updatedUser);
      assertThat(returnedUser).isNotNull();
      assertThat(returnedUser.getId()).isEqualTo(createdUser.getId());
      assertThat(returnedUser.getName()).isEqualTo(USER1_NAME);
      assertThat(returnedUser.getEmail()).isEqualTo(USER1);
      assertThat(returnedUser.getOrganizations()).containsOnly(organizationId2);
      assertThat(returnedUser.getOrganizations()).doesNotContain(organizationId1);

      final var storedUser = userDao.getUserById(createdUser.getId());
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getId()).isEqualTo(createdUser.getId());
      assertThat(storedUser.getName()).isEqualTo(USER1_NAME);
      assertThat(storedUser.getEmail()).isEqualTo(USER1);
      assertThat(storedUser.getOrganizations()).contains(organizationId1);
      assertThat(storedUser.getOrganizations()).contains(organizationId2);

   }

   @Test
   public void testCreateUserNotPermission() {
      assertThatThrownBy(() -> userFacade.createUser(organizationIdNotPermission, prepareUser(organizationIdNotPermission, USER1)))
            .isInstanceOf(NoResourcePermissionException.class);
   }

   @Test
   public void testUpdateNameAndEmail() {
      String userId = createUser(organizationId1, USER1).getId();

      User toUpdate = prepareUser(organizationId1, USER1);
      toUpdate.setEmail(USER3);
      toUpdate.setName("newName");
      userFacade.updateUser(organizationId1, userId, toUpdate);

      User storedNotExisting = getUser(organizationId1, USER1);
      assertThat(storedNotExisting).isNull();

      User storedExisting = getUser(organizationId1, USER3);
      assertThat(storedExisting).isNotNull();
      assertThat(storedExisting.getName()).isEqualTo("newName");
   }

   @Test
   public void testUpdateUserNotPermission() {
      String userId = createUser(organizationId1, USER1).getId();

      assertThatThrownBy(() -> userFacade.updateUser(organizationIdNotPermission, userId, prepareUser(organizationIdNotPermission, USER3)))
            .isInstanceOf(NoResourcePermissionException.class);
   }

   @Test
   public void testUpdateUserBadFormat() {
      String userId = createUser(organizationId1, USER1).getId();

      assertThatThrownBy(() -> userFacade.updateUser(organizationId2, userId, prepareUser(organizationId1, USER3)))
            .isInstanceOf(BadFormatException.class);
   }

   @Test
   public void testDeleteUserNoPermission() {
      String id = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1)).getId();

      assertThatThrownBy(() -> userFacade.deleteUser(organizationIdNotPermission, id))
            .isInstanceOf(NoResourcePermissionException.class);
   }

   @Test
   public void testGetAllUsers() {
      String id = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1)).getId();
      String id2 = userFacade.createUser(organizationId1, prepareUser(organizationId1, USER2)).getId();
      userFacade.createUser(organizationId2, prepareUser(organizationId2, USER1));
      String id3 = userFacade.createUser(organizationId2, prepareUser(organizationId2, USER3)).getId();

      List<User> users = userFacade.getUsers(organizationId1);
      assertThat(users).extracting(User::getId).containsOnly(id, id2);

      users = userFacade.getUsers(organizationId2);
      assertThat(users).extracting(User::getId).containsOnly(id, id3);
   }

   @Test
   public void testGetAllUsersNoPermission() {
      assertThatThrownBy(() -> userFacade.getUsers(organizationIdNotPermission))
            .isInstanceOf(NoResourcePermissionException.class);
   }

   @Test
   public void testTooManyUsers() {
      userFacade.createUser(organizationId1, prepareUser(organizationId1, USER1));
      userFacade.createUser(organizationId1, prepareUser(organizationId1, USER2));
      userFacade.createUser(organizationId1, prepareUser(organizationId1, USER3));

      assertThatExceptionOfType(ServiceLimitsExceededException.class).isThrownBy(() -> {
         userFacade.createUser(organizationId1, prepareUser(organizationId1, USER4));
      }).as("On Trial plan, only 3 users should be allowed but it was possible to create 4th one.");
   }

   @Test
   public void testSetWorkspace() {
      DefaultWorkspace defaultWorkspace = new DefaultWorkspace(organization.getId(), project.getId());
      userFacade.setDefaultWorkspace(defaultWorkspace);

      User currentUser = userFacade.getCurrentUser();
      assertThat(currentUser.getDefaultWorkspace()).isNotNull();
      assertThat(currentUser.getDefaultWorkspace().getOrganizationId()).isEqualTo(organization.getId());
      assertThat(currentUser.getDefaultWorkspace().getOrganizationCode()).isEqualTo(organization.getCode());
      assertThat(currentUser.getDefaultWorkspace().getProjectId()).isEqualTo(project.getId());
      assertThat(currentUser.getDefaultWorkspace().getProjectCode()).isEqualTo(project.getCode());
   }

   @Test
   public void testSetWorkspaceNotExisting() {
      DefaultWorkspace defaultWorkspace = new DefaultWorkspace("5aedf1030b4e0ec3f46502d8", "5aedf1030b4e0ec3f46502d8");

      assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
            userFacade.setDefaultWorkspace(defaultWorkspace));

   }

   @Test
   public void testSetWorkspaceAndChange() {
      DefaultWorkspace defaultWorkspace = new DefaultWorkspace(organization.getId(), project.getId());
      userFacade.setDefaultWorkspace(defaultWorkspace);

      User currentUser = userFacade.getCurrentUser();
      assertThat(currentUser.getDefaultWorkspace().getOrganizationCode()).isNotEqualTo("newCode");
      assertThat(currentUser.getDefaultWorkspace().getProjectCode()).isNotEqualTo("someNewCode");

      project.setCode("someNewCode");
      projectDao.updateProject(project.getId(), project);

      organization.setCode("newCode");
      organizationDao.updateOrganization(organization.getId(), organization);

      currentUser = userFacade.getCurrentUser();
      assertThat(currentUser.getDefaultWorkspace().getOrganizationCode()).isEqualTo("newCode");
      assertThat(currentUser.getDefaultWorkspace().getProjectCode()).isEqualTo("someNewCode");
   }

   @Test
   public void testSetWorkspaceAndRemove() {
      DefaultWorkspace defaultWorkspace = new DefaultWorkspace(organization.getId(), project.getId());
      userFacade.setDefaultWorkspace(defaultWorkspace);

      User currentUser = userFacade.getCurrentUser();
      assertThat(currentUser.getDefaultWorkspace()).isNotNull();

      projectDao.deleteProject(project.getId());

      currentUser = userFacade.getCurrentUser();
      assertThat(currentUser.getDefaultWorkspace()).isNull();
   }

   @Test
   public void testCreateFeedback() {
      ZonedDateTime before = ZonedDateTime.now();

      String message = "This application is great!";
      Feedback feedback = new Feedback(message);
      Feedback returnedFeedback = userFacade.createFeedback(feedback);

      ZonedDateTime after = ZonedDateTime.now();

      assertThat(returnedFeedback).isNotNull();
      assertThat(returnedFeedback.getId()).isNotNull();
      assertThat(returnedFeedback.getUserId()).isEqualTo(userFacade.getCurrentUser().getId());
      assertThat(returnedFeedback.getCreationTime()).isAfterOrEqualTo(before.truncatedTo(ChronoUnit.MILLIS));
      assertThat(returnedFeedback.getCreationTime()).isBeforeOrEqualTo(after.truncatedTo(ChronoUnit.MILLIS));
      assertThat(returnedFeedback.getMessage()).isEqualTo(message);

      Feedback storedFeedback = feedbackDao.getFeedbackById(returnedFeedback.getId());
      assertThat(storedFeedback).isNotNull();
      assertThat(storedFeedback.getId()).isEqualTo(returnedFeedback.getId());
      assertThat(storedFeedback.getUserId()).isEqualTo(returnedFeedback.getUserId());
      assertThat(storedFeedback.getCreationTime()).isEqualTo(returnedFeedback.getCreationTime().truncatedTo(ChronoUnit.MILLIS));
      assertThat(storedFeedback.getMessage()).isEqualTo(returnedFeedback.getMessage());
   }

   private User createUser(String organizationId, String user) {
      return userDao.createUser(prepareUser(organizationId, user));
   }

   private User getUser(String organizationId, String user) {
      Optional<User> userOptional = userDao.getAllUsers(organizationId).stream().filter(u -> u.getEmail().equals(user)).findFirst();
      return userOptional.orElse(null);
   }

   private User prepareUser(String organizationId, String user) {
      User u = new User(user);
      u.setName(user);
      u.setOrganizations(Collections.singleton(organizationId));
      return u;
   }
}
