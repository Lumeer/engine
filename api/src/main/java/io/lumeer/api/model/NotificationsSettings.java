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
import java.util.Objects;

public class NotificationsSettings {

   public static final String SETTINGS = "settings";
   public static final String LANGUAGE = "language";

   private final List<NotificationSetting> notifications;
   private final String language;

   @JsonCreator
   public NotificationsSettings(@JsonProperty(SETTINGS) final List<NotificationSetting> notifications, @JsonProperty(LANGUAGE) final String language) {
      this.notifications = notifications;
      this.language = language;
   }

   public List<NotificationSetting> getNotifications() {
      return notifications;
   }

   public String getLanguage() {
      return language;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final NotificationsSettings that = (NotificationsSettings) o;
      return Objects.equals(notifications, that.notifications) && Objects.equals(language, that.language);
   }

   @Override
   public int hashCode() {
      return Objects.hash(notifications, language);
   }

   @Override
   public String toString() {
      return "NotificationsSettings{" +
            "notifications=" + notifications +
            ", language='" + language + '\'' +
            '}';
   }
}
