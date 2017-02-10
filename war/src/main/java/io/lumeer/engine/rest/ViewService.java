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
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.ViewFacade;
import io.lumeer.engine.rest.dao.AccessRightsDao;
import io.lumeer.engine.rest.dao.ViewDao;

import org.bson.Document;

import java.util.List;
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
@Path("/views/")
@RequestScoped
public class ViewService {

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private SecurityFacade securityFacade;

   /**
    * Gets a complete list of all views. Filters the views by type when parameter is not empty.
    *
    * @param typeName
    *       Name of view type to filter the result (only the selected type will be returned). All views are returned when null or empty.
    * @return The list of view descriptions.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<ViewDao> getAllViews(final @QueryParam("typeName") String typeName) {
      if (typeName == null || typeName.isEmpty()) {
         return viewFacade.getAllViews();
      } else {
         return viewFacade.getAllViewsOfType(typeName);
      }
   }

   /**
    * Creates a new view.
    *
    * @param view
    *       The view description.
    * @return Id of the newly created view.
    */
   @POST
   @Path("/")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public int createView(final ViewDao view) throws ViewAlreadyExistsException {
      return viewFacade.createView(view.getName(), view.getType(), view.getConfiguration());
   }

   /**
    * Updates a view.
    *
    * @param view
    *       The view descriptor.
    */
   @PUT
   @Path("/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateView(final ViewDao view) throws ViewMetadataNotFoundException, UnauthorizedAccessException, ViewAlreadyExistsException {
      // TODO update view with id view.id
      DataDocument viewDocument = viewFacade.getViewMetadata(view.getId());

      if (view.getName() != null && !view.getName().equals(viewDocument.getString(LumeerConst.View.VIEW_NAME_KEY))) {
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
    */
   @PUT
   @Path("/{id}/configure/{attribute}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateViewConfiguration(final @PathParam("id") int id, final @PathParam("attribute") String attribute, final String configuration) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      DataDocument value = null;

      try {
         value = new DataDocument(Document.parse(configuration));
      } catch (Throwable t) {
         value = null;
      }

      if (value != null) {
         viewFacade.setViewConfigurationAttribute(id, attribute, value);
      } else {
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
    */
   @GET
   @Path("/{id}/configure/{attribute}")
   @Produces(MediaType.APPLICATION_JSON)
   public Object readViewConfiguration(final @PathParam("id") int id, final @PathParam("attribute") String attribute) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
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
    */
   @POST
   @Path("/{id}/clone/{newName}")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public int cloneView(final @PathParam("id") int id, final @PathParam("newName") String newName) throws ViewAlreadyExistsException, UnauthorizedAccessException, ViewMetadataNotFoundException {
      return viewFacade.copyView(id, newName);
   }

   /**
    * Gets view access rights.
    *
    * @param id
    *       The view id.
    * @return The access rights.
    */
   @GET
   @Path("/{id}/rights")
   @Produces(MediaType.APPLICATION_JSON)
   public AccessRightsDao getViewAccessRights(final @PathParam("id") int id) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      final String user = userFacade.getUserEmail();
      final DataDocument view = viewFacade.getViewMetadata(id);

      return new AccessRightsDao(securityFacade.checkForRead(view, user),
            securityFacade.checkForWrite(view, user), securityFacade.checkForExecute(view, user),
            user);
   }

   /**
    * Sets view access rights.
    *
    * @param id
    *       The view id.
    * @param accessRights
    *       The rights to set.
    */
   @PUT
   @Path("/{id}/rights")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setViewAccessRights(final @PathParam("id") int id, final AccessRightsDao accessRights) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      final String user = userFacade.getUserEmail();
      final DataDocument view = viewFacade.getViewMetadata(id);

      if (securityFacade.checkForAddRights(view, user)) {
         if (accessRights.isRead()) {
            securityFacade.setRightsRead(view, accessRights.getUserName());
         } else {
            securityFacade.removeRightsRead(view, accessRights.getUserName());
         }

         if (accessRights.isWrite()) {
            securityFacade.setRightsWrite(view, accessRights.getUserName());
         } else {
            securityFacade.removeRightsWrite(view, accessRights.getUserName());
         }

         if (accessRights.isExecute()) {
            securityFacade.setRightsExecute(view, accessRights.getUserName());
         } else {
            securityFacade.removeRightsExecute(view, accessRights.getUserName());
         }

         viewFacade.updateViewAccessRights(view);
      } else {
         throw new UnauthorizedAccessException("Cannot set user rights on this view.");
      }
   }
}
