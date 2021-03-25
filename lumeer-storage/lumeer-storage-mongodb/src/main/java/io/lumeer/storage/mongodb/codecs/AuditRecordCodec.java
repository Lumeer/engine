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

import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;

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

public class AuditRecordCodec implements CollectibleCodec<AuditRecord> {

   public static final String ID = "_id";

   private final Codec<Document> codec;

   public AuditRecordCodec(final CodecRegistry codecRegistry) {
      codec = codecRegistry.get(Document.class);
   }

   @Override
   public AuditRecord generateIdIfAbsentFromDocument(final AuditRecord record) {
      if (!documentHasId(record)) {
         record.setId(new ObjectId().toHexString());
      }
      return record;
   }

   @Override
   public boolean documentHasId(final AuditRecord record) {
      return record.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final AuditRecord record) {
      if (!documentHasId(record)) {
         throw new IllegalStateException("The record does not contain an id");
      }

      return new BsonObjectId(new ObjectId(record.getId()));
   }

   @Override
   public AuditRecord decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      final Document bson = codec.decode(bsonReader, decoderContext);
      final AuditRecord record = new AuditRecord();

      record.setId(bson.getObjectId(ID).toHexString());
      record.setParentId(bson.getString(AuditRecord.PARENT_ID));
      record.setResourceType(ResourceType.valueOf(bson.getString(AuditRecord.RESOURCE_TYPE).toUpperCase()));
      record.setResourceId(bson.getString(AuditRecord.RESOUCE_ID));
      record.setUser(AuditRecord.USER);

      final Date changeDate = bson.getDate(AuditRecord.CHANGE_DATE);
      record.setChangeDate(changeDate != null ? ZonedDateTime.ofInstant(changeDate.toInstant(), ZoneOffset.UTC) : null);

      final Document oldState = bson.get(AuditRecord.OLD_STATE, Document.class);
      record.setOldState(oldState != null ? new DataDocument(oldState) : new DataDocument());

      final Document newState = bson.get(AuditRecord.OLD_STATE, Document.class);
      record.setOldState(newState != null ? new DataDocument(newState) : new DataDocument());

      return record;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final AuditRecord record, final EncoderContext encoderContext) {
      Document bson = record.getId() != null ? new Document(ID, new ObjectId(record.getId())) : new Document();
      bson.append(AuditRecord.PARENT_ID, record.getParentId())
          .append(AuditRecord.RESOURCE_TYPE, record.getResourceType().toString())
          .append(AuditRecord.PARENT_ID, record.getResourceId())
          .append(AuditRecord.USER, record.getUser())
          .append(AuditRecord.OLD_STATE, record.getOldState())
          .append(AuditRecord.NEW_STATE, record.getNewState());

      if (record.getChangeDate() != null) {
         bson.append(AuditRecord.CHANGE_DATE, Date.from(record.getChangeDate().toInstant()));
      }

      codec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<AuditRecord> getEncoderClass() {
      return AuditRecord.class;
   }
}
