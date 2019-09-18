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

import io.lumeer.api.model.Sequence;

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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SequenceCodec implements CollectibleCodec<Sequence> {

   public static final String ID = "_id";
   public static final String NAME = "name";
   public static final String SEQ = "seq";

   private final Codec<Document> documentCodec;

   public SequenceCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Sequence generateIdIfAbsentFromDocument(final Sequence sequence) {
      if (!documentHasId(sequence)) {
         sequence.setId(new ObjectId().toHexString());
      }
      return sequence;
   }

   @Override
   public boolean documentHasId(final Sequence sequence) {
      return sequence.getId() != null && !("".equals(sequence.getId()));
   }

   @Override
   public BsonValue getDocumentId(final Sequence sequence) {
      if (!documentHasId(sequence)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(sequence.getId()));
   }

   @Override
   public Sequence decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(bsonReader, decoderContext);

      return SequenceCodec.convertFromDocument(document);
   }

   private static Sequence convertFromDocument(final Document bson) {
      String id = bson.get(ID) != null ? bson.getObjectId(ID).toHexString() : null;
      String name = bson.getString(NAME);
      int seq = bson.getInteger(SEQ);

      Sequence sequence = new Sequence(name, seq);
      sequence.setId(id);

      return sequence;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final Sequence sequence, final EncoderContext encoderContext) {
      Document document = (documentHasId(sequence) ? new Document(ID, getDocumentId(sequence)) : new Document())
            .append(NAME, sequence.getName())
            .append(SEQ, sequence.getSeq());

      documentCodec.encode(bsonWriter, document, encoderContext);
   }

   @Override
   public Class<Sequence> getEncoderClass() {
      return Sequence.class;
   }
}
