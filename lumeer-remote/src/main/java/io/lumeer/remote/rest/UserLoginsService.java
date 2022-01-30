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

import io.lumeer.api.model.OrganizationLoginsInfo;
import io.lumeer.core.facade.OrganizationFacade;

import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("userLogins")
public class UserLoginsService extends AbstractService {

   @Inject
   private OrganizationFacade organizationFacade;

   @GET
   @Path("info")
   public List<OrganizationLoginsInfo> getOrganizationsLoginsInfo(@QueryParam("descending") boolean descending) {
      return organizationFacade.getOrganizationsLoginsInfo(descending);
   }

   @GET
   @Path("delete")
   public List<OrganizationLoginsInfo> getOrganizationsLoginsInfoToDelete(@QueryParam("descending") boolean descending, @QueryParam("monthsOld") int months) {
      return organizationFacade.getOrganizationsLoginsInfoToDelete(descending, months);
   }

   @DELETE
   @Path("delete")
   public int getOrganizationsLoginsInfoToDelete(@QueryParam("monthsOld") int months) {
      return organizationFacade.deleteOldOrganizations(months);
   }
}
