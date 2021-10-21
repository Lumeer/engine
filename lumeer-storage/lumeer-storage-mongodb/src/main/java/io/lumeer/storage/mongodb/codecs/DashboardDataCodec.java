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

import io.lumeer.api.model.DashboardData;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class DashboardDataCodec implements Codec<DashboardData> {

   public static final String TYPE = "type";
   public static final String TYPE_ID = "typeId";
   public static final String USER_ID = "userId";
   public static final String DATA = "data";

   private final Codec<Document> documentCodec;

   public DashboardDataCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public DashboardData decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      String type = document.getString(TYPE);
      String typeId = document.getString(TYPE_ID);
      String userId = document.getString(USER_ID);
      Object data = document.get(DATA);

      return new DashboardData(type, typeId, userId, data);
   }

   @Override
   public void encode(final BsonWriter writer, final DashboardData value, final EncoderContext encoderContext) {
      Document document = new Document(TYPE, value.getType())
            .append(TYPE_ID, value.getTypeId())
            .append(USER_ID, value.getUserId())
            .append(DATA, value.getData());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<DashboardData> getEncoderClass() {
      return DashboardData.class;
   }
}
