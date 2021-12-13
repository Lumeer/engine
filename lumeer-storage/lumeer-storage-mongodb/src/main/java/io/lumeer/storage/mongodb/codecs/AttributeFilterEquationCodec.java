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

import io.lumeer.api.model.AttributeFilter;
import io.lumeer.api.model.AttributeFilterEquation;

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

public class AttributeFilterEquationCodec implements Codec<AttributeFilterEquation> {

   public static final String EQUATIONS = "equations";
   public static final String FILTER = "filter";
   public static final String OPERATOR = "operator";

   private final Codec<Document> documentCodec;

   public AttributeFilterEquationCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public AttributeFilterEquation decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeFilterEquationCodec.convertFromDocument(bson);
   }

   public static AttributeFilterEquation convertFromDocument(final Document document) {
      AttributeFilter filter = AttributeFilterCodec.convertFromDocument(document.get(FILTER, Document.class));
      String operator = document.getString(OPERATOR);

      List<AttributeFilterEquation> equations;
      List equationsList = document.get(EQUATIONS, List.class);
      if (equationsList != null) {
         equations = new ArrayList<Document>(equationsList).stream()
                                                            .map(AttributeFilterEquationCodec::convertFromDocument)
                                                            .collect(Collectors.toList());
      } else {
         equations = Collections.emptyList();
      }

      return new AttributeFilterEquation(equations, filter, operator);
   }

   @Override
   public void encode(final BsonWriter writer, final AttributeFilterEquation value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(EQUATIONS, value.getEquations())
            .append(FILTER, value.getFilter())
            .append(OPERATOR, value.getOperator());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<AttributeFilterEquation> getEncoderClass() {
      return AttributeFilterEquation.class;
   }
}

