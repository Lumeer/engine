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

import io.lumeer.api.dto.JsonPermission;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionCodec implements Codec<JsonPermission> {

   public static final String NAME = "name";
   public static final String ROLES = "roles";

   private final Codec<Document> documentCodec;

   public PermissionCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public JsonPermission decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return PermissionCodec.convertFromDocument(document);
   }

   public static JsonPermission convertFromDocument(Document bson) {
      String name = bson.getString(NAME);
      Set<String> roles = new HashSet<String>(bson.get(ROLES, List.class));

      return new JsonPermission(name, roles);
   }

   @Override
   public void encode(final BsonWriter writer, final JsonPermission value, final EncoderContext encoderContext) {
      Document document = new Document(NAME, value.getName())
            .append(ROLES, value.getRoles());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<JsonPermission> getEncoderClass() {
      return JsonPermission.class;
   }

}
