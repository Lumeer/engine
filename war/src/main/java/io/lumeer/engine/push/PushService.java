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

import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.push.PushMessage;
import io.lumeer.engine.controller.UserFacade;

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
   private UserFacade userFacade;

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
      publishMessage(userFacade.getUserSessionId(), channel, message);
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
