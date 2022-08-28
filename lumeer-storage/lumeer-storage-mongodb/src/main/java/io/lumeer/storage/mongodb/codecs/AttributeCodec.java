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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.AttributeFormatting;
import io.lumeer.api.model.AttributeLock;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.function.Function;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class AttributeCodec implements Codec<Attribute> {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String DESCRIPTION = "description";
   public static final String LOCK = "lock";
   public static final String FORMATTING = "formatting";
   public static final String CONSTRAINT = "constraint";
   public static final String FUNCTION = "function";
   public static final String USAGE_COUNT = "usageCount";
   public static final String SUGGEST_VALUES = "suggestValues";

   private final Codec<Document> documentCodec;

   public AttributeCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Attribute decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeCodec.convertFromDocument(bson);
   }

   public static Attribute convertFromDocument(final Document document) {
      String id = document.getString(ID);
      String name = document.getString(NAME);
      String description = document.getString(DESCRIPTION);
      Constraint constraint = ConstraintCodec.convertFromDocument(document.get(CONSTRAINT, Document.class));
      AttributeLock lock = AttributeLockCodec.convertFromDocument(document.get(LOCK, Document.class));
      AttributeFormatting formatting = AttributeFormattingCodec.convertFromDocument(document.get(FORMATTING, Document.class));
      Function function = FunctionCodec.convertFromDocument(document.get(FUNCTION, Document.class));
      Integer usageCount = document.getInteger(USAGE_COUNT);
      Boolean suggestValues = document.getBoolean(SUGGEST_VALUES);

      return new Attribute(id, name, description, constraint, lock, formatting, function, usageCount, suggestValues);
   }

   @Override
   public void encode(final BsonWriter writer, final Attribute value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(ID, value.getId())
            .append(NAME, value.getName())
            .append(DESCRIPTION, value.getDescription())
            .append(LOCK, value.getLock())
            .append(FORMATTING, value.getFormatting())
            .append(CONSTRAINT, value.getConstraint())
            .append(FUNCTION, value.getFunction())
            .append(USAGE_COUNT, value.getUsageCount())
            .append(SUGGEST_VALUES, value.getSuggestValues());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Attribute> getEncoderClass() {
      return Attribute.class;
   }
}

