package io.lumeer.engine.util;

import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.InvalidQueryException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.VersionUpdateConflictException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {

   @Override
   public Response toResponse(final Exception e) {

      // 400 - BAD REQUEST
      if (e instanceof UserCollectionAlreadyExistsException || e instanceof CollectionAlreadyExistsException ||
            e instanceof AttributeNotFoundException || e instanceof AttributeAlreadyExistsException ||
            e instanceof InvalidQueryException || e instanceof InvalidDocumentKeyException ||
            e instanceof UnsuccessfulOperationException || e instanceof InvalidDocumentKeyException) {
         return Response.status(Response.Status.BAD_REQUEST).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 404 - NOT FOUND
      if (e instanceof CollectionNotFoundException || e instanceof CollectionMetadataDocumentNotFoundException ||
            e instanceof DocumentNotFoundException) {
         return Response.status(Response.Status.NOT_FOUND).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // 500 - INTERNAL SERVER ERROR
      if (e instanceof VersionUpdateConflictException) {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // ILLEGAL ARGUMENT
      if (e instanceof IllegalArgumentException) {
         return Response.status(Response.Status.BAD_REQUEST).entity("Illegal argument.").type(MediaType.TEXT_PLAIN).build();
      }

      if (e instanceof UnsupportedOperationException) {
         return Response.status(Response.Status.BAD_REQUEST).entity(e.getLocalizedMessage()).type(MediaType.TEXT_PLAIN).build();
      }

      // return Response.status(Response.Status.CONFLICT).type(MediaType.TEXT_PLAIN).build();
      return null;
   }
}
