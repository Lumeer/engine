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
package io.lumeer.core.facade;

import io.lumeer.api.model.User;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class EventLogFacade {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Logger log;

   private Client client;

   private static String EVENT_LOG_URL;
   private static String PRIORITY_EVENT_LOG_URL;

   @PostConstruct
   public void init() {
      EVENT_LOG_URL = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.EVENT_LOG_URL)).orElse("");
      PRIORITY_EVENT_LOG_URL = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PRIORITY_EVENT_LOG_URL)).orElse("");

      if (StringUtils.isNotEmpty(EVENT_LOG_URL) || StringUtils.isNotEmpty(PRIORITY_EVENT_LOG_URL)) {
         client = ClientBuilder.newBuilder().build();
      }
   }

   @PreDestroy
   public void destroy() {
      if (client != null) {
         client.close();
      }
   }

   public void logEvent(final User user, final String message) {
      if (StringUtils.isNotEmpty(EVENT_LOG_URL)) {
         sendMessage(String.format("[%s] %s: %s", defaultConfigurationProducer.getEnvironment().toString(), formatUser(user), message), false);
      }
   }

   public void logPriorityEvent(final User user, final String message) {
      if (StringUtils.isNotEmpty(PRIORITY_EVENT_LOG_URL)) {
         sendMessage(String.format("[%s] %s: %s", defaultConfigurationProducer.getEnvironment().toString(), formatUser(user), message), true);
      }
   }

   private void sendMessage(final String message, final boolean priority) {
      final Future<Response> response = client.target(priority ? PRIORITY_EVENT_LOG_URL : EVENT_LOG_URL)
                                              .request(MediaType.APPLICATION_JSON)
                                              .buildPost(Entity.json(new EventLogMessage(message)))
                                              .submit();

      new Thread(() -> {
         try {
            final Response resp = response.get();
            if (!resp.getStatusInfo().equals(Response.Status.OK)) {
               throw new IllegalStateException("Response status is not ok: " + resp.getStatusInfo().toString());
            }
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to log event message: ", e);
         }
      }).start();
   }

   private String formatUser(final User user) {
      if (user == null) {
         return "UNKNOWN USER";
      }

      return String.format("%s (%s)", user.getName(), user.getEmail());
   }

   public record EventLogMessage(String text) {
         @JsonCreator
         public EventLogMessage(@JsonProperty("text") final String text) {
            this.text = text;
         }
      }
}
