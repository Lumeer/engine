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

import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.UserNotification;
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

public class UserNotificationCodec implements CollectibleCodec<UserNotification> {

   public static final String ID = "_id";
   public static final String USER_ID = "userId";
   public static final String CREATED_AT = "createdAt";
   public static final String FIRST_READ_AT = "firstReadAt";
   public static final String READ = "read";
   public static final String DATA = "data";
   public static final String TYPE = "type";

   private final Codec<Document> documentCodec;

   public UserNotificationCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public UserNotification generateIdIfAbsentFromDocument(final UserNotification document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final UserNotification document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final UserNotification document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }

   @Override
   public UserNotification decode(final BsonReader reader, final DecoderContext decoderContext) {
      final Document bson = documentCodec.decode(reader, decoderContext);

      final String id = bson.getObjectId(ID).toHexString();
      final String userId = bson.getString(USER_ID);
      final boolean read = bson.getBoolean(READ);
      final NotificationType type = NotificationType.values()[bson.getInteger(TYPE)];
      final org.bson.Document data = bson.get(DATA, org.bson.Document.class);

      ZonedDateTime createdAt = null;
      if (bson.getDate(CREATED_AT) != null) {
         createdAt = ZonedDateTime.ofInstant(bson.getDate(CREATED_AT).toInstant(), ZoneOffset.UTC);
      }

      ZonedDateTime firstReadAt = null;
      if (bson.getDate(FIRST_READ_AT) != null) {
         firstReadAt = ZonedDateTime.ofInstant(bson.getDate(FIRST_READ_AT).toInstant(), ZoneOffset.UTC);
      }

      final UserNotification notification = new UserNotification(userId, createdAt, read, firstReadAt, type, new DataDocument(data != null ? data : new org.bson.Document()));
      notification.setId(id);

      return notification;
   }

   @Override
   public void encode(final BsonWriter writer, final UserNotification value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(USER_ID, value.getUserId())
          .append(READ, value.isRead())
          .append(DATA, value.getData())
          .append(TYPE, value.getType().ordinal());

      if (value.getFirstReadAt() != null) {
         bson.append(FIRST_READ_AT, new Date(value.getFirstReadAt().toInstant().toEpochMilli()));
      }
      if (value.getCreatedAt() != null) {
         bson.append(CREATED_AT, new Date(value.getCreatedAt().toInstant().toEpochMilli()));
      }

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<UserNotification> getEncoderClass() {
      return UserNotification.class;
   }
}
