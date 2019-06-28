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

import io.lumeer.api.model.CollectionAttributeFilter;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class AttributeFilterCodec implements Codec<CollectionAttributeFilter> {

   public static final String COLLECTION_ID = "collectionId";
   public static final String OPERATOR = "condition";
   public static final String VALUE = "value";
   public static final String ATTRIBUTE_ID = "attributeId";

   private final Codec<Document> documentCodec;

   public AttributeFilterCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public CollectionAttributeFilter decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeFilterCodec.convertFromDocument(bson);
   }

   public static CollectionAttributeFilter convertFromDocument(final Document document) {
      String collectionId = document.getString(COLLECTION_ID);
      String attributeId = document.getString(ATTRIBUTE_ID);
      String operator = document.getString(OPERATOR);
      Object value = document.get(VALUE);

      return new CollectionAttributeFilter(collectionId, attributeId, operator, value);
   }

   @Override
   public void encode(final BsonWriter writer, final CollectionAttributeFilter value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(COLLECTION_ID, value.getCollectionId())
            .append(OPERATOR, value.getOperator())
            .append(VALUE, value.getValue())
            .append(ATTRIBUTE_ID, value.getAttributeId());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<CollectionAttributeFilter> getEncoderClass() {
      return CollectionAttributeFilter.class;
   }
}

