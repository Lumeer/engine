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
package io.lumeer.storage.mongodb.codecs;

import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationType;
import io.lumeer.engine.api.data.DataDocument;

import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DelayedActionCodec implements CollectibleCodec<DelayedAction> {

   public static final String ID = "_id";

   private final Codec<Document> documentCodec;

   public DelayedActionCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public DelayedAction generateIdIfAbsentFromDocument(final DelayedAction document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final DelayedAction document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final DelayedAction document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }

   @Override
   public DelayedAction decode(final BsonReader reader, final DecoderContext decoderContext) {
      final Document bson = documentCodec.decode(reader, decoderContext);

      final DelayedAction action = new DelayedAction();

      final String id = bson.getObjectId(ID).toHexString();
      action.setId(id);

      if (bson.getDate(DelayedAction.CHECK_AFTER) != null) {
         ZonedDateTime checkAfter = ZonedDateTime.ofInstant(bson.getDate(DelayedAction.CHECK_AFTER).toInstant(), ZoneOffset.UTC);
         action.setCheckAfter(checkAfter);
      }

      if (bson.getDate(DelayedAction.STARTED_PROCESSING) != null) {
         ZonedDateTime startedProcessing = ZonedDateTime.ofInstant(bson.getDate(DelayedAction.STARTED_PROCESSING).toInstant(), ZoneOffset.UTC);
         action.setStartedProcessing(startedProcessing);
      }

      if (bson.getDate(DelayedAction.COMPLETED) != null) {
         ZonedDateTime completed = ZonedDateTime.ofInstant(bson.getDate(DelayedAction.COMPLETED).toInstant(), ZoneOffset.UTC);
         action.setCompleted(completed);
      }

      action.setProgress(bson.getInteger(DelayedAction.PROGRESS));
      action.setResourcePath(bson.getString(DelayedAction.RESOURCE_PATH));
      action.setInitiator(bson.getString(DelayedAction.INITIATOR));
      action.setReceiver(bson.getString(DelayedAction.RECEIVER));
      action.setCorrelationId(bson.getString(DelayedAction.CORRELATION_ID));

      if (bson.getString(DelayedAction.NOTIFICATION_TYPE) != null) {
         action.setNotificationType(NotificationType.valueOf(bson.getString(DelayedAction.NOTIFICATION_TYPE)));
      }

      if (bson.getString(DelayedAction.NOTIFICATION_CHANNEL) != null) {
         action.setNotificationChannel(NotificationChannel.valueOf(bson.getString(DelayedAction.NOTIFICATION_CHANNEL)));
      }

      Document data = bson.get(DelayedAction.DATA, Document.class);
      action.setData(new DataDocument(data == null ? new Document() : data));

      return action;
   }

   @Override
   public void encode(final BsonWriter writer, final DelayedAction value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();

      if (value.getCheckAfter() != null) {
         bson.append(DelayedAction.CHECK_AFTER, new Date(value.getCheckAfter().toInstant().toEpochMilli()));
      }
      if (value.getStartedProcessing() != null) {
         bson.append(DelayedAction.STARTED_PROCESSING, new Date(value.getStartedProcessing().toInstant().toEpochMilli()));
      }
      if (value.getCompleted() != null) {
         bson.append(DelayedAction.COMPLETED, new Date(value.getCompleted().toInstant().toEpochMilli()));
      }

      bson.append(DelayedAction.PROGRESS, value.getProgress())
          .append(DelayedAction.RESOURCE_PATH, value.getResourcePath())
          .append(DelayedAction.INITIATOR, value.getInitiator())
          .append(DelayedAction.RECEIVER, value.getReceiver());

      if (value.getNotificationType() != null) {
         bson.append(DelayedAction.NOTIFICATION_TYPE, value.getNotificationType().toString());
      }

      if (value.getNotificationChannel() != null) {
         bson.append(DelayedAction.NOTIFICATION_CHANNEL, value.getNotificationChannel().toString());
      }

      bson.append(DelayedAction.CORRELATION_ID, value.getCorrelationId())
          .append(DelayedAction.DATA, value.createIfAbsentData());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<DelayedAction> getEncoderClass() {
      return DelayedAction.class;
   }
}