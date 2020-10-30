package io.lumeer.core.facade;/*
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

import io.lumeer.api.model.User;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class MailerLiteFacade implements MailerService {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Logger log;

   private static String MAILERLITE_APIKEY;
   private static String MAILERLITE_GROUP_CS;
   private static String MAILERLITE_GROUP_EN;

   @PostConstruct
   public void init() {
      MAILERLITE_APIKEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_APIKEY)).orElse("");
      MAILERLITE_GROUP_CS = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_GROUP_CS)).orElse("");
      MAILERLITE_GROUP_EN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_GROUP_EN)).orElse("");
   }

   @Override
   public void setUserSubscription(final User user, final boolean enSite) {
      if (MAILERLITE_APIKEY != null && !"".equals(MAILERLITE_APIKEY) && user != null && user.getEmail() != null) {

         if (userSubscribed(user.getEmail())) {
            updateUser(user.getEmail(), user.hasNewsletter() != null && user.hasNewsletter());
         } else {
            subscribeUser(enSite ? MAILERLITE_GROUP_EN : MAILERLITE_GROUP_CS, user);
         }
      }
   }

   private static String encodeValue(String value) {
      try {
         return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
      } catch (UnsupportedEncodingException ex) {
         return value; // we tried, moreover email characters are safe anyway
      }
   }

   private boolean userSubscribed(final String userEmail) {
      String response = mailerLiteClient("subscribers/" + encodeValue(userEmail), null, false);

      return response.startsWith("{\"id\":") && response.contains("\"email\":\"" + userEmail + "\"");
   }

   private void subscribeUser(final String groupId, final User user) {
      mailerLiteClient("groups/" + groupId + "/subscribers", "{\"email\": \"" + user.getEmail() + "\", \"name\":\"" + user.getName() + "\", \"type\": \"" + (user.hasNewsletter() ? "active" : "unsubscribed") +
            "\"}", false);
   }

   private void updateUser(final String userEmail, boolean subscribed) {
      mailerLiteClient("subscribers/" + encodeValue(userEmail), "{\"type\": \"" + (subscribed ? "active" : "unsubscribed") + "\"}", true);
   }

   private String mailerLiteClient(final String path, final String body, final boolean put) {
      final Client client = ClientBuilder.newBuilder().build();
      final Invocation.Builder builder = client.target("https://api.mailerlite.com/api/v2/" + path)
                                               .request(MediaType.APPLICATION_JSON)
                                               .header("X-MailerLite-ApiKey", MAILERLITE_APIKEY)
                                               .header("Content-Type", "application/json");
      Invocation invocation;

      if (body != null && !"".equals(body)) {
         if (put) {
            invocation = builder.build("PUT", Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
         } else {
            invocation = builder.buildPost(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
         }
      } else {
         invocation = builder.buildGet();
      }

      Response response = invocation.invoke();
      try {
         return response.readEntity(String.class);
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to communicate with MailerLite:", e);
      } finally {
         client.close();
      }

      return "";
   }

}
