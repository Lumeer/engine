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

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionsCodec implements Codec<Permissions> {

   public static final String USER_ROLES = "users";
   public static final String GROUP_ROLES = "groups";

   private final Codec<Document> documentCodec;

   public PermissionsCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Permissions decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return PermissionsCodec.convertFromDocument(document);
   }

   public static Permissions convertFromDocument(final Document document) {
      if (document == null) {
         return new Permissions();
      }

      Set<Permission> userPermissions = new ArrayList<Document>(document.get(USER_ROLES, List.class)).stream()
                                                                                                  .map(PermissionCodec::convertFromDocument)
                                                                                                  .collect(Collectors.toSet());
      Set<Permission> groupPermissions = new ArrayList<Document>(document.get(GROUP_ROLES, List.class)).stream()
                                                                                                    .map(PermissionCodec::convertFromDocument)
                                                                                                    .collect(Collectors.toSet());

      Permissions permissions = new Permissions(userPermissions, groupPermissions);
      return permissions;
   }

   @Override
   public void encode(final BsonWriter writer, final Permissions value, final EncoderContext encoderContext) {
      Set<Permission> userPermissions = value.getUserPermissions();
      Set<Permission> groupPermissions = value.getGroupPermissions();
      Document bson = new Document(USER_ROLES, userPermissions).append(GROUP_ROLES, groupPermissions);

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Permissions> getEncoderClass() {
      return Permissions.class;
   }

}
