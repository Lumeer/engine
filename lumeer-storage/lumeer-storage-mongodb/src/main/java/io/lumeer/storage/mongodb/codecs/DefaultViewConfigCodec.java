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

import io.lumeer.api.model.DefaultViewConfig;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DefaultViewConfigCodec implements Codec<DefaultViewConfig> {

   public static final String USER_ID = "userId";
   public static final String PERSPECTIVE = "perspective";
   public static final String KEY = "key";
   public static final String CONFIG = "config";
   public static final String UPDATED_AT = "updatedAt";

   private final Codec<Document> configCodec;

   public DefaultViewConfigCodec(final CodecRegistry registry) {
      this.configCodec = registry.get(Document.class);
   }

   @Override
   public DefaultViewConfig decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = configCodec.decode(bsonReader, decoderContext);

      String userId = bson.getString(USER_ID);
      String perspective = bson.getString(PERSPECTIVE);
      String collectionId = bson.getString(KEY);
      Object config = bson.get(CONFIG);
      Date updateAtDate = bson.getDate(UPDATED_AT);
      ZonedDateTime updatedAt = updateAtDate != null ? ZonedDateTime.ofInstant(updateAtDate.toInstant(), ZoneOffset.UTC) : null;

      var defaultConfig = new DefaultViewConfig(collectionId, perspective, config, updatedAt);
      defaultConfig.setUserId(userId);

      return defaultConfig;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final DefaultViewConfig config, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(USER_ID, config.getUserId())
            .append(PERSPECTIVE, config.getPerspective())
            .append(KEY, config.getKey())
            .append(CONFIG, config.getConfig());

      if (config.getUpdatedAt() != null) {
         bson.append(UPDATED_AT, Date.from(config.getUpdatedAt().toInstant()));
      }

      configCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<DefaultViewConfig> getEncoderClass() {
      return DefaultViewConfig.class;
   }

}
