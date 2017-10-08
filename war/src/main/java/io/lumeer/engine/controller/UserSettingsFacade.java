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
package io.lumeer.engine.controller;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.UserSettings;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
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
    * @throws DbException
    *       When organization or project doesn't exist.
    **/
   public void upsertUserSettings(UserSettings userSettings) throws DbException {
      if (userSettings.getDefaultOrganization() == null || userSettings.getDefaultProject() == null) {
         return;
      }
      String organizationId = organizationFacade.getOrganizationId(userSettings.getDefaultOrganization());
      if (organizationId == null) {
         // TODO add another exception by new principle
         throw new InvalidValueException(ErrorMessageBuilder.organizationDoesntExist(userSettings.getDefaultOrganization()));
      }
      String projectId = projectFacade.getProjectId(organizationId, userSettings.getDefaultProject());
      if (projectId == null) {
         // TODO add another exception by new principle
         throw new InvalidValueException(ErrorMessageBuilder.projectDoesntExist(userSettings.getDefaultOrganization(), userSettings.getDefaultProject()));
      }
      DataDocument dataDocument = new DataDocument(LumeerConst.UserSettings.ATTR_DEFAULT_ORGANIZATION, organizationId)
            .append(LumeerConst.UserSettings.ATTR_DEFAULT_PROJECT, projectId)
            .append(LumeerConst.UserSettings.ATTR_USER, userFacade.getUserEmail());
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
