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

import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationFrequency;
import io.lumeer.api.model.NotificationSetting;
import io.lumeer.api.model.NotificationType;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class NotificationSettingCodec implements Codec<NotificationSetting> {

   private final Codec<Document> documentCodec;

   public NotificationSettingCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public NotificationSetting decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return convertFromDocument(bson);
   }

   public static NotificationSetting convertFromDocument(final Document bson) {
      final String notificationTypeString = bson.getString(NotificationSetting.NOTIFICATION_TYPE);
      final NotificationType notificationType = notificationTypeString != null ? NotificationType.valueOf(notificationTypeString) : null;

      final String notificationChannelString = bson.getString(NotificationSetting.NOTIFICATION_CHANNEL);
      final NotificationChannel notificationChannel = notificationChannelString != null ? NotificationChannel.valueOf(notificationChannelString) : null;

      final String notificationFrequencyString = bson.getString(NotificationSetting.NOTIFICATION_FREQUENCY);
      final NotificationFrequency notificationFrequency = notificationFrequencyString != null ? NotificationFrequency.valueOf(notificationFrequencyString) : null;

      return new NotificationSetting(notificationType, notificationChannel, notificationFrequency);
   }

   @Override
   public void encode(final BsonWriter writer, final NotificationSetting value, final EncoderContext encoderContext) {
      Document document = new Document()
            .append(NotificationSetting.NOTIFICATION_TYPE, value.getNotificationType().toString())
            .append(NotificationSetting.NOTIFICATION_CHANNEL, value.getNotificationChannel().toString())
            .append(NotificationSetting.NOTIFICATION_FREQUENCY, value.getNotificationFrequency().toString());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<NotificationSetting> getEncoderClass() {
      return NotificationSetting.class;
   }
}
