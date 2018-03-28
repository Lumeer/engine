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

import io.lumeer.api.model.Group;

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

public class GroupCodec implements CollectibleCodec<Group> {

   public static final String ID = "_id";
   public static final String NAME = "name";

   private final Codec<Document> documentCodec;

   public GroupCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Group generateIdIfAbsentFromDocument(final Group group) {
      if (!documentHasId(group)) {
         group.setId(new ObjectId().toHexString());
      }
      return group;
   }

   @Override
   public boolean documentHasId(final Group group) {
      return group.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final Group group) {
      if (!documentHasId(group)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(group.getId()));
   }

   @Override
   public Group decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String name = bson.getString(NAME);

      return new Group(id, name);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final Group group, final EncoderContext encoderContext) {
      Document bson = group.getId() != null ? new Document(ID, new ObjectId(group.getId())) : new Document();
      bson.append(NAME, group.getName());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<Group> getEncoderClass() {
      return Group.class;
   }

}

