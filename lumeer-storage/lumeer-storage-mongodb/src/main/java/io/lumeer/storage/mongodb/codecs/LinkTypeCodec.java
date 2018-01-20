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

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.model.LinkType;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LinkTypeCodec implements CollectibleCodec<LinkType> {

   private final Codec<Document> documentCodec;

   public LinkTypeCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public LinkType decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      String id = bson.getObjectId(LinkType.ID).toHexString();
      String name = bson.getString(LinkType.NAME);
      List<String> collectionCodes = bson.get(LinkType.COLLECTION_IDS, List.class);
      List<JsonAttribute> attributes = new ArrayList<Document>(bson.get(LinkType.ATTRIBUTES, List.class)).stream()
                                                                                                .map(AttributeCodec::convertFromDocument)
                                                                                                .collect(Collectors.toList());

      LinkType linkType = new LinkType(name, collectionCodes, attributes);
      linkType.setId(id);
      return linkType;
   }

   @Override
   public void encode(final BsonWriter writer, final LinkType value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(LinkType.ID, new ObjectId(value.getId())) : new Document();
      bson.append(LinkType.NAME, value.getName())
          .append(LinkType.COLLECTION_IDS, value.getCollectionIds())
          .append(LinkType.ATTRIBUTES, value.getAttributes());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<LinkType> getEncoderClass() {
      return LinkType.class;
   }

   @Override
   public LinkType generateIdIfAbsentFromDocument(final LinkType document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final LinkType document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final LinkType document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}

