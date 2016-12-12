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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.rest.dao.ViewDao;

import org.bson.Document;

import java.util.List;
import javax.enterprise.context.RequestScoped;
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
         // TODO return all available views
      } else {
         // TODO return only views of the given type
      }

      return null;
   }

   /**
    * Creates a new view.
    *
    * @param view
    *       The view description.
    * @return Id of the newly created view.
    */
   @PUT
   @Path("/")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public int createView(final ViewDao view) {
      // TODO create view
      // TODO return ID of the newly created view
      return 0;
   }

   /**
    * Updates a view.
    *
    * @param view
    *       The view descriptor.
    */
   @POST
   @Path("/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateView(final ViewDao view) {
      // TODO update view with id view.id
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
   @POST
   @Path("/{id}/configure/{attribute}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateViewConfiguration(final @PathParam("id") int id, final @PathParam("attribute") String attribute, final String configuration) {
      try {
         final DataDocument value = new DataDocument(Document.parse(configuration));
         // TODO it is a document, update configuration - ViewDao.getConfiguration().put(attribute, value)
      } catch (Throwable t) {
         // TODO it is not a JSON, simply use configuration as a string - ViewDao.getConfiguration().put(attribute, configuration)
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
   public Object readViewConfiguration(final @PathParam("id") int id, final @PathParam("attribute") String attribute) {
      // TODO return ViewDao.getConfiguration().get(attribute)
      return null;
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
   @PUT
   @Path("/{id}/clone/{newName}")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public int cloneView(final @PathParam("id") int id, final @PathParam("newName") String newName) {
      // TODO copy the view with id under a new name and return the new id

      return 0;
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
   public String getViewAccessRights(final @PathParam("id") int id) {
      // TODO return access rights

      return "";
   }

   /**
    * Sets view access rights.
    *
    * @param id
    *       The view id.
    * @param accessRights
    *       The rights to set.
    */
   @POST
   @Path("/{id}/rights")
   @Produces(MediaType.APPLICATION_JSON)
   public void setViewAccessRights(final @PathParam("id") int id, final String accessRights) {
      // TODO set access rights
   }
}
