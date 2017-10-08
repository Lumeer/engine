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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

/**
 * DTO object to store user settings
 */
@Immutable
public class UserSettings {

   private final String defaultOrganization;
   private final String defaultProject;

   public UserSettings() {
      this(null, null);
   }

   public UserSettings(final DataDocument dataDocument) {
      this(dataDocument.getString(LumeerConst.UserSettings.ATTR_DEFAULT_ORGANIZATION),
            dataDocument.getString(LumeerConst.UserSettings.ATTR_DEFAULT_PROJECT));
   }

   @JsonCreator
   public UserSettings(
         final @JsonProperty("defaultOrganization") String defaultOrganization,
         final @JsonProperty("defaultProject") String defaultProject) {
      this.defaultOrganization = defaultOrganization;
      this.defaultProject = defaultProject;
   }

   public String getDefaultOrganization() {
      return defaultOrganization;
   }

   public String getDefaultProject() {
      return defaultProject;
   }

   public DataDocument toDataDocument() {
      return new DataDocument(LumeerConst.UserSettings.ATTR_DEFAULT_ORGANIZATION, defaultOrganization)
            .append(LumeerConst.UserSettings.ATTR_DEFAULT_PROJECT, defaultProject);
   }
}
