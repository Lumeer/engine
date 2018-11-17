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
package io.lumeer.remote.rest;

import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.exception.AccessForbiddenException;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Path("pusher")
public class PusherService extends AbstractService {

   private Logger log = Logger.getLogger(PusherService.class.getName());

   private static String PUSHER_APP_ID;
   private static String PUSHER_KEY;
   private static String PUSHER_SECRET;
   private static String PUSHER_CLUSTER;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

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

   @PostConstruct
   public void init() {
      PUSHER_APP_ID = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_APP_ID)).orElse("");
      PUSHER_KEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_KEY)).orElse("");
      PUSHER_SECRET = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_SECRET)).orElse("");
      PUSHER_CLUSTER = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_CLUSTER)).orElse("");
   }

   @POST
   @Path("/")
   public Response authenticateClient(@FormParam("socket_id") final String socketId, @FormParam("channel_name") final String channelName) {
      String auth = null;

      if (PUSHER_SECRET != null && !"".equals(PUSHER_SECRET) &&
            channelName != null && channelName.startsWith("private-") &&
            channelName.substring(8).equals(authenticatedUser.getCurrentUserId())) {
         auth = sign(socketId + ":" + channelName, PUSHER_SECRET);
      }

      if (auth == null) {
         throw new AccessForbiddenException("User is not authorize to join this channel.");
      }

      return Response.ok(new PusherAuthResponse(PUSHER_KEY + ":" + auth)).build();
   }

   private String sign(final String input, final String secret) {
      try {
         final Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(new SecretKeySpec(secret.getBytes(), "SHA256"));
         final byte[] digest = mac.doFinal(input.getBytes("UTF-8"));
         return Hex.encodeHexString(digest);
      } catch (final InvalidKeyException e) {
         log.log(Level.WARNING, "Invalid secret key", e);
      } catch (final NoSuchAlgorithmException e) {
         log.log(Level.WARNING, "The Pusher HTTP client requires HmacSHA256 support", e);
      } catch (final UnsupportedEncodingException e) {
         log.log(Level.WARNING, "The Pusher HTTP client needs UTF-8 support", e);
      }

      return null;
   }
}
