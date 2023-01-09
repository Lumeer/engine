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

import io.lumeer.api.model.InformationRecord;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class InformationRecordCodec implements CollectibleCodec<InformationRecord> {

   private final Codec<Document> documentCodec;

   public InformationRecordCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public InformationRecord generateIdIfAbsentFromDocument(final InformationRecord informationRecord) {
      if (!documentHasId(informationRecord)) {
         informationRecord.setId(new ObjectId().toHexString());
      }
      return informationRecord;
   }

   @Override
   public boolean documentHasId(final InformationRecord informationRecord) {
      return informationRecord.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final InformationRecord informationRecord) {
      if (!documentHasId(informationRecord)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(informationRecord.getId()));
   }

   @Override
   public InformationRecord decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      final Document bson = documentCodec.decode(bsonReader, decoderContext);

      final String id = bson.getObjectId(InformationRecord.ID).toHexString();
      final String userId = bson.getString(InformationRecord.USER_ID);
      final String source = bson.getString(InformationRecord.SOURCE);
      final String target = bson.getString(InformationRecord.TARGET);
      final String data = bson.getString(InformationRecord.DATA);

      ZonedDateTime date = null;
      if (bson.getDate(InformationRecord.DATE) != null) {
         date = ZonedDateTime.ofInstant(bson.getDate(InformationRecord.DATE).toInstant(), ZoneOffset.UTC);
      }

      return new InformationRecord(id, userId, date, source, target, data);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final InformationRecord informationRecord, final EncoderContext encoderContext) {
      final Document bson = informationRecord.getId() != null ? new Document(InformationRecord.ID, new ObjectId(informationRecord.getId())) : new Document();
      bson.append(InformationRecord.USER_ID, informationRecord.getUserId())
              .append(InformationRecord.DATE, new Date(informationRecord.getDate().toInstant().toEpochMilli()))
              .append(InformationRecord.SOURCE, informationRecord.getSource())
              .append(InformationRecord.TARGET, informationRecord.getTarget())
              .append(InformationRecord.DATA, informationRecord.getData());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<InformationRecord> getEncoderClass() {
      return InformationRecord.class;
   }
}
