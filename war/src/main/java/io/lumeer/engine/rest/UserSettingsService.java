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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.dto.UserSettings;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.UserSettingsFacade;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Service for manipulation with user settings.
 */
@Path("/settings/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserSettingsService implements Serializable {

   @Inject
   private UserSettingsFacade userSettingsFacade;

   @GET
   public UserSettings readUserSettings() {
      return userSettingsFacade.readUserSettings();
   }

   @PUT
   public void upsertUserSettings(final UserSettings userSettings) throws DbException {
      if (userSettings == null) {
         throw new BadRequestException();
      }
      userSettingsFacade.upsertUserSettings(userSettings);
   }

}
