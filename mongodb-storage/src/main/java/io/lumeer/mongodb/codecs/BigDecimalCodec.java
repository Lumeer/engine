/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.mongodb.codecs;

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
