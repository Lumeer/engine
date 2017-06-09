/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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

   private final String activeOrganization;
   private final String activeProject;

   public UserSettings(final DataDocument dataDocument) {
      this(dataDocument.getString(LumeerConst.UserSettings.ATTR_ACTIVE_ORGANIZATION),
            dataDocument.getString(LumeerConst.UserSettings.ATTR_ACTIVE_PROJECT));
   }

   @JsonCreator
   public UserSettings(
         final @JsonProperty("activeOrganization") String activeOrganization,
         final @JsonProperty("activeProject") String activeProject) {
      this.activeOrganization = activeOrganization;
      this.activeProject = activeProject;
   }

   public String getActiveOrganization() {
      return activeOrganization;
   }

   public String getActiveProject() {
      return activeProject;
   }

   public DataDocument toDataDocument() {
      return new DataDocument(LumeerConst.UserSettings.ATTR_ACTIVE_ORGANIZATION, activeOrganization)
            .append(LumeerConst.UserSettings.ATTR_ACTIVE_PROJECT, activeProject);
   }
}
