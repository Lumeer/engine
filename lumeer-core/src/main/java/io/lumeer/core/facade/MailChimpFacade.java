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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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
import javax.xml.bind.DatatypeConverter;

@ApplicationScoped
public class MailChimpFacade {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Logger log;

   private static String MAILCHIMP_APIKEY;
   private static String MAILCHIMP_SUBDOMAIN;
   private static String MAILCHIMP_LIST_CS;
   private static String MAILCHIMP_LIST_EN;

   @PostConstruct
   public void init() {
      MAILCHIMP_APIKEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILCHIMP_APIKEY)).orElse("");
      MAILCHIMP_SUBDOMAIN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILCHIMP_SUBDOMAIN)).orElse("");
      MAILCHIMP_LIST_CS = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILCHIMP_LIST_CS)).orElse("");
      MAILCHIMP_LIST_EN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILCHIMP_LIST_EN)).orElse("");
   }

   public void setUserSubscription(final User user, final boolean enSite) {
      if (MAILCHIMP_APIKEY != null && !"".equals(MAILCHIMP_APIKEY) && user != null && user.getEmail() != null) {

         final String listId = enSite ? MAILCHIMP_LIST_EN : MAILCHIMP_LIST_CS;
         final String userId = md5sum(user.getEmail());

         if (userSubscribed(listId, userId)) {
            updateUser(listId, userId, user.hasNewsletter() != null && user.hasNewsletter());
         } else {
            subscribeUser(listId, user);
         }
      }
   }

   private String md5sum(final String original) {
      try {
         MessageDigest md = MessageDigest.getInstance("MD5");
         md.update(original.getBytes());
         return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
      } catch (NoSuchAlgorithmException nsae) {
         log.log(Level.WARNING, "This JRE suffers significant issues because it is missing MD5 algorithm:", nsae);
      }

      return null;
   }

   private boolean userSubscribed(final String listId, final String userId) {
      String response = mailChimpClient("lists/" + listId + "/members/" + userId, null, false);
      if (response.startsWith("{\"id\":\"")) {
         response = response.substring(7);
         response = response.substring(0, response.indexOf("\""));

         if (response.equals(userId)) {
            return true;
         }
      }
      return false;
   }

   private void subscribeUser(final String listId, final User user) {
      mailChimpClient("lists/" + listId + "/members", "{\"email_address\": \"" + user.getEmail() + "\", \"status\": \"" + (user.hasNewsletter() ? "" : "un") + "subscribed\", \"merge_fields\": {\"FNAME\": \"" + user.getName() + "\"}}", false);
   }

   private void updateUser(final String listId, final String userId, boolean subscribed) {
      mailChimpClient("lists/" + listId + "/members/" + userId, "{\"status\": \"" + (subscribed ? "" : "un") + "subscribed\"}", true);
   }

   private String mailChimpClient(final String path, final String body, final boolean patch) {
      final Client client = ClientBuilder.newBuilder().build();
      final Invocation.Builder builder = client.target("https://" + MAILCHIMP_SUBDOMAIN + ".api.mailchimp.com/3.0/" + path)
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", "Basic "+ Base64.getEncoder().encodeToString(new String("user:" + MAILCHIMP_APIKEY).getBytes()))
            .header("Content-Type", "application/json");
      Invocation invocation;

      if (body != null && !"".equals(body)) {
         if (patch) {
            invocation = builder.build("PATCH", Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
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
         log.log(Level.WARNING, "Unable to communicate with MailChimp:", e);
      } finally {
         client.close();
      }

      return "";
   }
}
