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
package io.lumeer.core.facade;

import io.lumeer.api.model.User;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class FreshdeskFacade {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   private Logger log = Logger.getLogger(FreshdeskFacade.class.getName());

   private static String FRESHDESK_DOMAIN;
   private static String FRESHDESK_APIKEY;

   private Set<String> limitsExceeded = new ConcurrentHashMap<String, String>().keySet();

   @PostConstruct
   public void init() {
      FRESHDESK_DOMAIN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.FRESHDESK_DOMAIN)).orElse("");
      FRESHDESK_APIKEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.FRESHDESK_APIKEY)).orElse("");
   }

   public void logTicket(final User user, final String subject, final String body) {
      if (FRESHDESK_APIKEY != null && !"".equals(FRESHDESK_APIKEY)) {

         final String ticket = "{ \"description\": \"" + body.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r") + "\", "
               + "\"subject\": \"" + subject.replaceAll("\"", "\\\"").replaceAll("\n", "").replaceAll("\r", "") + "\", "
               + "\"email\": \"" + user.getEmail() + "\", \"name\": \"" + user.getName() + "\", \"priority\": 2, \"status\": 2}";

         final Client client = ClientBuilder.newBuilder().build();
         final Response response = client.target("https://" + FRESHDESK_DOMAIN + ".freshdesk.com/api/v2/tickets")
               .request(MediaType.APPLICATION_JSON)
               .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(new String(FRESHDESK_APIKEY + ":dummyPassword").getBytes()))
               .header("Content-Type", "application/json")
               .buildPost(Entity.entity(ticket, MediaType.APPLICATION_JSON_TYPE))
               .invoke();

         try {
            response.readEntity(String.class);
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               throw new IOException("Freshdesk returned error HTTP code " + response.getStatus() + ": " + response.getStatusInfo().getReasonPhrase());
            }
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to communicate with Freshdesk:", e);
         } finally {
            client.close();
         }
      }
   }

   public void logLimitsExceeded(final User user, final String resourceName, final String organizationCode) {
      if (FRESHDESK_APIKEY != null && !"".equals(FRESHDESK_APIKEY)) {
         if (limitsExceeded.add(user.getEmail())) {
            logTicket(user, "Limits exceeded by user " + user.getEmail() + "in organization " + organizationCode,
                  "Limits exceeded on resource: " + resourceName);
         }
      }
   }
}
