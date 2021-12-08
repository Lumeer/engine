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

import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.ResourceVariable;
import io.lumeer.api.model.ResourceVariableType;

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

public class ResourceVariableCodec implements CollectibleCodec<ResourceVariable> {

   public static final String ID = "_id";
   public static final String KEY = "key";
   public static final String VALUE = "value";
   public static final String RESOURCE_ID = "resourceId";
   public static final String RESOURCE_TYPE = "resourceType";
   public static final String TYPE = "type";
   public static final String SECURE = "secure";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String PROJECT_ID = "projectId";

   private final Codec<Document> documentCodec;

   public ResourceVariableCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public ResourceVariable generateIdIfAbsentFromDocument(final ResourceVariable variable) {
      if (!documentHasId(variable)) {
         variable.setId(new ObjectId().toHexString());
      }
      return variable;
   }

   @Override
   public boolean documentHasId(final ResourceVariable variable) {
      return variable.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final ResourceVariable variable) {
      if (!documentHasId(variable)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(variable.getId()));
   }

   @Override
   public ResourceVariable decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String key = bson.getString(KEY);
      Object value = bson.getString(VALUE);
      String resourceId = bson.getString(RESOURCE_ID);
      ResourceType resourceType = ResourceType.fromString(bson.getString(RESOURCE_TYPE));
      ResourceVariableType type = ResourceVariableType.fromString(bson.getString(TYPE));

      String organizationId = bson.getString(ORGANIZATION_ID);
      String projectId = bson.getString(PROJECT_ID);
      Boolean secure = bson.getBoolean(SECURE);

      return new ResourceVariable(id, resourceId, resourceType, key, value, type, secure, organizationId, projectId);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final ResourceVariable variable, final EncoderContext encoderContext) {
      Document bson = variable.getId() != null ? new Document(ID, new ObjectId(variable.getId())) : new Document();

      bson.append(KEY, variable.getKey())
          .append(VALUE, variable.getValue())
          .append(PROJECT_ID, variable.getProjectId())
          .append(ORGANIZATION_ID, variable.getOrganizationId())
          .append(RESOURCE_TYPE, variable.getResourceType())
          .append(TYPE, variable.getType() != null ? variable.getType().getValue() : null)
          .append(SECURE, variable.getSecure())
          .append(RESOURCE_TYPE, variable.getResourceType() != null ? variable.getResourceType().toString() : null)
          .append(RESOURCE_ID, variable.getResourceId());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<ResourceVariable> getEncoderClass() {
      return ResourceVariable.class;
   }

}

