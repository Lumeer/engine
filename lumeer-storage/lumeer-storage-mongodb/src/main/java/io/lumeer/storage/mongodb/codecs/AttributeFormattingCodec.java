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

import io.lumeer.api.model.AttributeFormatting;
import io.lumeer.api.model.AttributeFormattingGroup;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeFormattingCodec implements Codec<AttributeFormatting> {

   public static final String GROUPS = "grouos";

   private final Codec<Document> documentCodec;

   public AttributeFormattingCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public AttributeFormatting decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeFormattingCodec.convertFromDocument(bson);
   }

   public static AttributeFormatting convertFromDocument(final Document document) {
      if (document == null) {
         return null;
      }

      List<AttributeFormattingGroup> groups;
      List groupsList = document.get(GROUPS, List.class);
      if (groupsList != null) {
         groups = new ArrayList<Document>(groupsList)
                                   .stream()
                                   .map(AttributeFormattingGroupCodec::convertFromDocument)
                                   .collect(Collectors.toList());
      } else {
         groups = Collections.emptyList();
      }

      return new AttributeFormatting(groups);
   }

   @Override
   public void encode(final BsonWriter writer, final AttributeFormatting value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(GROUPS, value.getGroups());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<AttributeFormatting> getEncoderClass() {
      return AttributeFormatting.class;
   }
}

