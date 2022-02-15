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

import java.util.HashSet;
import java.util.Set;

public class UserUtil {

   public static User mergeUsers(final User existingUser, final User updatedUser) {
      final var user = new User(existingUser.getEmail());

      user.setId(existingUser.getId());
      user.setName(updatedUser.getName() != null ? updatedUser.getName() : existingUser.getName());
      user.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : existingUser.getEmail());
      user.setAuthIds(updatedUser.getAuthIds() != null ? updatedUser.getAuthIds() : existingUser.getAuthIds());
      user.setOrganizations(UserUtil.mergeOrganizations(existingUser.getOrganizations(), updatedUser.getOrganizations()));
      user.setDefaultWorkspace(updatedUser.getDefaultWorkspace() != null ? updatedUser.getDefaultWorkspace() : existingUser.getDefaultWorkspace());
      user.setAgreement(updatedUser.hasAgreement() != null ? updatedUser.hasAgreement() : existingUser.hasAgreement());
      user.setAgreementDate(updatedUser.getAgreementDate() != null ? updatedUser.getAgreementDate() : existingUser.getAgreementDate());
      user.setNewsletter(updatedUser.hasNewsletter() != null ? updatedUser.hasNewsletter() : existingUser.hasNewsletter());
      user.setWizardDismissed(updatedUser.getWizardDismissed() != null ? updatedUser.getWizardDismissed() : existingUser.getWizardDismissed());
      user.setWishes(updatedUser.getWishes() != null ? updatedUser.getWishes() : existingUser.getWishes());
      user.setNotifications(updatedUser.getNotifications() != null ? updatedUser.getNotifications() : existingUser.getNotifications());
      user.setHints(updatedUser.getHints() != null ? updatedUser.getHints() : existingUser.getHints());
      user.setOnboarding(existingUser.getOnboarding());
      user.setEmailVerified(existingUser.isEmailVerified());
      user.setAffiliatePartner(existingUser.isAffiliatePartner());

      return user;
   }

   public static Set<String> mergeOrganizations(final Set<String> existingOrganizations, final Set<String> newOrganizations) {
      if (existingOrganizations == null) {
         return newOrganizations;
      }

      if (newOrganizations == null) {
         return existingOrganizations;
      }

      final var organizations = new HashSet<String>();
      organizations.addAll(existingOrganizations);
      organizations.addAll(newOrganizations);
      return organizations;
   }

}
