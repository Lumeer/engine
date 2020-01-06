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

public class DefaultViewConfigCodec implements Codec<DefaultViewConfig> {

   public static final String USER_ID = "userId";
   public static final String PERSPECTIVE = "perspective";
   public static final String COLLECTION_ID = "collectionId";
   public static final String CONFIG = "config";

   private final Codec<Document> configCodec;

   public DefaultViewConfigCodec(final CodecRegistry registry) {
      this.configCodec = registry.get(Document.class);
   }

   @Override
   public DefaultViewConfig decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = configCodec.decode(bsonReader, decoderContext);

      String userId = bson.getString(USER_ID);
      String perspective = bson.getString(PERSPECTIVE);
      String collectionId = bson.getString(COLLECTION_ID);
      Object config = bson.get(CONFIG);

      var defaultConfig = new DefaultViewConfig(perspective, collectionId, config);
      defaultConfig.setUserId(userId);

      return defaultConfig;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final DefaultViewConfig config, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(USER_ID, config.getUserId())
            .append(PERSPECTIVE, config.getPerspective())
            .append(COLLECTION_ID, config.getCollectionId())
            .append(CONFIG, config.getConfig());

      configCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<DefaultViewConfig> getEncoderClass() {
      return DefaultViewConfig.class;
   }

}
