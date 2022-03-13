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
package io.lumeer.core.auth;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.adapter.AuditAdapter;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.facade.EventLogFacade;
import io.lumeer.core.facade.FreshdeskFacade;
import io.lumeer.core.util.Colors;
import io.lumeer.core.util.Icons;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@SessionScoped
public class AuthenticatedUser implements Serializable {

   static class AuthUserInfo {

      User user = null;
      long lastUpdated = 0;
      String accessToken = "";
   }

   public static final String DEFAULT_USER_FULL_NAME = "Alan Turing";

   public static final String DEFAULT_USERNAME = "aturing";
   public static final String DEFAULT_EMAIL = "aturing@lumeer.io";
   @Inject
   private HttpServletRequest request;

   @Inject
   private UserCache userCache;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserLoginDao userLoginDao;

   @Inject
   private SelectedWorkspace selectedWorkspace;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Inject
   private EventLogFacade eventLogFacade;

   @Inject
   private AuditDao auditDao;

   private AuthUserInfo authUserInfo = new AuthUserInfo();
   private AuditAdapter auditAdapter;
   private Random rnd = new Random();

   @PostConstruct
   public void init() {
      auditAdapter = new AuditAdapter(auditDao);
   }

   void checkUser(final boolean firstLogin) {
      Set<String> authIds = authUserInfo.user != null && authUserInfo.user.getAuthIds() != null ? authUserInfo.user.getAuthIds() : Collections.emptySet();
      String authId = authIds.isEmpty() ? null : authIds.iterator().next();

      if (authId != null) { // production
         checkUserInProduction(authId, getUserEmail(), getUserName(), getUserEmailVerified(), firstLogin);
      } else {
         checkLocalUser(DEFAULT_EMAIL);
      }
   }

   AuthUserInfo getAuthUserInfo() {
      return authUserInfo;
   }

   void setAuthUserInfo(final AuthUserInfo authUserInfo) {
      this.authUserInfo = authUserInfo;
   }

   public User getCurrentUser() {
      if (authUserInfo.user == null) {
         checkLocalUser(DEFAULT_EMAIL);
      }

      String userEmail = getUserEmail();
      return userCache.getUser(userEmail);
   }

   public String getCurrentUserId() {
      return getCurrentUser().getId();
   }

   private void checkUserInProduction(String authId, String email, String name, boolean emailVerified, boolean firstLogin) {
      User userByAuthId = userDao.getUserByAuthId(authId);
      if (userByAuthId != null) {
         if (!userByAuthId.getEmail().equalsIgnoreCase(email)) {
            userByAuthId.setEmail(email.toLowerCase());
         }
         checkUserAndUpdate(userByAuthId, name, emailVerified, firstLogin);
      } else {
         User userByEmail = userDao.getUserByEmail(email);
         if (userByEmail != null) {
            userByEmail.setEmail(userByEmail.getEmail().toLowerCase());
            if (userByEmail.getAuthIds() != null) {
               userByEmail.getAuthIds().add(authId);
            } else {
               userByEmail.setAuthIds(new HashSet<>(Collections.singletonList(authId)));
            }

            checkUserAndUpdate(userByEmail, name, emailVerified, firstLogin);
         } else {
            User createdUser = createNewUser(email, authId);
            checkUserAndUpdate(createdUser, name, emailVerified, firstLogin);
         }
      }
   }

   private void checkUserAndUpdate(User user, String name, boolean emailVerified, boolean firstLogin) {
      user.setName(name);

      if (!user.isEmailVerified() && emailVerified) {
         eventLogFacade.logEvent(user, "Email verified.");
      }
      user.setEmailVerified(emailVerified);

      createDemoWorkspaceIfNeeded(user);
      userDao.updateUser(user.getId(), user);
      if (firstLogin) {
         userLoginDao.userLoggedIn(user.getId());
         eventLogFacade.logEvent(user, "Logged in");

         checkUserWorkspace(user);
      }
   }

