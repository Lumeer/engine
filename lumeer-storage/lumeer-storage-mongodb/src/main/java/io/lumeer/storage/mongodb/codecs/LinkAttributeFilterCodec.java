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
import io.lumeer.api.model.LinkAttributeFilter;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class LinkAttributeFilterCodec implements Codec<LinkAttributeFilter> {

   public static final String LINK_TYPE_ID = "linkTypeId";

   private final Codec<Document> documentCodec;

   public LinkAttributeFilterCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public LinkAttributeFilter decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return LinkAttributeFilterCodec.convertFromDocument(bson);
   }

   public static LinkAttributeFilter convertFromDocument(final Document document) {
      String linkTypeId = document.getString(LINK_TYPE_ID);
      AttributeFilter filter = AttributeFilterCodec.convertFromDocument(document);

      return new LinkAttributeFilter(linkTypeId, filter.getAttributeId(), filter.getCondition(), filter.getConditionValues());
   }

   @Override
   public void encode(final BsonWriter writer, final LinkAttributeFilter value, final EncoderContext encoderContext) {
      Document bson = AttributeFilterCodec.convertToDocument(value)
            .append(LINK_TYPE_ID, value.getLinkTypeId());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<LinkAttributeFilter> getEncoderClass() {
      return LinkAttributeFilter.class;
   }
}

