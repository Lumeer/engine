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

import io.lumeer.api.model.ConstraintObject;
import io.lumeer.api.model.ConstraintType;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ConstraintCodec implements Codec<ConstraintObject> {

   public static final String TYPE = "type";
   public static final String CONFIG = "config";

   private final Codec<Document> documentCodec;

   public ConstraintCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public ConstraintObject decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      return ConstraintCodec.convertFromDocument(bson);
   }

   public static ConstraintObject convertFromDocument(final Document document) {
      if (document == null) {
         return null;
      }

      ConstraintType type = ConstraintType.valueOf(document.getString(TYPE));
      Object config = document.get(CONFIG);

      return new ConstraintObject(type, config);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final ConstraintObject constraint, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(TYPE, constraint.getType().toString())
            .append(CONFIG, constraint.getConfig());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<ConstraintObject> getEncoderClass() {
      return ConstraintObject.class;
   }
}
