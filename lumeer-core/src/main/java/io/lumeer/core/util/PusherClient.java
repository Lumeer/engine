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
import com.pusher.rest.Pusher;
import com.pusher.rest.data.Event;
import com.pusher.rest.data.Result;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends events to Pusher in a background thread so that requests do not wait for the Pusher API.
 * The events are sent one batch after another in the order they were triggered.
 */
public class PusherClient {

   private static final Logger log = Logger.getLogger(PusherClient.class.getName());

   private static final Map<Integer, PusherClient> cache = new LRUMap<>(10);

   // Pusher accepts at most 10 events in one batch request
   private static final int MAX_BATCH_SIZE = 10;

   // Pusher limits a message to 10 kB, keep some space for the surrounding data
   private static final int MAX_DATA_LENGTH = 9_000;

   // appended to the event name when the backup data are sent instead of the regular data
   private static final String BACKUP_EVENT_SUFFIX = ":ALT";

   // batches waiting to be sent, the oldest ones are dropped when Pusher does not keep up
   private static final int MAX_QUEUED_BATCHES = 1_000;

   // the sender thread ends when idle so that it does not outlive a redeployment
   private static final long SENDER_KEEP_ALIVE_SECONDS = 60;

   private Pusher pusher;
   private ObjectMapper mapper;
   private final ThreadPoolExecutor sender;

   private String secret;
   private String key;

   public static PusherClient getInstance(final DefaultConfigurationProducer configurationProducer) {
      String pusherAppId = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_APP_ID)).orElse("");
      String pusheyKey = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_KEY)).orElse("");
      String pusherSecret = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_SECRET)).orElse("");
      String pusherCluster = Optional.ofNullable(configurationProducer.get(DefaultConfigurationProducer.PUSHER_CLUSTER)).orElse("");

      if (StringUtils.isNotEmpty(pusherSecret)) {
         int hash = (pusherAppId + pusheyKey + pusherSecret + pusheyKey).hashCode();

         return cache.computeIfAbsent(hash, k -> new PusherClient(pusherAppId, pusheyKey, pusherSecret, pusherCluster));
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

      pusher.setDataMarshaller(o -> o instanceof SerializedData serializedData ? serializedData.json() : serialize(o));

      // a single thread keeps the events in order
      sender = new ThreadPoolExecutor(1, 1, SENDER_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new ArrayBlockingQueue<>(MAX_QUEUED_BATCHES), runnable -> {
         final Thread thread = new Thread(runnable, "pusher-sender");
         thread.setDaemon(true);
         return thread;
      }, (runnable, executor) -> {
         log.warning("Pusher does not keep up, dropping the oldest queued events");
         executor.getQueue().poll();
         executor.execute(runnable);
      });
      sender.allowCoreThreadTimeOut(true);
   }

   private String serialize(final Object o) {
      StringWriter sw = new StringWriter();
      try {
         mapper.writeValue(sw, o);
         return sw.toString();
      } catch (IOException e) {
         return null;
      }
   }

   /**
    * Queues the events for sending and returns immediately. The data are serialized right away,
    * so the caller can change the objects afterwards.
    */
   public void trigger(final List<? extends Event> notifications) {
      final List<Event> events = notifications.stream().map(this::serializeEvent).toList();

      ListUtils.partition(events, MAX_BATCH_SIZE).forEach(batch -> sender.execute(() -> send(batch)));
   }

   private void send(final List<Event> batch) {
      try {
         final Result result = pusher.trigger(batch);
         if (result.getStatus() != Result.Status.SUCCESS) {
            log.warning("Unable to send push notifications (" + result.getStatus() + ", HTTP " + result.getHttpStatus() + "): " + result.getMessage());
         }
      } catch (RuntimeException e) {
         log.log(Level.WARNING, "Unable to send push notifications: ", e);
      }
   }

   /**
    * Serializes the event data, using the backup data when the regular data are too large for Pusher.
    */
   private Event serializeEvent(final Event event) {
      String eventName = event.getName();
      String json = serialize(event.getData());

      if (event instanceof BackupDataEvent backupDataEvent && json != null && json.length() > MAX_DATA_LENGTH && backupDataEvent.getBackupData() != null) {
         json = serialize(backupDataEvent.getBackupData());
         eventName += BACKUP_EVENT_SUFFIX;
      }

      return new Event(event.getChannel(), eventName, new SerializedData(json), event.getSocketId());
   }

   /**
    * Already serialized event data, passed through the data marshaller unchanged.
    */
   private record SerializedData(String json) {
   }

   public String getSecret() {
      return secret;
   }

   public String getKey() {
      return key;
   }
}
