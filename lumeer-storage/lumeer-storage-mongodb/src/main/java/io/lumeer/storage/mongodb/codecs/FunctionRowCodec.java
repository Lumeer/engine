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

import io.lumeer.api.model.function.FunctionResourceType;
import io.lumeer.api.model.function.FunctionRow;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class FunctionRowCodec implements Codec<FunctionRow> {

   public static String RESOURCE_ID = "resourceId";
   public static String TYPE = "resourceType";
   public static String ATTRIBUTE_ID = "attributeId";
   public static String DEPENDENT_COLLECTION_ID = "dependentCollectionId";
   public static String DEPENDENT_LINK_TYPE_ID = "dependentLinkTypeId";
   public static String DEPENDENT_ATTRIBUTE_ID = "dependentAttributeId";

   private final Codec<Document> documentCodec;

   public FunctionRowCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public FunctionRow decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      String resourceId = bson.getString(RESOURCE_ID);
      FunctionResourceType type = FunctionResourceType.valueOf(bson.getString(TYPE));
      String attributeId = bson.getString(ATTRIBUTE_ID);
      String dependentCollectionId = bson.getString(DEPENDENT_COLLECTION_ID);
      String dependentLinkTypeId = bson.getString(DEPENDENT_LINK_TYPE_ID);
      String dependentAttributeId = bson.getString(DEPENDENT_ATTRIBUTE_ID);

      return new FunctionRow(resourceId, type, attributeId, dependentCollectionId, dependentLinkTypeId, dependentAttributeId);
   }

   @Override
   public void encode(final BsonWriter writer, final FunctionRow function, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(RESOURCE_ID, function.getResourceId())
            .append(TYPE, function.getType().toString())
            .append(ATTRIBUTE_ID, function.getAttributeId())
            .append(DEPENDENT_COLLECTION_ID, function.getDependentCollectionId())
            .append(DEPENDENT_LINK_TYPE_ID, function.getDependentLinkTypeId())
            .append(DEPENDENT_ATTRIBUTE_ID, function.getDependentAttributeId());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<FunctionRow> getEncoderClass() {
      return FunctionRow.class;
   }

}
