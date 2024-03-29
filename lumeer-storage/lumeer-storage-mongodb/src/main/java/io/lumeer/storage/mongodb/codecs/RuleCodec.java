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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class RuleCodec implements Codec<Rule> {

   public static final String NAME = "name";
   public static final String CREATED_AT = "createdAt";
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
            .append(NAME, rule.getName())
            .append(TYPE, rule.getType() != null ? rule.getType().ordinal() : null)
            .append(CONFIGURATION, rule.getConfiguration());

      if (rule.getTiming() != null) {
         bson.append(TIMING, rule.getTiming().ordinal());
      }
      if (rule.getCreatedAt() != null) {
         bson.append(CREATED_AT, Date.from(rule.getCreatedAt().toInstant()));
      }

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   public static Rule convertFromDocument(final Document bson) {
      final String name = bson.getString(NAME);
      final Integer typeInt = bson.getInteger(TYPE);
      final Rule.RuleType type = typeInt != null ? Rule.RuleType.values()[typeInt] : null;

      final Integer timingInt = bson.getInteger(TIMING);
      final Rule.RuleTiming timing = timingInt != null ? Rule.RuleTiming.values()[timingInt] : null;
      final Document configuration = bson.get(CONFIGURATION, org.bson.Document.class);

      final Rule rule = new Rule(name, type, timing, new DataDocument(configuration));

      final Date createdAt = bson.getDate(CREATED_AT);
      if (createdAt != null) {
         rule.setCreatedAt(ZonedDateTime.ofInstant(createdAt.toInstant(), ZoneOffset.UTC));
      }

      return rule;
   }

   @Override
   public Class<Rule> getEncoderClass() {
      return Rule.class;
   }
}
