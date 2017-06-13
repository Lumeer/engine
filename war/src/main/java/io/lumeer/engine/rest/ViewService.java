/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.ViewFacade;
import io.lumeer.engine.rest.dao.ViewMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Manipulates with data views and all their attributes.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("/{organisation}/{project}/views/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ViewService {

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private SecurityFacade securityFacade;

   @PathParam("organisation")
   private String organisationCode;

   @PathParam("project")
   private String projectCode;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationCode(organisationCode);
      projectFacade.setCurrentProjectCode(projectCode);
   }

   /**
    * Gets a complete list of all views which current user can read. Filters the views by type when parameter is not empty.
    *
    * @param typeName
    *       Name of view type to filter the result (only the selected type will be returned). All views are returned when null or empty.
    * @return The list of view descriptions.
    */
   @GET
   @Path("/")
   public List<ViewMetadata> getAllViews(final @QueryParam("typeName") String typeName) {
      if (typeName == null || typeName.isEmpty()) {
         return filterViews(viewFacade.getAllViews());
      } else {
         return filterViews(viewFacade.getAllViewsOfType(typeName));
      }
   }

   private List<ViewMetadata> filterViews(List<ViewMetadata> allViews) {
      List<ViewMetadata> views = new ArrayList<>();
      String projectId = projectFacade.getCurrentProjectId();

      for (ViewMetadata v : allViews) {
         if (securityFacade.hasViewRole(projectId, v.getId(), LumeerConst.Security.ROLE_READ)) {
            views.add(v);
         }
      }

      return views;
   }

   /**
    * Creates a new view.
    *
    * @param view
    *       The view object.
    * @return Id of the newly created view.
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   @POST
   @Path("/")
   public int createView(final ViewMetadata view) throws ViewAlreadyExistsException {
      return viewFacade.createView(view.getName(), view.getType(), view.getDescription(), view.getConfiguration());
   }

   /**
    * Updates a view.
    *
    * @param view
    *       The view descriptor.
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to edit the view
    * @throws ViewAlreadyExistsException
    *       when the update contains change of view name and view with given name already exists
    */
   @PUT
   @Path("/")
   public void updateView(final ViewMetadata view) throws UnauthorizedAccessException, ViewAlreadyExistsException {
      if (!securityFacade.hasViewRole(projectCode, view.getId(), LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      ViewMetadata viewMetadata = viewFacade.getViewMetadata(view.getId());

      if (view.getName() != null && !view.getName().equals(viewMetadata.getName())) {
         viewFacade.setViewName(view.getId(), view.getName());
      }

      viewFacade.setViewConfiguration(view.getId(), view.getConfiguration());
   }

   /**
    * Configures a view attribute.
    *
    * @param id
    *       Id of the view to configure.
    * @param attribute
    *       An attribute of the view to configure.
    * @param configuration
    *       Configuration string, can specify either JSON or just a plain string.
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to edit the view
    */
   @PUT
   @Path("/{id}/configure/{attribute}")
   public void updateViewConfiguration(final @PathParam("id") int id, final @PathParam("attribute") String attribute, final String configuration) throws UnauthorizedAccessException {
      if (!securityFacade.hasViewRole(projectCode, id, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }
      try {
         Map<String, Object> configurationData = new ObjectMapper().readValue(configuration, HashMap.class);
         DataDocument configurationDocument = new DataDocument(configurationData);
         viewFacade.setViewConfigurationAttribute(id, attribute, configurationDocument);
      } catch (Throwable t) {
         viewFacade.setViewConfigurationAttribute(id, attribute, configuration);
      }
   }

   /**
    * Gets a view configuration attribute.
    *
    * @param id
    *       Id of the view to read configuration of.
    * @param attribute
    *       An attribute of the view to return.
    * @return The configuration value. Either JSON or a plain string.
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   @GET
   @Path("/{id}/configure/{attribute}")
   public Object readViewConfiguration(final @PathParam("id") int id, final @PathParam("attribute") String attribute) throws UnauthorizedAccessException {
      return viewFacade.getViewConfigurationAttribute(id, attribute);
   }

   /**
    * Copies an existing view and saves it under a new name.
    *
    * @param id
    *       Id of the view to copy.
    * @param newName
    *       The name of the newly created view.
    * @return The id of the newly created view.
    * @throws ViewAlreadyExistsException
    *       when a view with given new name already exists
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read copied view
    */
   @POST
   @Path("/{id}/clone/{newName}")
   public int cloneView(final @PathParam("id") int id, final @PathParam("newName") String newName) throws UnauthorizedAccessException, ViewAlreadyExistsException {
      return viewFacade.copyView(id, newName);
   }
}
