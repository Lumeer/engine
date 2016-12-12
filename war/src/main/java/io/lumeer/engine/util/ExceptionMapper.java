package io.lumeer.engine.util;

import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidQueryException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {

   @Override
   public Response toResponse(final Exception e) {
      // 400 - BAD REQUEST
      if (e instanceof UserCollectionAlreadyExistsException || e instanceof CollectionAlreadyExistsException || e instanceof AttributeNotFoundException || e instanceof AttributeAlreadyExistsException || e instanceof InvalidQueryException) {
         return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 404 - NOT FOUND
      if (e instanceof CollectionNotFoundException || e instanceof CollectionMetadataDocumentNotFoundException || e instanceof DocumentNotFoundException) {
         return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // ILLEGAL ARGUMENT
      if (e instanceof IllegalArgumentException) {
         return Response.status(Response.Status.BAD_REQUEST).entity("Wrong argument.").type(MediaType.TEXT_PLAIN).build();
      }

      return null;
   }
}
