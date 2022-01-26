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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class OrganizationLoginsInfo  {

   private final String organizationId;
   private final String organizationCode;
   private final Integer projectsCount;
   private final Set<String> projectsCodes;
   private final Integer usersCount;
   private final Set<String> userEmails;
   private final ZonedDateTime lastLoginDate;
   private final String lastLogin;

   public OrganizationLoginsInfo(final String organizationId, final String organizationCode, final Set<String> projectsCodes, final Set<String> userEmails, final ZonedDateTime lastLoginDate) {
      this.organizationId = organizationId;
      this.organizationCode = organizationCode;
      this.projectsCount = projectsCodes.size();
      this.projectsCodes = projectsCodes;
      this.userEmails = userEmails;
      this.usersCount =userEmails.size();
      this.lastLoginDate = lastLoginDate;
      this.lastLogin = lastLoginDate != null ? lastLoginDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")) : null;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public String getOrganizationCode() {
      return organizationCode;
   }

   public Set<String> getProjectsCodes() {
      return projectsCodes;
   }

   public Integer getProjectsCount() {
      return projectsCount;
   }

   public Integer getUsersCount() {
      return usersCount;
   }

   public String getLastLogin() {
      return lastLogin;
   }

   @JsonIgnore
   public Set<String> getUserEmails() {
      return userEmails;
   }

   @JsonIgnore
   public ZonedDateTime getLastLoginDate() {
      return lastLoginDate;
   }
}
