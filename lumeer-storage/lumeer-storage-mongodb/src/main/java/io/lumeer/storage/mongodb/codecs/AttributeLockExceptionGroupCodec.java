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

import io.lumeer.api.model.AttributeFilterEquation;
import io.lumeer.api.model.AttributeLockExceptionGroup;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class AttributeLockExceptionGroupCodec implements Codec<AttributeLockExceptionGroup> {

   public static final String EQUATION = "equation";
   public static final String TYPE_VALUE = "typeValue";
   public static final String TYPE = "type";

   private final Codec<Document> documentCodec;

   public AttributeLockExceptionGroupCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public AttributeLockExceptionGroup decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeLockExceptionGroupCodec.convertFromDocument(bson);
   }

   public static AttributeLockExceptionGroup convertFromDocument(final Document document) {
      AttributeFilterEquation equation = AttributeFilterEquationCodec.convertFromDocument(document.get(EQUATION, Document.class));
      List<String> typeValue = document.getList(TYPE_VALUE, String.class);
      String type = document.getString(TYPE);

      return new AttributeLockExceptionGroup(equation, typeValue, type);
   }

   @Override
   public void encode(final BsonWriter writer, final AttributeLockExceptionGroup value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(EQUATION, value.getEquation())
            .append(TYPE_VALUE, value.getTypeValue())
            .append(TYPE, value.getType());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<AttributeLockExceptionGroup> getEncoderClass() {
      return AttributeLockExceptionGroup.class;
   }
}

