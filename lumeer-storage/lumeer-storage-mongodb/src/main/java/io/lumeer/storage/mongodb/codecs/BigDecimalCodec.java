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

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

/**
 * A codec to convert {@link BigDecimal} to internal MongoDb Decimal128 type.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class BigDecimalCodec implements Codec<BigDecimal> {

   @Override
   public void encode(final BsonWriter writer, final BigDecimal value, final EncoderContext encoderContext) {
      writer.writeDecimal128(new Decimal128(value));
   }

   @Override
   public BigDecimal decode(final BsonReader reader, final DecoderContext decoderContext) {
      return reader.readDecimal128().bigDecimalValue();
   }

   @Override
   public Class<BigDecimal> getEncoderClass() {
      return BigDecimal.class;
   }
}
