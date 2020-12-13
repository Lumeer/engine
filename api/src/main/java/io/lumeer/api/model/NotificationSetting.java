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

import java.util.Objects;

public class NotificationSetting {

   public static final String NOTIFICATION_TYPE = "notificationType";
   public static final String NOTIFICATION_CHANNEL = "notificationChannel";
   public static final String NOTIFICATION_FREQUENCY = "notificationFrequency";

   private NotificationType notificationType;
   private NotificationChannel notificationChannel;
   private NotificationFrequency notificationFrequency;

   @JsonCreator
   public NotificationSetting(@JsonProperty(NOTIFICATION_TYPE) final NotificationType notificationType, @JsonProperty(NOTIFICATION_CHANNEL) final NotificationChannel notificationChannel, @JsonProperty(NOTIFICATION_FREQUENCY) final NotificationFrequency notificationFrequency) {
      this.notificationType = notificationType;
      this.notificationChannel = notificationChannel;
      this.notificationFrequency = notificationFrequency;
   }

   public NotificationType getNotificationType() {
      return notificationType;
   }

   public void setNotificationType(final NotificationType notificationType) {
      this.notificationType = notificationType;
   }

   public NotificationChannel getNotificationChannel() {
      return notificationChannel;
   }

   public void setNotificationChannel(final NotificationChannel notificationChannel) {
      this.notificationChannel = notificationChannel;
   }

   public NotificationFrequency getNotificationFrequency() {
      return notificationFrequency;
   }

   public void setNotificationFrequency(final NotificationFrequency notificationFrequency) {
      this.notificationFrequency = notificationFrequency;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final NotificationSetting that = (NotificationSetting) o;
      return notificationType == that.notificationType && notificationChannel == that.notificationChannel && notificationFrequency == that.notificationFrequency;
   }

   @Override
   public int hashCode() {
      return Objects.hash(notificationType, notificationChannel, notificationFrequency);
   }

   @Override
   public String toString() {
      return "NotificationSetting{" +
            "notificationType=" + notificationType +
            ", notificationChannel=" + notificationChannel +
            ", notificationFrequency=" + notificationFrequency +
            '}';
   }
}
