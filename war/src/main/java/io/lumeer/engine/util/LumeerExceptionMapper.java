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
package io.lumeer.engine.util;

import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.exception.NoSystemPermissionException;
import io.lumeer.core.exception.PaymentGatewayException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidCollectionAttributeTypeException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.InvalidQueryException;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.engine.api.exception.LinkAlreadyExistsException;
import io.lumeer.api.exception.LumeerException;
import io.lumeer.engine.api.exception.NullParameterException;
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
public class LumeerExceptionMapper implements ExceptionMapper<LumeerException> {

   @Inject
   private Logger log;

   @Override
   public Response toResponse(final LumeerException e) {
      log.log(Level.SEVERE, "Exception while serving request: ", e);

      // 400 - BAD REQUEST
      if (e instanceof UserCollectionAlreadyExistsException || e instanceof CollectionAlreadyExistsException ||
            e instanceof AttributeNotFoundException || e instanceof AttributeAlreadyExistsException ||
            e instanceof InvalidQueryException || e instanceof InvalidDocumentKeyException ||
            e instanceof UnsuccessfulOperationException || e instanceof InvalidDocumentKeyException ||
            e instanceof NullParameterException || e instanceof LinkAlreadyExistsException ||
            e instanceof ViewAlreadyExistsException || e instanceof InvalidCollectionAttributeTypeException ||
            e instanceof InvalidValueException || e instanceof BadFormatException) {
         return Response.status(Response.Status.BAD_REQUEST).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 403 - FORBIDDEN
      if (e instanceof NoPermissionException || e instanceof NoSystemPermissionException) {
         return Response.status(Response.Status.FORBIDDEN).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 404 - NOT FOUND
      if (e instanceof UserCollectionNotFoundException || e instanceof CollectionNotFoundException ||
            e instanceof CollectionMetadataDocumentNotFoundException || e instanceof DocumentNotFoundException ||
            e instanceof ViewMetadataNotFoundException || e instanceof ResourceNotFoundException) {
         return Response.status(Response.Status.NOT_FOUND).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 500 - INTERNAL SERVER ERROR
      if (e instanceof VersionUpdateConflictException) {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      if (e instanceof PaymentGatewayException) {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while communicating with the payment gateway.").type(MediaType.TEXT_PLAIN).build();
      }

      if (e instanceof ServiceLimitsExceededException) {
         return Response.status(Response.Status.PAYMENT_REQUIRED).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
   }
}
