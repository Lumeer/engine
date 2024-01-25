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

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class MailerLiteFacade implements MailerService {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Logger log;

   final private static String ERROR_MESSAGE = "Unable to communicate with MailerLite:";

   private static String MAILERLITE_APIKEY;
   private static String MAILERLITE_GROUP_CS;
   private static String MAILERLITE_GROUP_EN;
   private static String MAILERLITE_NEWSLETTER_CS;
   private static String MAILERLITE_NEWSLETTER_EN;

   @PostConstruct
   public void init() {
      MAILERLITE_APIKEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_APIKEY)).orElse("");
      MAILERLITE_GROUP_CS = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_GROUP_CS)).orElse("");
      MAILERLITE_GROUP_EN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_GROUP_EN)).orElse("");
      MAILERLITE_NEWSLETTER_CS = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_NEWSLETTER_CS)).orElse("");
      MAILERLITE_NEWSLETTER_EN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.MAILERLITE_NEWSLETTER_EN)).orElse("");
   }

   @Override
   public void setUserSubscription(final User user, final boolean enSite) {
      if (StringUtils.isNotEmpty(MAILERLITE_APIKEY) && user != null && user.getEmail() != null) {

         final String welcomeGroup = enSite ? MAILERLITE_GROUP_EN : MAILERLITE_GROUP_CS;
         final String newsletterGroup = enSite ? MAILERLITE_NEWSLETTER_EN : MAILERLITE_NEWSLETTER_CS;

         final SubscribedUser subscribedUser = userSubscribed(user.getEmail());
         final String fields = "\"goto_news\": \"" + (user.hasNewsletter() != null && user.hasNewsletter() ? "news" : "no") + "\"";

         if (subscribedUser == null) { // new user is getting subscribed
            // user is subscribed as active
            // user is added to app welcome sequence
            // goto_news filed is set to "news" if newsletter is opted in
            // user is added to newsletter group by a MailerLite automation once they go through the welcome sequence

            final String groups = "\"" + welcomeGroup + "\"";

            upsertUser(user, fields, groups);
         } else {
            // update newsletter group and goto_news field according to subscription status
            String groups = "";

            // if the user is subscribed to the newsletter but is still in the welcome sequence, do nothing with the groups
            // if the user is subscribed to the newsletter and is not in the welcome sequence, add them to the newsletter group
            if (user.hasNewsletter() != null && user.hasNewsletter()) {
               if (!subscribedUser.groups.contains(welcomeGroup)) {
                  groups = "\"" + newsletterGroup + "\"";
               }
            }

            // if the user is in the welcome sequence, keep them there
            if (subscribedUser.groups.contains(welcomeGroup)) {
               if (StringUtils.isNotEmpty(groups)) {
                  groups += ", ";
               }
               groups += "\"" + welcomeGroup + "\"";
            }

            updateUser(subscribedUser.id, user, fields, groups);
         }
      }
   }

   @Override
   public void setUserTemplate(final User user, final String template) {
      if (StringUtils.isNotEmpty(MAILERLITE_APIKEY) && user != null && StringUtils.isNotEmpty(user.getEmail())) {
         final String fields = "\"template\": \"" + encodeValue(template) + "\"";

         upsertUser(user, fields, "");
      }
   }

   private static String encodeValue(String value) {
      try {
         return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
      } catch (UnsupportedEncodingException ex) {
         return value; // we tried, moreover email characters are safe anyway
      }
   }

   @SuppressWarnings("unchecked")
   private SubscribedUser userSubscribed(final String userEmail) {
      String response = mailerLiteClient("subscribers/" + encodeValue(userEmail), null, false, true);

      JSONParser parser = new JSONParser();
      try {
         JSONObject jsonObject = (JSONObject) parser.parse(response);
         if (jsonObject.containsKey("data")) {
            final String id = (String) ((JSONObject) jsonObject.get("data")).get("id");
            final List<String> groups = ((Stream<JSONObject>) ((JSONArray) ((JSONObject) jsonObject.get("data")).get("groups")).stream()).map(o -> (String) o.get("id")).collect(Collectors.toList());
            return new SubscribedUser(id, groups);
         } else {
            return null;
         }
      } catch (Throwable e) {
         return null;
      }
   }

   private void upsertUser(final User user, final String fields, final String groups) {
      mailerLiteClient("subscribers", "{\"email\": \"" + user.getEmail() +
            "\", \"fields\": {\"name\":\"" + user.getName() + "\"" + (StringUtils.isNotEmpty(fields) ? ", " + fields : "") +
            "}" + (StringUtils.isNotEmpty(groups) ? ", \"groups\": [" + groups + "]" : "") +
            ", \"status\": \"active\"}", false, false);
   }

   private void updateUser(final String userMailreLiteId, final User user, final String fields, final String groups) {
      mailerLiteClient("subscribers/" + encodeValue(userMailreLiteId), "{\"fields\": {\"name\":\"" + user.getName() + "\"" + (StringUtils.isNotEmpty(fields) ? ", " + fields : "") +
            "}" + ", \"groups\": [" + groups + "]" +
            ", \"status\": \"active\"}", true, false);
   }

   private String mailerLiteClient(final String path, final String body, final boolean put, final boolean blocking) {
      final Client client = ClientBuilder.newBuilder().build();
      final Invocation.Builder builder = client.target("https://connect.mailerlite.com/api/" + path)
                                               .request(MediaType.APPLICATION_JSON)
                                               .header("Authorization", "Bearer " + MAILERLITE_APIKEY)
                                               .header("Content-Type", "application/json");
      Invocation invocation;

      if (StringUtils.isNotEmpty(body)) {
         if (put) {
            invocation = builder.build("PUT", Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
         } else {
            invocation = builder.buildPost(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
         }
      } else {
         invocation = builder.buildGet();
      }

      if (blocking) {
         return blockingCall(invocation, client);
      } else {
         asyncCall(invocation, client);
         return null;
      }
   }

   private String blockingCall(final Invocation invocation, final Client client) {
      final Response response = invocation.invoke();

      try {
         return response.readEntity(String.class);
      } catch (Exception e) {
         log.log(Level.WARNING, ERROR_MESSAGE, e);
      } finally {
         client.close();
      }

      return "";
   }

   private void asyncCall(final Invocation invocation, final Client client) {
      final Future<Response> response = invocation.submit();

      new Thread(() -> {
         try {
            final Response resp = response.get();
            if (!resp.getStatusInfo().equals(Response.Status.OK)) {
               throw new IllegalStateException("Response status is not ok: " + resp.getStatusInfo().toString());
            }
         } catch (Exception e) {
            log.log(Level.WARNING, ERROR_MESSAGE, e);
         } finally {
            client.close();
         }
      }).start();
   }

   public record SubscribedUser(String id, List<String> groups) { }
}
