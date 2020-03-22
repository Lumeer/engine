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
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.facade.FreshdeskFacade;
import io.lumeer.core.util.Colors;
import io.lumeer.core.util.Icons;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@SessionScoped
public class AuthenticatedUser implements Serializable {

   static class AuthUserInfo {
      User user = null;
      long lastUpdated = 0;
      String accessToken = "";
   }

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

   private AuthUserInfo authUserInfo = new AuthUserInfo();

   private Random rnd = new Random();

   void checkUser() {
      Set<String> authIds = authUserInfo.user != null && authUserInfo.user.getAuthIds() != null ? authUserInfo.user.getAuthIds() : Collections.emptySet();
      String authId = authIds.isEmpty() ? null : authIds.iterator().next();

      if (authId != null) { // production
         checkUserInProduction(authId, getUserEmail(), getUserName());
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

   private void checkUserInProduction(String authId, String email, String name) {
      User userByAuthId = userDao.getUserByAuthId(authId);
      if (userByAuthId != null) {
         if (!userByAuthId.getEmail().equals(email)) {
            userByAuthId.setName(name);
            userByAuthId.setEmail(email);
            createDemoWorkspaceIfNeeded(userByAuthId);
            userDao.updateUser(userByAuthId.getId(), userByAuthId);
         } else {
            createDemoWorkspaceIfNeeded(userByAuthId);
            if (userByAuthId.getName() == null || !userByAuthId.getName().equals(name)) {
               userByAuthId.setName(name);
            }
            userDao.updateUser(userByAuthId.getId(), userByAuthId);
         }
         userLoginDao.userLoggedIn(userByAuthId.getId());
      } else {
         User userByEmail = userDao.getUserByEmail(email);
         if (userByEmail != null) {
            userByEmail.setName(name);
            if (userByEmail.getAuthIds() != null) {
               userByEmail.getAuthIds().add(authId);
            } else {
               userByEmail.setAuthIds(new HashSet<>(Collections.singletonList(authId)));
            }
            createDemoWorkspaceIfNeeded(userByEmail);
            userDao.updateUser(userByEmail.getId(), userByEmail);
            userLoginDao.userLoggedIn(userByEmail.getId());
         } else {
            User createdUser = createNewUser(email, authId);
            createdUser.setName(name);
            createDemoWorkspaceIfNeeded(createdUser);
            userDao.updateUser(createdUser.getId(), createdUser);
            userLoginDao.userLoggedIn(createdUser.getId());
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
      User user = new User(email);
      user.setAuthIds(new HashSet<>(Arrays.asList(authId)));
      return userDao.createUser(user);
   }

   private void createDemoWorkspaceIfNeeded(User user) {
      if (user.getGroups() != null && !user.getGroups().isEmpty()) {
         return;
      }

      Organization organization = createDemoOrganization(user);
      user.setGroups(Collections.singletonMap(organization.getId(), new HashSet<>()));

      ((WorkspaceKeeper) selectedWorkspace).setOrganizationId(organization.getId());

      //Project project = createDemoProject(user);

      //user.setDefaultWorkspace(new DefaultWorkspace(organization.getId(), project.getId()));

      freshdeskFacade.logTicket(user, "A new user " + user.getEmail() + " logged for the first time in the system",
            "Organization " + organization.getCode() + " was created for them.");
   }

   private Organization createDemoOrganization(User user) {
      String code = generateOrganizationCode(user.getEmail());
      Permission userPermission = Permission.buildWithRoles(user.getId(), Organization.ROLES);

      Organization organization = new Organization(code, "Lumeer demo", getDemoIcon(), getDemoColor(), null, null);
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

   private Project createDemoProject(final User user) {
      final String code = generateProjectCode();
      final Permission userPermission = Permission.buildWithRoles(user.getId(), Project.ROLES);
      Project project = new Project(code, "Project", getDemoIcon(), getDemoColor(), null, null);
      project.getPermissions().updateUserPermissions(userPermission);
      project.setNonRemovable(true);

      return projectDao.createProject(project);
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

   private String generateProjectCode() {
      final Set<String> existingCodes = projectDao.getProjectsCodes();
      final String prefix = "PRJ";
      int no = 1;

      while (existingCodes.contains(prefix + no)) {
         no++;
      }

      return prefix + no;
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
   private String getUserName() {
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

   public String getUserSessionId() {
      return request.getSession().getId();
   }
}
