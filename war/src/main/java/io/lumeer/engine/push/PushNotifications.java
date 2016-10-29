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

import io.netty.util.internal.ConcurrentSet;

import java.util.Set;
import javax.ejb.Stateless;
import javax.enterprise.inject.Produces;
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

   private Set<Session> sessions = new ConcurrentSet<>();

   @Produces
   public Set<Session> getSessions() {
      return sessions;
   }

   @OnMessage
   public String receiveMessage(final String message, final Session session) {
      return "";
   }

   @OnOpen
   public void open(final Session session) {
      sessions.add(session);
   }

   @OnClose
   public void close(final Session session, final CloseReason c) {
      sessions.remove(session);
   }
}