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

import io.lumeer.api.model.AttributeLock;
import io.lumeer.api.model.AttributeLockExceptionGroup;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeLockCodec implements Codec<AttributeLock> {

   public static final String LOCKED = "locked";
   public static final String EXCEPTION_GROUPS = "exceptionGroups";

   private final Codec<Document> documentCodec;

   public AttributeLockCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public AttributeLock decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return AttributeLockCodec.convertFromDocument(bson);
   }

   public static AttributeLock convertFromDocument(final Document document) {
      Boolean locked = document.getBoolean(LOCKED, false);

      List<AttributeLockExceptionGroup> exceptionGroups;
      List groupsList = document.get(EXCEPTION_GROUPS, List.class);
      if (groupsList != null) {
         exceptionGroups = document.getList(groupsList, Document.class)
                          .stream()
                          .map(AttributeLockExceptionGroupCodec::convertFromDocument)
                          .collect(Collectors.toList());
      } else {
         exceptionGroups = Collections.emptyList();
      }

      return new AttributeLock(exceptionGroups, locked);
   }

   @Override
   public void encode(final BsonWriter writer, final AttributeLock value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(LOCKED, value.getLocked())
            .append(EXCEPTION_GROUPS, value.getExceptionGroups());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<AttributeLock> getEncoderClass() {
      return AttributeLock.class;
   }
}

