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

import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.dto.common.JsonResource;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ProjectCodec extends ResourceCodec implements CollectibleCodec<JsonProject> {

   public ProjectCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public JsonProject decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      JsonResource resource = decodeResource(bson);

      JsonProject project = new JsonProject(resource.getCode(), resource.getName(), resource.getIcon(), resource.getColor(), resource.getDescription(), (JsonPermissions) resource.getPermissions());
      project.setId(resource.getId());
      return project;
   }

   @Override
   public void encode(final BsonWriter writer, final JsonProject project, final EncoderContext encoderContext) {
      Document bson = encodeResource(project);

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<JsonProject> getEncoderClass() {
      return JsonProject.class;
   }

   @Override
   public JsonProject generateIdIfAbsentFromDocument(final JsonProject jsonProject) {
      JsonResource resource = generateIdIfAbsentFromDocument((JsonResource) jsonProject);
      jsonProject.setId(resource.getId());
      return jsonProject;
   }

   @Override
   public boolean documentHasId(final JsonProject jsonProject) {
      return documentHasId((JsonResource) jsonProject);
   }

   @Override
   public BsonValue getDocumentId(final JsonProject jsonProject) {
      return getDocumentId((JsonResource) jsonProject);
   }
}
