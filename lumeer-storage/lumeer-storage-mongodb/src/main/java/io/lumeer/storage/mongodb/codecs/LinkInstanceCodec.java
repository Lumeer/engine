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

import io.lumeer.api.model.LinkInstance;

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

import java.util.List;
import java.util.Map;

public class LinkInstanceCodec implements CollectibleCodec<LinkInstance> {

   public static final String ID = "_id";
   public static final String LINK_TYPE_ID = "linkTypeId";
   public static final String DOCUMENTS_IDS = "documentIds";
   public static final String VERSION = "collectionIds";
   public static final String DATA = "data";

   private final Codec<Document> documentCodec;

   public LinkInstanceCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public LinkInstance decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String linkTypeId = bson.getString(LINK_TYPE_ID);
      List<String> documentIds = bson.get(DOCUMENTS_IDS, List.class);
      Map<String, Object> data = bson.get(DATA, Map.class);
      Integer version = bson.getInteger(VERSION);

      LinkInstance linkInstance = new LinkInstance(id, linkTypeId, documentIds, data);
      linkInstance.setVersion(version);
      return linkInstance;
   }

   @Override
   public void encode(final BsonWriter writer, final LinkInstance value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(LINK_TYPE_ID, value.getLinkTypeId())
          .append(DOCUMENTS_IDS, value.getDocumentIds())
          .append(DATA, value.getData());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<LinkInstance> getEncoderClass() {
      return LinkInstance.class;
   }

   @Override
   public LinkInstance generateIdIfAbsentFromDocument(final LinkInstance document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final LinkInstance document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final LinkInstance document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}

