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

import io.lumeer.api.model.Rule;
import io.lumeer.engine.api.data.DataDocument;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class RuleCodec implements Codec<Rule> {

   public static final String TYPE = "type";
   public static final String TIMING = "timing";
   public static final String CONFIGURATION = "configuration";

   private final Codec<Document> documentCodec;

   public RuleCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Rule decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      final Document bson = documentCodec.decode(bsonReader, decoderContext);

      return convertFromDocument(bson);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final Rule rule, final EncoderContext encoderContext) {
      final Document bson = new Document()
            .append(TYPE, rule.getType().ordinal())
            .append(TIMING, rule.getTiming().ordinal())
            .append(CONFIGURATION, rule.getConfiguration());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   public static Rule convertFromDocument(final Document bson) {
      final Integer typeInt = bson.getInteger(TYPE);
      final Rule.RuleType type = Rule.RuleType.values()[typeInt != null ? typeInt : 0];

      final Integer timingInt = bson.getInteger(TIMING);
      final Rule.RuleTiming timing = Rule.RuleTiming.values()[timingInt != null ? timingInt : 0];
      final Document configuration = bson.get(CONFIGURATION, org.bson.Document.class);

      return new Rule(type, timing, new DataDocument(configuration));
   }

   @Override
   public Class<Rule> getEncoderClass() {
      return Rule.class;
   }
}
