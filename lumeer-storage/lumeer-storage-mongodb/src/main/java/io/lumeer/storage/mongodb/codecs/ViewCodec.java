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
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.dto.JsonView;

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

public class ViewCodec implements CollectibleCodec<JsonView> {

   public static final String ID = "_id";
   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String DESCRIPTION = "description";
   public static final String PERMISSIONS = "permissions";
   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";
   public static final String AUTHOR_ID = "authorId";

   private final Codec<Document> documentCodec;

   public ViewCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public JsonView decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String code = bson.getString(CODE);
      String name = bson.getString(NAME);
      String icon = bson.getString(ICON);
      String color = bson.getString(COLOR);
      String description = bson.getString(DESCRIPTION);
      JsonPermissions permissions = PermissionsCodec.convertFromDocument(bson.get(PERMISSIONS, Document.class)); // TODO try to use better approach
      JsonQuery query = QueryCodec.convertFromDocument(bson.get(QUERY, Document.class));
      String perspective = bson.getString(PERSPECTIVE);
      Object config = bson.get(CONFIG);
      String authorId = bson.getString(AUTHOR_ID);

      JsonView view = new JsonView(code, name, icon, color, description, permissions, query, perspective, config, authorId);
      view.setId(id);
      return view;
   }

   @Override
   public void encode(final BsonWriter writer, final JsonView value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(CODE, value.getCode())
          .append(NAME, value.getName())
          .append(ICON, value.getIcon())
          .append(COLOR, value.getColor())
          .append(DESCRIPTION, value.getDescription())
          .append(PERMISSIONS, value.getPermissions())
          .append(QUERY, value.getQuery())
          .append(PERSPECTIVE, value.getPerspective())
          .append(CONFIG, value.getConfig())
          .append(AUTHOR_ID, value.getAuthorId());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<JsonView> getEncoderClass() {
      return JsonView.class;
   }

   @Override
   public JsonView generateIdIfAbsentFromDocument(final JsonView document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final JsonView document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final JsonView document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}
