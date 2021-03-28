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

import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.engine.api.data.DataDocument;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class CollectionPurposeCodec implements Codec<CollectionPurpose> {

   public static final String TYPE = "type";
   public static final String META_DATA = "metaData";

   private final Codec<Document> documentCodec;

   public CollectionPurposeCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public CollectionPurpose decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return CollectionPurposeCodec.convertFromDocument(document);
   }

   public static CollectionPurpose convertFromDocument(Document bson) {
      if (bson == null) {
         return new CollectionPurpose(CollectionPurposeType.None, new DataDocument());
      }

      String typeString = bson.getString(TYPE);
      CollectionPurposeType purposeType = typeString != null ? CollectionPurposeType.valueOf(typeString) : CollectionPurposeType.None;
      Document metaData = bson.get(META_DATA, Document.class);

      return new CollectionPurpose(purposeType, new DataDocument(metaData == null ? new Document() : metaData));
   }

   @Override
   public void encode(final BsonWriter writer, final CollectionPurpose value, final EncoderContext encoderContext) {
      Document document = new Document()
            .append(TYPE, value.getType() != null ? value.getType().toString() : null)
            .append(META_DATA, value.getMetaData());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<CollectionPurpose> getEncoderClass() {
      return CollectionPurpose.class;
   }

}
