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
package io.lumeer.engine.util;

import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidCollectionAttributeTypeException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.InvalidQueryException;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.engine.api.exception.LinkAlreadyExistsException;
import io.lumeer.engine.api.exception.NullParameterException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.api.exception.VersionUpdateConflictException;
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Provider
public class LumeerExceptionMapper implements ExceptionMapper<DbException> {

   @Inject
   private Logger log;

   @Override
   public Response toResponse(final DbException e) {
      log.log(Level.INFO, "Exception while serving request: ", e);

      // 400 - BAD REQUEST
      if (e instanceof UserCollectionAlreadyExistsException || e instanceof CollectionAlreadyExistsException ||
            e instanceof AttributeNotFoundException || e instanceof AttributeAlreadyExistsException ||
            e instanceof InvalidQueryException || e instanceof InvalidDocumentKeyException ||
            e instanceof UnsuccessfulOperationException || e instanceof InvalidDocumentKeyException ||
            e instanceof NullParameterException || e instanceof LinkAlreadyExistsException ||
            e instanceof ViewAlreadyExistsException || e instanceof InvalidCollectionAttributeTypeException ||
            e instanceof InvalidValueException) {
         return Response.status(Response.Status.BAD_REQUEST).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 401 - UNAUTHORIZED
      if (e instanceof UnauthorizedAccessException) {
         return Response.status(Response.Status.NOT_FOUND).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 404 - NOT FOUND
      if (e instanceof UserCollectionNotFoundException || e instanceof CollectionNotFoundException ||
            e instanceof CollectionMetadataDocumentNotFoundException || e instanceof DocumentNotFoundException ||
            e instanceof ViewMetadataNotFoundException) {
         return Response.status(Response.Status.NOT_FOUND).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 500 - INTERNAL SERVER ERROR
      if (e instanceof VersionUpdateConflictException) {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
   }
}
