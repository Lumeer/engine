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

package io.lumeer.api.util;

import io.lumeer.api.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserUtil {

   public static User mergeUsers(final User existingUser, final User updatedUser) {
      final var user = new User(existingUser.getEmail());

      user.setId(existingUser.getId());
      user.setName(updatedUser.getName() != null ? updatedUser.getName() : existingUser.getName());
      user.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : existingUser.getEmail());
      user.setAuthIds(updatedUser.getAuthIds() != null ? updatedUser.getAuthIds() : existingUser.getAuthIds());
      user.setGroups(UserUtil.mergeUserGroups(existingUser.getGroups(), updatedUser.getGroups()));
      user.setDefaultWorkspace(updatedUser.getDefaultWorkspace() != null ? updatedUser.getDefaultWorkspace() : existingUser.getDefaultWorkspace());
      user.setAgreement(updatedUser.hasAgreement() != null ? updatedUser.hasAgreement() : existingUser.hasAgreement());
      user.setAgreementDate(updatedUser.getAgreementDate() != null ? updatedUser.getAgreementDate() : existingUser.getAgreementDate());
      user.setNewsletter(updatedUser.hasNewsletter() != null ? updatedUser.hasNewsletter() : existingUser.hasNewsletter());
      user.setWizardDismissed(updatedUser.getWizardDismissed() != null ? updatedUser.getWizardDismissed() : existingUser.getWizardDismissed());
      user.setWishes(updatedUser.getWishes() != null ? updatedUser.getWishes() : existingUser.getWishes());
      user.setNotifications(updatedUser.getNotifications() != null ? updatedUser.getNotifications() : existingUser.getNotifications());
      user.setNotificationsLanguage(updatedUser.getNotificationsLanguage() != null ? updatedUser.getNotificationsLanguage() : existingUser.getNotificationsLanguage());
      user.setHints(updatedUser.getHints() != null ? updatedUser.getHints() : existingUser.getHints());

      return user;
   }

   public static Map<String, Set<String>> mergeUserGroups(final Map<String, Set<String>> existingGroups, final Map<String, Set<String>> newGroups) {
      if (existingGroups == null) {
         return newGroups;
      }

      if (newGroups == null) {
         return existingGroups;
      }

      final var groups = new HashMap<String, Set<String>>();
      groups.putAll(existingGroups);
      groups.putAll(newGroups);
      return groups;
   }

}
