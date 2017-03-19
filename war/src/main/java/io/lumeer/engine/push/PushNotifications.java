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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Push notification client. Can interact with browser clients.
 * Sample client is in https://github.com/Lumeer/lumeer.github.io/blob/master/push.html
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ServerEndpoint("/push/{channel}")
@Stateless
public class PushNotifications {

   @Inject
   private PushService pushService;

   @OnMessage
   public String receiveMessage(final String message, final Session session) {
      if (message != null) {
         if (message.startsWith("auth ")) {
            final String token = message.substring(5);

            if (pushService.getTokens().containsKey(token)) {
               session.getUserProperties().put(PushService.LUMEER_AUTH_HEADER, token);

               return "authenticated";
            }
         }

         // all other services need an authenticated client
         if (session.getUserProperties().containsKey(PushService.LUMEER_AUTH_HEADER)) {
            if (message.startsWith("observe ")) {
               final String objectId = message.substring(8);
               pushService.getObservedObjects().computeIfAbsent(objectId, k -> ConcurrentHashMap.newKeySet()).add(session);

               return "observing";
            }

            if (message.startsWith("unobserve ")) {
               final String objectId = message.substring(10);
               final Set<Session> sessions = pushService.getObservedObjects().get(objectId);
               if (sessions != null) {
                  sessions.remove(session);
               }

               return "not observing";
            }
         }
      }

      return "";
   }

   @OnOpen
   public void open(final Session session) {
      pushService.getSessions().add(session);
   }

   @OnClose
   public void close(final Session session, final CloseReason c) {
      // remove token from authenticated tokens
      final String token = (String) session.getUserProperties().get(PushService.LUMEER_AUTH_HEADER);
      if (token != null && !token.isEmpty()) {
         pushService.getTokens().remove(token);
      }

      // un-register all observations
      pushService.getObservedObjects().forEach((k, v) -> v.remove(session));

      // delete the session
      pushService.getSessions().remove(session);
   }
}