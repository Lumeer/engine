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
package io.lumeer.engine.controller;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.UserSettings;

import java.io.Serializable;
import java.util.Objects;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Stores user settings like default organization, project etc.
 */
@SessionScoped
public class UserSettingsFacade implements Serializable {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserFacade userFacade;

   /**
    * Reads user settings.
    *
    * @return Dto object for stored user settings.
    **/
   public UserSettings readUserSettings() {
      DataDocument dataDocument = dataStorage.readDocument(LumeerConst.UserSettings.COLLECTION_NAME, userFilter(userFacade.getUserEmail()));
      if (dataDocument == null) {
         return new UserSettings();
      }

      String organizationId = dataDocument.getString(LumeerConst.UserSettings.ATTR_DEFAULT_ORGANIZATION);
      String organizationCode = organizationId != null ? organizationFacade.getOrganizationCode(organizationId) : null;

      String projectId = dataDocument.getString(LumeerConst.UserSettings.ATTR_DEFAULT_PROJECT);
      String projectCode = organizationId != null && projectId != null ? projectFacade.getProjectCode(organizationId, projectId) : null;

      return new UserSettings(organizationCode, projectCode);
   }

   /**
    * Updates or creates user settings if not exist.
    *
    * @param userSettings
    *       Dto object for user settings.
    **/
   public void upsertUserSettings(UserSettings userSettings) {
      DataDocument dataDocument = new DataDocument();
      if (userSettings.getDefaultOrganization() != null) {
         String organizationId = organizationFacade.getOrganizationId(userSettings.getDefaultOrganization());
         if (organizationId == null) {
            return;
         }
         dataDocument.append(LumeerConst.UserSettings.ATTR_DEFAULT_ORGANIZATION, organizationId);
         if (userSettings.getDefaultProject() != null) {
            dataDocument.append(LumeerConst.UserSettings.ATTR_DEFAULT_PROJECT, projectFacade.getProjectId(organizationId, userSettings.getDefaultProject()));
         } else {
            dataDocument.append(LumeerConst.UserSettings.ATTR_DEFAULT_PROJECT, null);
         }
      } else {
         return;
      }
      dataDocument.append(LumeerConst.UserSettings.ATTR_USER, userFacade.getUserEmail());
      dataStorage.updateDocument(LumeerConst.UserSettings.COLLECTION_NAME, dataDocument, userFilter(userFacade.getUserEmail()));
   }

   /**
    * Removes user and settings.
    **/
   public void removeUserSettings() {
      dataStorage.dropDocument(LumeerConst.UserSettings.COLLECTION_NAME, userFilter(userFacade.getUserEmail()));
   }

   private DataFilter userFilter(String user) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.UserSettings.ATTR_USER, user);
   }

}
