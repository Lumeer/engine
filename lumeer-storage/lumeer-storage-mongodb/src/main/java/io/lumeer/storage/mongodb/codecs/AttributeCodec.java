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

import io.lumeer.api.dto.JsonAttribute;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashSet;
import java.util.List;

public class AttributeCodec implements Codec<JsonAttribute> {

   public static final String NAME = "name";
   public static final String FULLNAME = "fullName";
   public static final String CONSTRAINTS = "constraints";
   public static final String USAGE_COUNT = "usageCount";

   private final Codec<Document> documentCodec;

   public AttributeCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public JsonAttribute decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeCodec.convertFromDocument(bson);
   }

   public static JsonAttribute convertFromDocument(final Document document) {
      String name = document.getString(NAME);
      String fullName = document.getString(FULLNAME);
      List<String> constraints = document.get(CONSTRAINTS, List.class);
      Integer usageCount = document.getInteger(USAGE_COUNT);

      return new JsonAttribute(name, fullName, new HashSet<>(constraints), usageCount);
   }

   @Override
   public void encode(final BsonWriter writer, final JsonAttribute value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(NAME, value.getName())
            .append(FULLNAME, value.getFullName())
            .append(CONSTRAINTS, value.getConstraints())
            .append(USAGE_COUNT, value.getUsageCount());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<JsonAttribute> getEncoderClass() {
      return JsonAttribute.class;
   }
}

