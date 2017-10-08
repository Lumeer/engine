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
