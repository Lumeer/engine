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

import io.lumeer.api.model.Feedback;

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

import java.sql.Date;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class FeedbackCodec implements CollectibleCodec<Feedback> {

   public static final String ID = "_id";
   public static final String USER_ID = "userId";
   public static final String CREATION_TIME = "creationTime";
   public static final String MESSAGE = "message";

   private final Codec<Document> documentCodec;

   public FeedbackCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Feedback generateIdIfAbsentFromDocument(final Feedback feedback) {
      if (!documentHasId(feedback)) {
         feedback.setId(new ObjectId().toHexString());
      }
      return feedback;
   }

   @Override
   public boolean documentHasId(final Feedback feedback) {
      return feedback.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final Feedback feedback) {
      if (!documentHasId(feedback)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(feedback.getId()));
   }

   @Override
   public Feedback decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String userId = bson.getString(USER_ID);
      ZonedDateTime creationTime = ZonedDateTime.ofInstant(bson.getDate(CREATION_TIME).toInstant(), ZoneOffset.UTC);
      String message = bson.getString(MESSAGE);

      return new Feedback(id, userId, creationTime, message);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final Feedback feedback, final EncoderContext encoderContext) {
      Document bson = feedback.getId() != null ? new Document(ID, new ObjectId(feedback.getId())) : new Document();
      bson.append(USER_ID, feedback.getUserId())
          .append(CREATION_TIME, Date.from(feedback.getCreationTime().toInstant()))
          .append(MESSAGE, feedback.getMessage());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<Feedback> getEncoderClass() {
      return Feedback.class;
   }
}
