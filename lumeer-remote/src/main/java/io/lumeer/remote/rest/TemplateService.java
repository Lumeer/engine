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

import io.lumeer.api.model.Language;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.TemplateData;
import io.lumeer.core.facade.TemplateFacade;

import java.util.List;
import java.util.Objects;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Path("templates")
public class TemplateService extends AbstractService {

   @Inject
   private TemplateFacade templateFacade;

   @GET
   public List<Project> getTemplates() {
      return templateFacade.getTemplates();
   }

   @GET
   @Path("code/{templateCode:[a-zA-Z0-9_]{2,6}}")
   public Project getTemplate(@QueryParam("l") final Language language, @PathParam("templateCode") final String templateCode) {
      var nonNullLanguage = Objects.requireNonNullElse(language, Language.EN);

      return templateFacade.getTemplate(nonNullLanguage, templateCode);
   }

   @GET
   @Path("data/code/{organizationId:[0-9a-fA-F]{24}}/{templateCode:[a-zA-Z0-9_]{2,6}}")
   public TemplateData getTemplateDataByCode(@PathParam("organizationId") final String organizationId, @PathParam("templateCode") final String templateCode) {
      return templateFacade.getTemplateDataByCode(organizationId, templateCode);
   }

   @GET
   @Path("data/{organizationId:[0-9a-fA-F]{24}}/{templateId:[0-9a-fA-F]{24}}")
   public TemplateData getTemplateData(@PathParam("organizationId") final String organizationId, @PathParam("templateId") final String templateId) {
      return templateFacade.getTemplateData(organizationId, templateId);
   }
}
