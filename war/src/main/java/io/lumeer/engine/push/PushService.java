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

import io.lumeer.core.AuthenticatedUser;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.push.PushMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;
import javax.websocket.EncodeException;
import javax.websocket.Session;

/**
 * Sends message to clients using web sockets.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class PushService {

   /**
    * WebSocket session header that carries the authentication token.
    */
   public static final String LUMEER_AUTH_HEADER = "lumeer.auth";

   @Inject
   private PushNotifications pushNotifications;

   @Inject
   private Logger log;

   @Inject
   private AuthenticatedUser authenticatedUser;

   /**
    * Currently opened sessions with clients.
    */
   private Set<Session> sessions = ConcurrentHashMap.newKeySet();

   /**
    * Authentication tokens of clients authenticated via HTTP.
    */
   private Map<String, Long> tokens = new ConcurrentHashMap<>();

   /**
    * Clients registered to observe given objects.
    */
   private Map<String, Set<Session>> observedObjects = new ConcurrentHashMap<>();

   public Set<Session> getSessions() {
      return sessions;
   }

   public Map<String, Long> getTokens() {
      return tokens;
   }

   public Map<String, Set<Session>> getObservedObjects() {
      return observedObjects;
   }

   /**
    * Sends push-notification message to WebSocket clients listening on the given channel.
    *
    * @param channel
    *       Channel suffix to send to or empty to send to all channels.
    * @param message
    *       The message to be sent.
    */
   public void publishMessage(final String channel, final String message) {
      sessions.forEach(session -> {
         try {
            log.info(session.getRequestURI().toString());
            if (channel == null || channel.isEmpty() || session.getRequestURI().toString().endsWith(channel)) {
               if (session.getUserProperties().containsKey(LUMEER_AUTH_HEADER)) {
                  session.getBasicRemote().sendText(message);
               }
            }
         } catch (IOException e) {
            log.log(Level.FINE, "Unable to send push notification: ", e);
         }
      });
   }

   /**
    * Sends push-notification message to WebSocket clients listening on the given channel.
    *
    * @param channel
    *       Channel suffix to send to or empty to send to all channels.
    * @param message
    *       The message to be sent.
    */
   public void publishMessage(final String channel, final Object message) {
      sessions.forEach(session -> {
         try {
            if (channel == null || channel.isEmpty() || session.getRequestURI().toString().endsWith(channel)) {
               if (session.getUserProperties().containsKey(LUMEER_AUTH_HEADER)) {
                  session.getBasicRemote().sendObject(message);
               }
            }
         } catch (IOException | EncodeException e) {
            log.log(Level.FINE, "Unable to send push notification: ", e);
         }
      });
   }

   /**
    * Sends push-notification message to a single WebSocket client listening on the given channel.
    *
    * @param clientSession
    *       Session ID of the client to send the message to.
    * @param channel
    *       Channel suffix to send to or empty to send to all channels.
    * @param message
    *       The message to be sent.
    */
   public void publishMessage(final String clientSession, final String channel, final PushMessage message) {
      sessions.forEach(session -> {
         if (session.getId() != null && session.getId().equals(clientSession)) {
            try {
               if (channel == null || channel.isEmpty() || session.getRequestURI().toString().endsWith(channel)) {
                  if (session.getUserProperties().containsKey(LUMEER_AUTH_HEADER)) {
                     session.getBasicRemote().sendText(message.toString());
                  }
               }
            } catch (IOException e) {
               log.log(Level.FINE, "Unable to send push notification: ", e);
            }
         }
      });
   }

   /**
    * Sends push-notification message to the current WebSocket client listening on the given channel.
    *
    * @param channel
    *       Channel suffix to send to or empty to send to all channels.
    * @param message
    *       The message to be sent.
    */
   public void publishMessageToCurrentUser(final String channel, final PushMessage message) {
      publishMessage(authenticatedUser.getUserSessionId(), channel, message);
   }

   /**
    * Notifies all WebSocket clients observing given document.
    *
    * @param event
    *       The document event with information about change.
    */
   public void onDocumentEvent(@Observes(notifyObserver = Reception.ALWAYS) final DocumentEvent event) {
      final String objectId = event.getDocument().get("_id").toString();

      if (objectId != null && !objectId.isEmpty() && observedObjects.containsKey(objectId)) {
         observedObjects.get(objectId).forEach(session -> {
            try {
               session.getBasicRemote().sendObject(event);
            } catch (IOException | EncodeException e) {
               log.log(Level.SEVERE, "Unable to notify WebSocket client: ", e);
            }
         });
      }
   }
}
