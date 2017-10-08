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