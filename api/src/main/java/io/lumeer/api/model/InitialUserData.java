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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class InitialUserData {

   public static final String DASHBOARD = "dashboard";
   public static final String NOTIFICATIONS = "notifications";
   public static final String LANGUAGE = "language";

   private String organizationId;
   private String projectId;

   private List<NotificationSetting> notifications;
   private String language;
   private Object dashboard;

   @JsonCreator
   public InitialUserData(
         @JsonProperty(DASHBOARD) final Object dashboard,
         @JsonProperty(NOTIFICATIONS) final List<NotificationSetting> notifications,
         @JsonProperty(LANGUAGE) final String language) {
      this.dashboard = dashboard;
      this.notifications = notifications;
      this.language = language;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   public String getProjectId() {
      return projectId;
   }

   public void setProjectId(final String projectId) {
      this.projectId = projectId;
   }

   public String getLanguage() {
      return language;
   }

   public List<NotificationSetting> getNotifications() {
      return notifications;
   }

   public Object getDashboard() {
      return dashboard;
   }

   @Override
   public String toString() {
      return "InitialUserData{" +
            "organizationId='" + organizationId + '\'' +
            ", projectId='" + projectId + '\'' +
            ", notifications=" + notifications +
            ", language='" + language + '\'' +
            ", dashboard=" + dashboard +
            '}';
   }
}
