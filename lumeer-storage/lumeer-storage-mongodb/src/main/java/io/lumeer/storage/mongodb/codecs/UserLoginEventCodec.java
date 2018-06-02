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

package io.lumeer.storage.mongodb.codecs;

import io.lumeer.api.model.UserLoginEvent;

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

import java.util.Date;

public class UserLoginEventCodec implements CollectibleCodec<UserLoginEvent> {

   public static final String ID = "_id";
   public static final String USER_ID = "userId";
   public static final String DATE = "date";

   private final Codec<Document> documentCodec;

   public UserLoginEventCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public UserLoginEvent generateIdIfAbsentFromDocument(final UserLoginEvent userLoginEvent) {
      if (!documentHasId(userLoginEvent)) {
         userLoginEvent.setId(new ObjectId().toHexString());
      }
      return userLoginEvent;
   }

   @Override
   public boolean documentHasId(final UserLoginEvent userLoginEvent) {
      return userLoginEvent.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final UserLoginEvent userLoginEvent) {
      if (!documentHasId(userLoginEvent)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(userLoginEvent.getId()));
   }

   @Override
   public UserLoginEvent decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String userId = bson.getString(USER_ID);
      Date date = bson.getDate(DATE);

      UserLoginEvent userLoginEvent = new UserLoginEvent(id, userId, date);

      return userLoginEvent;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final UserLoginEvent userLoginEvent, final EncoderContext encoderContext) {
      Document bson = userLoginEvent.getId() != null ? new Document(ID, new ObjectId(userLoginEvent.getId())) : new Document();
      bson.append(USER_ID, userLoginEvent.getUserId())
          .append(DATE, userLoginEvent.getDate());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<UserLoginEvent> getEncoderClass() {
      return UserLoginEvent.class;
   }
}

