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

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DocumentCodec implements CollectibleCodec<Document> {

   public static final String ID = "_id";
   public static final String COLLECTION_ID = "collectionId";
   public static final String CREATION_DATE = "creationDate";
   public static final String UPDATE_DATE = "updateDate";
   public static final String CREATED_BY = "createdBy";
   public static final String UPDATED_BY = "updatedBy";
   public static final String DATA_VERSION = "dataVersion";
   public static final String META_DATA = "metaData";

   private final Codec<org.bson.Document> documentCodec;

   public DocumentCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(org.bson.Document.class);
   }

   @Override
   public Document decode(final BsonReader reader, final DecoderContext decoderContext) {
      org.bson.Document bson = documentCodec.decode(reader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String collectionId = bson.getString(COLLECTION_ID);
      Date creationDate = bson.getDate(CREATION_DATE);
      ZonedDateTime creationZonedDate = creationDate != null ? ZonedDateTime.ofInstant(creationDate.toInstant(), ZoneOffset.UTC) : null;
      String createdBy = bson.getString(CREATED_BY);
      Date updateDate = bson.getDate(UPDATE_DATE);
      ZonedDateTime updatedZonedDate = updateDate != null ? ZonedDateTime.ofInstant(updateDate.toInstant(), ZoneOffset.UTC) : null;
      String updatedBy = bson.getString(UPDATED_BY);
      Integer version = bson.getInteger(DATA_VERSION);
      org.bson.Document metaData = bson.get(META_DATA, org.bson.Document.class);

      Document document = new Document(collectionId, creationZonedDate, updatedZonedDate, createdBy, updatedBy, version, new DataDocument(metaData));
      document.setId(id);
      return document;
   }

   @Override
   public void encode(final BsonWriter writer, final Document document, final EncoderContext encoderContext) {
      org.bson.Document bson = document.getId() != null ? new org.bson.Document(ID, new ObjectId(document.getId())) : new org.bson.Document();
      bson.append(COLLECTION_ID, document.getCollectionId())
          .append(CREATED_BY, document.getCreatedBy())
          .append(UPDATED_BY, document.getUpdatedBy())
          .append(DATA_VERSION, document.getDataVersion())
          .append(META_DATA, document.getMetaData());

      if (document.getCreationDate() != null) {
         bson.append(CREATION_DATE, Date.from(document.getCreationDate().toInstant()));
      }
      if (document.getUpdateDate() != null) {
         bson.append(UPDATE_DATE, Date.from(document.getUpdateDate().toInstant()));
      }

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Document> getEncoderClass() {
      return Document.class;
   }

   @Override
   public Document generateIdIfAbsentFromDocument(final Document document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final Document document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final Document document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}

