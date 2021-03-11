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
package io.lumeer.core.util;

import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.commons.lang3.StringUtils;
import org.marvec.pusher.Pusher;
import org.marvec.pusher.data.BackupDataEvent;
import org.marvec.pusher.data.Event;
import org.marvec.pusher.data.Result;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PusherClient {

   private Pusher pusher;
   private ObjectMapper mapper;

   private String secret;
   private String key;

   public static PusherClient getInstance(final DefaultConfigurationProducer configurationProducer) {
      String pusherAppId = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_APP_ID)).orElse("");
      String pusheyKey = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_KEY)).orElse("");
      String pusherSecret = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_SECRET)).orElse("");
      String pusherCluster = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_CLUSTER)).orElse("");

      if (StringUtils.isNotEmpty(pusherSecret)) {
         return new PusherClient(pusherAppId, pusheyKey, pusherSecret, pusherCluster);
      }

      return null;
   }

   public PusherClient(final String appId, final String key, final String secret, final String cluster) {
      this.secret = secret;
      this.key = key;

      pusher = new Pusher(appId, key, secret);
      pusher.setCluster(cluster);
      pusher.setEncrypted(true);

      mapper = new ObjectMapper();
      AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
      AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
      AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
      mapper.setAnnotationIntrospector(pair);

      pusher.setDataMarshaller(o -> {
         StringWriter sw = new StringWriter();
         try {
            mapper.writeValue(sw, o);
            return sw.toString();
         } catch (IOException e) {
            return null;
         }
      });
   }

   public Result trigger(final String channel, final String eventName, final Object message) {
      return pusher.trigger(channel, eventName, message);
   }

   public Collection<Result> trigger(List<Event> notifications) {
      return pusher.trigger(notifications);
   }

   public String getSecret() {
      return secret;
   }

   public String getKey() {
      return key;
   }
}
