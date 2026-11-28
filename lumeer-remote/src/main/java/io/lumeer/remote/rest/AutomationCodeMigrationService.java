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
package io.lumeer.remote.rest;

import io.lumeer.core.facade.AutomationCodeMigrationFacade;
import io.lumeer.core.js.AutomationCodeMigrationReport;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Migration of the automation code stored in rules and attribute functions to the lumeer bridge calls (system
 * administrators only): a dry run over all or selected organizations and projects, and the migration of one project.
 */
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("automation-code-migration")
public class AutomationCodeMigrationService extends AbstractService {

   @Inject
   private AutomationCodeMigrationFacade automationCodeMigrationFacade;

   @GET
   public AutomationCodeMigrationReport dryRun(@QueryParam("organizationId") String organizationId, @QueryParam("projectId") String projectId) {
      return automationCodeMigrationFacade.dryRun(organizationId, projectId);
   }

   @POST
   @Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}")
   public AutomationCodeMigrationReport migrateProject(@PathParam("organizationId") String organizationId, @PathParam("projectId") String projectId) {
      return automationCodeMigrationFacade.migrateProject(organizationId, projectId);
   }
}
