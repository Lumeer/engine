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

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
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

   @Inject
   private Set<Session> sessions;

   @Inject
   private Logger log;

   public void publishMessage(final String channel, final String message) {
      sessions.forEach(session -> {
         try {
            session.getBasicRemote().sendText(message);
         } catch (IOException e) {
            log.log(Level.FINE, "Unable to send push notification: ", e);
         }
      });
   }

   public void publishMessage(final String channel, final Object message) {
      sessions.forEach(session -> {
         try {
            if (channel == null || channel.isEmpty() || session.getRequestURI().toString().endsWith(channel)) {
               session.getBasicRemote().sendObject(message);
            }
         } catch (IOException | EncodeException e) {
            log.log(Level.FINE, "Unable to send push notification: ", e);
         }
      });
   }
}
