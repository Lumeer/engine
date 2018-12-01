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

import io.lumeer.api.model.OldQuery;
import io.lumeer.api.model.OldView;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.common.SimpleResource;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class OldViewCodec extends ResourceCodec implements CollectibleCodec<OldView> {

   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";
   public static final String AUTHOR_ID = "authorId";

   public OldViewCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public OldView decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      SimpleResource resource = decodeResource(bson);

      OldQuery query = OldQueryCodec.convertFromDocument(bson.get(QUERY, Document.class));
      String perspective = bson.getString(PERSPECTIVE);
      Object config = bson.get(CONFIG);
      String authorId = bson.getString(AUTHOR_ID);

      OldView view = new OldView(resource.getCode(), resource.getName(), resource.getIcon(), resource.getColor(), resource.getDescription(), resource.getPermissions(), query, perspective, config, authorId);
      view.setId(resource.getId());
      return view;
   }

   @Override
   public void encode(final BsonWriter writer, final OldView value, final EncoderContext encoderContext) {
      Document bson = encodeResource(value)
            .append(QUERY, value.getQuery())
            .append(PERSPECTIVE, value.getPerspective())
            .append(CONFIG, value.getConfig())
            .append(AUTHOR_ID, value.getAuthorId());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<OldView> getEncoderClass() {
      return OldView.class;
   }

   @Override
   public OldView generateIdIfAbsentFromDocument(final OldView jsonView) {
      Resource resource = generateIdIfAbsentFromDocument((Resource) jsonView);
      jsonView.setId(resource.getId());
      return jsonView;
   }

   @Override
   public boolean documentHasId(final OldView jsonView) {
      return documentHasId((Resource) jsonView);
   }

   @Override
   public BsonValue getDocumentId(final OldView jsonView) {
      return getDocumentId((Resource) jsonView);
   }
}