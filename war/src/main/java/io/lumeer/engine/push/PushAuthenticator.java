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
package io.lumeer.engine.push;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

/**
 * The only way a JavaScript client can be authenticated is via HTTP (i.e. WebSockets do not have this feature).
 * We need to make sure we only send push notifications via WebSockets to the clients that can
 * pass HTTP authentication. Each JavaScript client first asks for this secured resource and gets back its
 * security token. It then connects via WebSockets passing the security token. This validates the client.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("push-auth")
@ApplicationScoped
public class PushAuthenticator {

   public static final long AUTH_TIMEOUT = 10L * 1000L * 1000L * 1000L;

   @Inject
   private PushService pushService;

   @GET
   @Path("/")
   public String authenticate(@Context HttpServletRequest httpServletRequest) {
      final String token = httpServletRequest.getSession().getId() + ":" + UUID.randomUUID();
      registerToken(token);

      return token;
   }

   /**
    * Registers a token for for later client connection. Also cleans up the cache of previously registered
    * tokens.
    *
    * @param token
    *       The token to register.
    */
   private void registerToken(final String token) {
      // first remove all old tokens
      final List<String> tokensToRemove = new ArrayList<>();

      pushService.getTokens().forEach((k, v) -> {
         if (v < System.nanoTime() - AUTH_TIMEOUT) {
            tokensToRemove.add(k);
         }
      });

      tokensToRemove.forEach(pushService.getTokens()::remove);

      // register a new one
      pushService.getTokens().put(token, System.nanoTime());
   }
}
