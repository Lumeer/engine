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
package io.lumeer.core.auth;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.core.util.Colors;
import io.lumeer.core.util.Icons;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;

import java.io.Serializable;
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

   private AuthUserInfo authUserInfo = new AuthUserInfo();

   private Random rnd = new Random();

   void checkUser() {
      String authId = authUserInfo.user != null && authUserInfo.user.getAuthId() != null ? authUserInfo.user.getAuthId() : null;

      if (authId != null) { // production
         checkUserInProduction(authId, getUserEmail());
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

   private void checkUserInProduction(String authId, String email) {
      User userByAuthId = userDao.getUserByAuthId(authId);
      if (userByAuthId != null) {
         if (!userByAuthId.getEmail().equals(email)) {
            userByAuthId.setEmail(email);
            createDemoOrganizationIfNeeded(userByAuthId, false);
            userDao.updateUser(userByAuthId.getId(), userByAuthId);
         } else {
            createDemoOrganizationIfNeeded(userByAuthId, true);
         }
         userLoginDao.userLoggedIn(userByAuthId.getId());
      } else {
         User userByEmail = userDao.getUserByEmail(email);
         if (userByEmail != null) {
            userByEmail.setAuthId(authId);
            createDemoOrganizationIfNeeded(userByEmail, false);
            userDao.updateUser(userByEmail.getId(), userByEmail);
            userLoginDao.userLoggedIn(userByEmail.getId());
         } else {
            User createdUser = createNewUser(email, authId);
            createDemoOrganizationIfNeeded(createdUser, true);
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
      user.setAuthId(authId);
      return userDao.createUser(user);
   }

   private void createDemoOrganizationIfNeeded(User user, boolean performUpdate) {
      if (user.getGroups() != null && !user.getGroups().isEmpty()) {
         return;
      }

      Organization organization = createDemoOrganization(user);
      user.setGroups(Collections.singletonMap(organization.getId(), new HashSet<>()));

      createDemoProject(user);

      if (performUpdate) {
         userDao.updateUser(user.getId(), user);
      }
   }

   private Organization createDemoOrganization(User user) {
      String code = generateOrganizationCode(user.getEmail());
      Permission userPermission = new SimplePermission(user.getId(), Organization.ROLES);

      Organization organization = new JsonOrganization(code, "Lumeer demo", getDemoIcon(), getDemoColor(), null, null);
      organization.getPermissions().updateUserPermissions(userPermission);

      return organizationDao.createOrganization(organization);
   }

   private Project createDemoProject(final User user) {
      final String code = generateProjectCode();
      final Permission userPermission = new SimplePermission(user.getId(), Project.ROLES);
      Project project = new JsonProject(code, "Project", getDemoIcon(), getDemoColor(), null, null);
      project.getPermissions().updateUserPermissions(userPermission);

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
         codeWithSuffix = finalCode + num;
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
      return Icons.solidIcons.get(rnd.nextInt(Icons.solidIcons.size()));
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
