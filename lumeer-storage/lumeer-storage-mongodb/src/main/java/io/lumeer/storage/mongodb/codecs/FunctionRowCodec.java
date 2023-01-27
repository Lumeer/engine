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

import io.lumeer.api.model.ResourceVariable;
import io.lumeer.api.model.function.FunctionResourceType;
import io.lumeer.api.model.function.FunctionRow;

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

public class FunctionRowCodec implements CollectibleCodec<FunctionRow> {

   public static final String ID = "_id";
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
   public FunctionRow generateIdIfAbsentFromDocument(final FunctionRow variable) {
      if (!documentHasId(variable)) {
         variable.setId(new ObjectId().toHexString());
      }
      return variable;
   }

   @Override
   public boolean documentHasId(final FunctionRow functionRow) {
      return functionRow.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final FunctionRow functionRow) {
      if (!documentHasId(functionRow)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(functionRow.getId()));
   }

   @Override
   public FunctionRow decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String resourceId = bson.getString(RESOURCE_ID);
      FunctionResourceType type = FunctionResourceType.valueOf(bson.getString(TYPE));
      String attributeId = bson.getString(ATTRIBUTE_ID);
      String dependentCollectionId = bson.getString(DEPENDENT_COLLECTION_ID);
      String dependentLinkTypeId = bson.getString(DEPENDENT_LINK_TYPE_ID);
      String dependentAttributeId = bson.getString(DEPENDENT_ATTRIBUTE_ID);

      return new FunctionRow(id, resourceId, type, attributeId, dependentCollectionId, dependentLinkTypeId, dependentAttributeId);
   }

   @Override
   public void encode(final BsonWriter writer, final FunctionRow function, final EncoderContext encoderContext) {
      final Document bson = documentHasId(function) ? new Document(ID, new ObjectId(function.getId())) : new Document();

      bson.append(RESOURCE_ID, function.getResourceId())
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
