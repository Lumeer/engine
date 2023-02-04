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

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.ResourcesPermissions;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

public class ResourcesPermissionsCodec implements Codec<ResourcesPermissions> {
   public static final String COLLECTIONS = "collections";
   public static final String LINK_TYPES = "linkTypes";

   private final Codec<Document> documentCodec;

   public ResourcesPermissionsCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public ResourcesPermissions decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return ResourcesPermissionsCodec.convertFromDocument(document);
   }

   public static ResourcesPermissions convertFromDocument(final Document document) {
      if (document == null) {
         return new ResourcesPermissions();
      }

      Map<String, Permissions> collections = convertPermissionsMap(document.get(COLLECTIONS, Document.class));
      Map<String, Permissions> linkTypes = convertPermissionsMap(document.get(LINK_TYPES, Document.class));

      return new ResourcesPermissions(collections, linkTypes);
   }

   private static Map<String, Permissions> convertPermissionsMap(final Document document) {
      Map<String, Permissions> rules = new HashMap<>();
      if (document != null) {
         document.forEach((k, v) -> {
            final Permissions rule = PermissionsCodec.convertFromDocument(document.get(k, Document.class));
            rules.put(k, rule);
         });
      }
      return rules;
   }

   @Override
   public void encode(final BsonWriter writer, final ResourcesPermissions value, final EncoderContext encoderContext) {
      Document bson = new Document(COLLECTIONS, value.getCollections())
            .append(LINK_TYPES, value.getLinkTypes());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<ResourcesPermissions> getEncoderClass() {
      return ResourcesPermissions.class;
   }

}
