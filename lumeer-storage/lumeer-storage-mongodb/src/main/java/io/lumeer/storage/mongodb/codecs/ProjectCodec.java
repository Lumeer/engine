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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.TemplateMetadata;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.common.SimpleResource;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ProjectCodec extends ResourceCodec implements CollectibleCodec<Project> {

   public static final String TEMPLATE_METADATA = "templateMetadata";
   public static final String TEMPLATE_ID = "templateId";
   public static final String IS_PUBLIC = "isPublic";

   public ProjectCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public Project decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      SimpleResource resource = decodeResource(bson, ResourceType.PROJECT);
      TemplateMetadata templateMetadata;
      if (bson.containsKey(TEMPLATE_METADATA)) {
         templateMetadata = TemplateMetadataCodec.convertFromDocument(bson.get(TEMPLATE_METADATA, Document.class));
      } else {
         templateMetadata = null;
      }

      boolean isPublic = bson.getBoolean(IS_PUBLIC, false);
      String templateId = bson.getString(TEMPLATE_ID);
      Project project = new Project(resource, isPublic, templateMetadata);
      project.setVersion(resource.getVersion());
      project.setTemplateId(templateId);
      return project;
   }

   @Override
   public void encode(final BsonWriter writer, final Project project, final EncoderContext encoderContext) {
      Document bson = encodeResource(project)
            .append(IS_PUBLIC, project.isPublic())
            .append(TEMPLATE_ID, project.getTemplateId());
      if (project.getTemplateMetadata() != null) {
         bson = bson.append(TEMPLATE_METADATA, project.getTemplateMetadata());
      }

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Project> getEncoderClass() {
      return Project.class;
   }

   @Override
   public Project generateIdIfAbsentFromDocument(final Project jsonProject) {
      Resource resource = generateIdIfAbsentFromDocument((Resource) jsonProject);
      jsonProject.setId(resource.getId());
      return jsonProject;
   }

   @Override
   public boolean documentHasId(final Project jsonProject) {
      return documentHasId((Resource) jsonProject);
   }

   @Override
   public BsonValue getDocumentId(final Project jsonProject) {
      return getDocumentId((Resource) jsonProject);
   }
}
