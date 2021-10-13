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

import io.lumeer.api.model.SelectOption;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class SelectOptionCodec implements Codec<SelectOption> {

   public static final String VALUE = "value";
   public static final String DISPLAY_VALUE = "displayValue";
   public static final String BACKGROUND = "background";

   private final Codec<Document> documentCodec;

   public SelectOptionCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public SelectOption decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);
      return convertFromDocument(bson);
   }

   public static SelectOption convertFromDocument(final Document bson) {
      String value = bson.getString(VALUE);
      String displayValue = bson.getString(DISPLAY_VALUE);
      String background = bson.getString(BACKGROUND);

      return new SelectOption(value, displayValue, background);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final SelectOption option, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(VALUE, option.getValue())
            .append(DISPLAY_VALUE, option.getDisplayValue())
            .append(BACKGROUND, option.getBackground());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<SelectOption> getEncoderClass() {
      return SelectOption.class;
   }

}