   private void checkUserWorkspace(User user) {
      if (user.getDefaultWorkspace() != null) {
         try {
            Organization organization = organizationDao.getOrganizationById(user.getDefaultWorkspace().getOrganizationId());
            projectDao.setOrganization(organization);

            Project project = projectDao.getProjectById(user.getDefaultWorkspace().getProjectId());
            auditDao.setProject(project);

            auditAdapter.registerEnter(user.getDefaultWorkspace().getOrganizationId(), ResourceType.PROJECT, project.getId(), user);
         } catch (ResourceNotFoundException ignore) {
         }
      }
   }

   private void checkLocalUser(String email) {
      User userByEmail = userDao.getUserByEmail(email);
      if (userByEmail == null) {
         createNewUser(email, null);
      }
   }

   private User createNewUser(String email, String authId) {
      User user = new User(email.toLowerCase());
      user.setAuthIds(new HashSet<>(Collections.singletonList(authId)));
      return userDao.createUser(user);
   }

   private void createDemoWorkspaceIfNeeded(User user) {
      if (user.getOrganizations() != null && !user.getOrganizations().isEmpty()) {
         return;
      }

      Organization organization = createDemoOrganization(user);
      user.setOrganizations(Collections.singleton(organization.getId()));

      ((WorkspaceKeeper) selectedWorkspace).setOrganizationId(organization.getId());

      eventLogFacade.logEvent(user, "A new user " + user.getEmail() + " logged for the first time in the system. " +
            "Organization " + organization.getCode() + " was created for them.");
   }

   private Organization createDemoOrganization(User user) {
      String code = generateOrganizationCode(user.getEmail());
      Permission userPermission = Permission.buildWithRoles(user.getId(), Organization.ROLES);

      Organization organization = new Organization(code, "Lumeer demo", getDemoIcon(), getDemoColor(), null, null, null);
      organization.getPermissions().updateUserPermissions(userPermission);
      organization.setNonRemovable(true);

      try {
         final Organization result = organizationDao.createOrganization(organization);
         return result;
      } catch (Exception e) {
         if (e.getCause().getMessage().contains("E11000")) {
            return createDemoOrganization(user);
         }

         throw e;
      }
   }

   private String generateOrganizationCode(String userEmail) {
      String cons = "bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ";

      StringBuilder code = new StringBuilder();
      for (int i = 0; i < userEmail.length(); i++) {
         if (cons.indexOf(userEmail.charAt(i)) != -1) {
            code.append(userEmail.charAt(i));
         }
      }

      if (code.length() < 5) {
         code.append("LMR");
      }

      String finalCode = code.substring(0, Math.min(5, code.length())).toUpperCase();
      Set<String> usedCodes = organizationDao.getOrganizationsCodes();
      if (!usedCodes.contains(finalCode)) {
         return finalCode;
      }

      int num = 2;
      String codeWithSuffix = finalCode;
      while (usedCodes.contains(codeWithSuffix)) {
         String numStr = String.valueOf(num);
         codeWithSuffix = finalCode.substring(0, Math.min(7, finalCode.length()) - numStr.length()) + num;
         num++;
      }
      return codeWithSuffix;
   }

   private String getDemoIcon() {
      return Icons.getSafeRandomIcon();
   }

   private String getDemoColor() {
      return Colors.palette.get(rnd.nextInt(Colors.palette.size()));
   }

   private String getCurrentUserEmail() {
      return getCurrentUser().getEmail();
   }

   /**
    * Gets the name of currently logged in user.
    *
    * @return The name of currently logged in user.
    */
   public String getUserName() {
      return authUserInfo.user != null && authUserInfo.user.getName() != null ? authUserInfo.user.getName() : DEFAULT_USERNAME;
   }

   /**
    * Gets the email of currently logged in user.
    *
    * @return The email of currently logged in user.
    */
   public String getUserEmail() {
      return authUserInfo.user != null && authUserInfo.user.getEmail() != null ? authUserInfo.user.getEmail() : DEFAULT_EMAIL;
   }

   /**
    * Gets the email verification status of the currently logged in user.
    *
    * @return The email verification status of the currently logged in user.
    */
   public Boolean getUserEmailVerified() {
      return authUserInfo.user != null && authUserInfo.user.isEmailVerified();
   }

   public String getUserSessionId() {
      return request.getSession().getId();
   }

   public static User getMachineUser() {
      return new User("", DEFAULT_USER_FULL_NAME, DEFAULT_EMAIL, Set.of());
   }
}
