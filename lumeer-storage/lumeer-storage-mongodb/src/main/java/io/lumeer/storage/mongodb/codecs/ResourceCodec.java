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

import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.common.JsonResource;

import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

public abstract class ResourceCodec {

   public static final String ID = "_id";
   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String DESCRIPTION = "description";
   public static final String PERMISSIONS = "permissions";

   protected final Codec<Document> documentCodec;
   protected Document decodedBson;

   protected ResourceCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   protected JsonResource decodeResource(final Document bson) {
      String id = bson.getObjectId(ID).toHexString();
      String code = bson.getString(CODE);
      String name = bson.getString(NAME);
      String icon = bson.getString(ICON);
      String color = bson.getString(COLOR);
      String description = bson.getString(DESCRIPTION);
      JsonPermissions permissions = PermissionsCodec.convertFromDocument(bson.get(PERMISSIONS, Document.class)); // TODO try to use better approach

      JsonResource view = new JsonResource(code, name, icon, color, description, permissions);
      view.setId(id);
      return view;
   }

   protected Document encodeResource(JsonResource value) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(CODE, value.getCode())
          .append(NAME, value.getName())
          .append(ICON, value.getIcon())
          .append(COLOR, value.getColor())
          .append(DESCRIPTION, value.getDescription())
          .append(PERMISSIONS, value.getPermissions());

      return bson;
   }

   protected JsonResource generateIdIfAbsentFromDocument(final JsonResource document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   protected boolean documentHasId(final JsonResource document) {
      return document.getId() != null;
   }

   protected BsonValue getDocumentId(final JsonResource document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}
