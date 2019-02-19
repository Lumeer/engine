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

import io.lumeer.api.model.Function;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FunctionCodec implements Codec<Function> {

   public static final String JS = "js";
   public static final String XML = "xml";
   public static final String ERROR_REPORT = "errorReport";
   public static final String TIMESTAMP = "timestamp";

   private final Codec<Document> documentCodec;

   public FunctionCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Function decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return FunctionCodec.convertFromDocument(bson);
   }

   @Override
   public void encode(final BsonWriter writer, final Function function, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(JS, function.getJs())
            .append(XML, function.getXml())
            .append(ERROR_REPORT, function.getErrorReport())
            .append(TIMESTAMP, function.getTimestamp());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Function> getEncoderClass() {
      return Function.class;
   }

   public static Function convertFromDocument(final Document document) {
      if (document == null) {
         return null;
      }

      String js = document.getString(JS);
      String xml = document.getString(XML);
      String errorReport = document.getString(ERROR_REPORT);
      long timestamp = document.getLong(TIMESTAMP);

      return new Function(js, xml, errorReport, timestamp);
   }

}
