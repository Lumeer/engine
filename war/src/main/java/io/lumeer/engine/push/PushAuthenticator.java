/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
