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

import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class RoleCodec implements Codec<Role> {

   public static final String TYPE = "type";
   public static final String TRANSITIVE = "transitive";

   private final Codec<Document> documentCodec;

   public RoleCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Role decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return RoleCodec.convertFromDocument(document);
   }

   public static Role convertFromDocument(Document bson) {
      RoleType type = RoleType.fromString(bson.getString(TYPE));
      boolean transitive = bson.getBoolean(TRANSITIVE, false);

      return new Role(type, transitive);
   }

   @Override
   public void encode(final BsonWriter writer, final Role value, final EncoderContext encoderContext) {
      Document document = new Document(TYPE, value.getType())
            .append(TRANSITIVE, value.isTransitive());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<Role> getEncoderClass() {
      return Role.class;
   }
}
