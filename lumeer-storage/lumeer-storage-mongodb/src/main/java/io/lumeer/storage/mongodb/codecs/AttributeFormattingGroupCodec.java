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
import io.lumeer.api.model.AttributeFormattingGroup;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class AttributeFormattingGroupCodec implements Codec<AttributeFormattingGroup> {

   public static final String EQUATION = "equation";
   public static final String STYLES = "styles";
   public static final String COLOR = "color";
   public static final String BACKGROUND = "background";

   private final Codec<Document> documentCodec;

   public AttributeFormattingGroupCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public AttributeFormattingGroup decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeFormattingGroupCodec.convertFromDocument(bson);
   }

   public static AttributeFormattingGroup convertFromDocument(final Document document) {
      AttributeFilterEquation equation = AttributeFilterEquationCodec.convertFromDocument(document.get(EQUATION, Document.class));
      List<String> styles = document.getList(STYLES, String.class);
      String color = document.getString(COLOR);
      String background = document.getString(BACKGROUND);

      return new AttributeFormattingGroup(equation, styles, color, background);
   }

   @Override
   public void encode(final BsonWriter writer, final AttributeFormattingGroup value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(EQUATION, value.getEquation())
            .append(STYLES, value.getStyles())
            .append(COLOR, value.getColor())
            .append(BACKGROUND, value.getBackground());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<AttributeFormattingGroup> getEncoderClass() {
      return AttributeFormattingGroup.class;
   }
}

