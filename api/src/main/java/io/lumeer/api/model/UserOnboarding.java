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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserOnboarding {

   public static final String TEMPLATE = "template";
   public static final String INVITED_USERS = "invitedUsers";
   public static final String HELP_OPENED = "helpOpened";
   public static final String VIDEO_SHOWED = "videoShowed";
   public static final String VIDEO_PLAYED = "videoPlayed";
   public static final String VIDEO_PLAYED_SECONDS = "videoPlayedSeconds";

   private String template = null;
   private Integer invitedUsers = null;
   private boolean helpOpened = false;
   private boolean videoShowed = false;
   private boolean videoPlayed = false;
   private Integer videoPlayedSeconds = null;

   public UserOnboarding() {
   }

   @JsonCreator
   public UserOnboarding(@JsonProperty(TEMPLATE) final String template,
         @JsonProperty(INVITED_USERS) final Integer invitedUsers,
         @JsonProperty(HELP_OPENED) final boolean helpOpened,
         @JsonProperty(VIDEO_SHOWED) final boolean videoShowed,
         @JsonProperty(VIDEO_PLAYED) final boolean videoPlayed,
         @JsonProperty(VIDEO_PLAYED_SECONDS) final Integer videoPlayedSeconds) {
      this.template = template;
      this.helpOpened = helpOpened;
      this.invitedUsers = invitedUsers;
      this.videoShowed = videoShowed;
      this.videoPlayed = videoPlayed;
      this.videoPlayedSeconds = videoPlayedSeconds;
   }

   public String getTemplate() {
      return template;
   }

   public Integer getInvitedUsers() {
      return invitedUsers;
   }

   public boolean isVideoShowed() {
      return videoShowed;
   }

   public boolean isHelpOpened() {
      return helpOpened;
   }

   public boolean isVideoPlayed() {
      return videoPlayed;
   }

   public Integer getVideoPlayedSeconds() {
      return videoPlayedSeconds;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof UserOnboarding)) {
         return false;
      }
      final UserOnboarding that = (UserOnboarding) o;
      return helpOpened == that.helpOpened && videoShowed == that.videoShowed && videoPlayed == that.videoPlayed && Objects.equals(template, that.template) && Objects.equals(invitedUsers, that.invitedUsers) && Objects.equals(videoPlayedSeconds, that.videoPlayedSeconds);
   }

   @Override
   public int hashCode() {
      return Objects.hash(template, invitedUsers, helpOpened, videoShowed, videoPlayed, videoPlayedSeconds);
   }

   @Override
   public String toString() {
      return "UserOnboarding{" +
            "template='" + template + '\'' +
            ", invitedUsers=" + invitedUsers +
            ", helpOpened=" + helpOpened +
            ", videoShowed=" + videoShowed +
            ", videoPlayed=" + videoPlayed +
            ", videoPlayedSeconds=" + videoPlayedSeconds +
            '}';
   }
}
