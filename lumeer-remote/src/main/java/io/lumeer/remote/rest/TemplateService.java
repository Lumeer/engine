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
import io.lumeer.core.facade.TemplateFacade;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;

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
   public Project getTemplate(@QueryParam("l") Language language, @PathParam("templateCode") String templateCode) {
      var nonNullLanguage = Objects.requireNonNullElse(language, Language.EN);

      return templateFacade.getTemplate(nonNullLanguage, templateCode);
   }
}
