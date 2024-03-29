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

import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.exception.AccessForbiddenException;
import io.lumeer.core.facade.PusherFacade;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Path("pusher")
public class PusherService extends AbstractService {

   @Inject
   private Logger log;

   @Inject
   private PusherFacade pusherFacade;

   @Inject
   private AuthenticatedUser authenticatedUser;

   public static final class PusherAuthResponse {
      private String auth;

      public PusherAuthResponse(final String auth) {
         this.auth = auth;
      }

      public String getAuth() {
         return auth;
      }

      public void setAuth(final String auth) {
         this.auth = auth;
      }
   }

   @POST
   @Path("/")
   public Response authenticateClient(@FormParam("socket_id") final String socketId, @FormParam("channel_name") final String channelName) {
      String auth = null;

      if (StringUtils.isNotEmpty(pusherFacade.getPusherSecret()) &&
            channelName != null && channelName.startsWith("private-") &&
            channelName.substring(8).equals(authenticatedUser.getCurrentUserId())) {
         auth = sign(socketId + ":" + channelName, pusherFacade.getPusherSecret());
      }

      if (auth == null) {
         throw new AccessForbiddenException("User is not authorized to join this channel.");
      }

      return Response.ok(new PusherAuthResponse(pusherFacade.getPusherKey() + ":" + auth)).build();
   }

   private String sign(final String input, final String secret) {
      try {
         final Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(new SecretKeySpec(secret.getBytes(), "SHA256"));
         final byte[] digest = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
         return Hex.encodeHexString(digest);
      } catch (final InvalidKeyException e) {
         log.log(Level.WARNING, "Invalid secret key", e);
      } catch (final NoSuchAlgorithmException e) {
         log.log(Level.WARNING, "The Pusher HTTP client requires HmacSHA256 support", e);
      }

      return null;
   }
}
