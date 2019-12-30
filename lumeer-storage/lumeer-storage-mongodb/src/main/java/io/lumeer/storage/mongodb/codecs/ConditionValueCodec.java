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

import io.lumeer.api.model.ConditionValue;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ConditionValueCodec implements Codec<ConditionValue> {

   public static final String TYPE = "type";
   public static final String VALUE = "value";

   private final Codec<Document> documentCodec;

   public ConditionValueCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public ConditionValue decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      final Document bson = documentCodec.decode(bsonReader, decoderContext);

      return convertFromDocument(bson);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final ConditionValue value, final EncoderContext encoderContext) {
      final Document bson = new Document()
            .append(TYPE, value.getType())
            .append(VALUE, value.getValue());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   public static ConditionValue convertFromDocument(final Document bson) {
      final String type = bson.getString(TYPE);
      final Object value = bson.get(VALUE);

      return new ConditionValue(type, value);
   }

   @Override
   public Class<ConditionValue> getEncoderClass() {
      return ConditionValue.class;
   }
}
