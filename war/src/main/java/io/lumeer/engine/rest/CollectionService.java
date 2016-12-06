/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.SearchFacade;
import io.lumeer.engine.controller.VersionFacade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Path("/collections")
@RequestScoped
public class CollectionService implements Serializable {

   private static final long serialVersionUID = 7581114783619845412L;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private VersionFacade versionFacade;

   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> getAllCollections() {
      try {
         return new ArrayList<>(collectionFacade.getAllCollections().keySet());
      } catch (CollectionNotFoundException e) {
         throw new InternalServerErrorException();
      } catch (CollectionMetadataNotFoundException e) {
         throw new InternalServerErrorException();
      }
   }

   @POST
   @Path("/{name}")
   public void createCollection(final @PathParam("name") String name) {
      try {
         if (name == null) {
            throw new BadRequestException();
         }
         collectionFacade.createCollection(name);
      } catch (CollectionNotFoundException e) {
         throw new InternalServerErrorException();
      } catch (CollectionMetadataNotFoundException e) {
         throw new InternalServerErrorException();
      } catch (CollectionAlreadyExistsException e) {
         throw new BadRequestException();
      } catch (UserCollectionAlreadyExistsException e) {
         throw new BadRequestException();
      }
   }

   @DELETE
   @Path("/{name}")
   public void dropCollection(final @PathParam("name") String name) {
      try {
         if (name == null) {
            throw new BadRequestException();
         }
         collectionFacade.dropCollection(name);
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      }
   }

   @POST
   @Path("/{collection-id-name}/attributes/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addAttribute(final @PathParam("collection-id-name") String collectionName, final String attributeName) {
      try {
         if (collectionName == null || attributeName == null) {
            throw new BadRequestException();
         }
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attributeName);
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      }
   }

   @DELETE
   @Path("/{collection-id-name}/attributes/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void dropAttribute(final @PathParam("collection-id-name") String collectionName, final String attributeName) {
      try {
         if (collectionName == null || attributeName == null) {
            throw new BadRequestException();
         }
         collectionFacade.dropAttribute(collectionName, attributeName);
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      } catch (AttributeNotFoundException e) {
         throw new NotFoundException();
      } catch (CollectionMetadataNotFoundException e) {
         throw new NotFoundException();
      }
   }

   @PUT
   @Path("/{collection-id-name}/attributes/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void renameAttribute(final @PathParam("collection-id-name") String collectionName, final String names) {
      // TODO: respresentation of old and newName in the request body
      final String oldName = names.split(",")[0];
      final String newName = names.split(",")[1];

      try {
         if (collectionName == null || oldName == null || newName == null
               || (collectionMetadataFacade.renameCollectionAttribute(collectionName, oldName, newName) == false)) {
            throw new BadRequestException();
         }
      } catch (CollectionMetadataNotFoundException e) {
         throw new NotFoundException();
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      }
   }

   @POST
   @Path("/{collection-id-name}/query")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public List<DataDocument> runQuery(final @PathParam("collection-id-name") String collectionName, final String filter, final String sort, final int skip, final int limit) {
      // TODO: representation of filter, sort, skip, limit in the request body
      try {
         if (collectionName == null) {
            throw new BadRequestException();
         }
         return searchFacade.search(collectionName, filter, sort, skip, limit);
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      }
   }

   // 3. note: ...able to store, read, update metadata to individual collections
   @POST
   @Path("/{collection-id-name}/meta/{attribute-name}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addCollectionMetadata(final @PathParam("collection-id-name") String collectionName, final @PathParam("attribute-name") String attributeName) {
      // TODO: value in request body?
      // TODO: which method to use?
   }

   @GET
   @Path("/{collection-id-name}/meta/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> readCollectionMetadata(final @PathParam("collection-id-name") String collectionName) {
      try {
         if (collectionName == null) {
            throw new BadRequestException();
         }
         return collectionFacade.readCollectionMetadata(collectionName);
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      }
   }

   @PUT
   @Path("/{collection-id-name}/meta/{attribute-name}")
   public void updateCollectionMetadata(final @PathParam("collection-id-name") String collectionName, final @PathParam("attribute-name") String attributeName) {
      // TODO: value in the request body?
      // TODO: which method to use?
   }

   @GET
   @Path("/{collection-id-name}/attributes/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readCollectionAttributes(final @PathParam("collection-id-name") String collectionName) {
      try {
         if (collectionName == null) {
            throw new BadRequestException();
         }
         return collectionFacade.readCollectionAttributes(collectionName);
      } catch (CollectionNotFoundException e) {
         throw new NotFoundException();
      }
   }

   // 10. note: ...able to read, update access right of individual entries and collections so that only allowed users can have access
   @GET
   @Path("/{collection-id-name}/rights")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readAccessRights(final @PathParam("collection-id-name") String collectionName) {
      // TODO: implement method in facade to manage access rights
      return null;
   }

   @PUT
   @Path("/{collection-id-name}/rights")
   public void updateAccessRights(final @PathParam("collection-id-name") String collectionName) {
      // TODO: implement method in facade to manage access rights
   }
}
