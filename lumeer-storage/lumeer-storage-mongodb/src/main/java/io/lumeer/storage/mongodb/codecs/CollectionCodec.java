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
import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.common.JsonResource;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionCodec extends ResourceCodec implements CollectibleCodec<JsonCollection> {

   public static final String ATTRIBUTES = "attributes";
   public static final String DOCUMENTS_COUNT = "docCount";
   public static final String LAST_TIME_USED = "lastTimeUsed";
   public static final String LAST_ATTRIBUTE_NUM = "lastAttributeNum";
   public static final String DEFAULT_ATTRIBUTE_ID = "defaultAttributeId";

   public CollectionCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public JsonCollection decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      JsonResource resource = decodeResource(bson);

      Set<JsonAttribute> attributes = new ArrayList<Document>(bson.get(ATTRIBUTES, List.class)).stream()
                                                                                               .map(AttributeCodec::convertFromDocument)
                                                                                               .collect(Collectors.toSet());

      Integer documentsCount = bson.getInteger(DOCUMENTS_COUNT);
      Integer lastAttributeNum = bson.getInteger(LAST_ATTRIBUTE_NUM);
      Date lastTimeUsed = bson.getDate(LAST_TIME_USED);
      String defaultAttributeId = bson.getString(DEFAULT_ATTRIBUTE_ID);

      JsonCollection collection = new JsonCollection(resource.getCode(), resource.getName(), resource.getIcon(), resource.getColor(), resource.getDescription(), (JsonPermissions) resource.getPermissions(), attributes);
      collection.setId(resource.getId());
      collection.setDocumentsCount(documentsCount);
      collection.setLastTimeUsed(ZonedDateTime.ofInstant(lastTimeUsed.toInstant(), ZoneOffset.UTC));
      collection.setDefaultAttributeId(defaultAttributeId);
      collection.setLastAttributeNum(lastAttributeNum);

      return collection;
   }

   @Override
   public void encode(final BsonWriter writer, final JsonCollection collection, final EncoderContext encoderContext) {
      Document bson = encodeResource(collection)
            .append(DOCUMENTS_COUNT, collection.getDocumentsCount())
            .append(DEFAULT_ATTRIBUTE_ID, collection.getDefaultAttributeId())
            .append(LAST_TIME_USED, Date.from(collection.getLastTimeUsed().toInstant()))
            .append(LAST_ATTRIBUTE_NUM, collection.getLastAttributeNum())
            .append(ATTRIBUTES, collection.getAttributes());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<JsonCollection> getEncoderClass() {
      return JsonCollection.class;
   }

   @Override
   public JsonCollection generateIdIfAbsentFromDocument(final JsonCollection jsonCollection) {
      JsonResource resource = generateIdIfAbsentFromDocument((JsonResource) jsonCollection);
      jsonCollection.setId(resource.getId());
      return jsonCollection;
   }

   @Override
   public boolean documentHasId(final JsonCollection jsonCollection) {
      return documentHasId((JsonResource) jsonCollection);
   }

   @Override
   public BsonValue getDocumentId(final JsonCollection jsonCollection) {
      return getDocumentId((JsonResource) jsonCollection);
   }
}
