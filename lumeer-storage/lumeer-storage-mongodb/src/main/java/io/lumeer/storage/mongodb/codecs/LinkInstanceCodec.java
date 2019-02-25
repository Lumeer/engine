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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

public class LinkInstanceCodec implements CollectibleCodec<LinkInstance> {

   public static final String ID = "_id";
   public static final String LINK_TYPE_ID = "linkTypeId";
   public static final String DOCUMENTS_IDS = "documentIds";
   public static final String CREATION_DATE = "creationDate";
   public static final String UPDATE_DATE = "updateDate";
   public static final String CREATED_BY = "createdBy";
   public static final String UPDATED_BY = "updatedBy";
   public static final String DATA_VERSION = "dataVersion";

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
      Date creationDate = bson.getDate(CREATION_DATE);
      ZonedDateTime creationZonedDate = creationDate != null ? ZonedDateTime.ofInstant(creationDate.toInstant(), ZoneOffset.UTC) : null;
      String createdBy = bson.getString(CREATED_BY);
      Date updateDate = bson.getDate(UPDATE_DATE);
      ZonedDateTime updatedZonedDate = updateDate != null ? ZonedDateTime.ofInstant(updateDate.toInstant(), ZoneOffset.UTC) : null;
      String updatedBy = bson.getString(UPDATED_BY);
      Integer version = bson.getInteger(DATA_VERSION);

      LinkInstance linkInstance = new LinkInstance(linkTypeId, documentIds);
      linkInstance.setId(id);
      linkInstance.setUpdatedBy(updatedBy);
      linkInstance.setUpdateDate(updatedZonedDate);
      linkInstance.setCreatedBy(createdBy);
      linkInstance.setCreationDate(creationZonedDate);
      linkInstance.setDataVersion(version == null ? 1 : version);
      return linkInstance;
   }

   @Override
   public void encode(final BsonWriter writer, final LinkInstance value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(LINK_TYPE_ID, value.getLinkTypeId())
          .append(DOCUMENTS_IDS, value.getDocumentIds())
          .append(CREATED_BY, value.getCreatedBy())
          .append(UPDATED_BY, value.getUpdatedBy());

      if (value.getCreationDate() != null) {
         bson.append(CREATION_DATE, Date.from(value.getCreationDate().toInstant()));
      }
      if (value.getUpdateDate() != null) {
         bson.append(UPDATE_DATE, Date.from(value.getUpdateDate().toInstant()));
      }

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

