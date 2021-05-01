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

import io.lumeer.api.model.Organization;
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

public class OrganizationCodec extends ResourceCodec implements CollectibleCodec<Organization> {

   public OrganizationCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public Organization decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      SimpleResource resource = decodeResource(bson);

      Organization organization = new Organization(resource.getCode(), resource.getName(), resource.getIcon(), resource.getColor(), resource.getDescription(), resource.getPriority(), resource.getPermissions());
      organization.setId(resource.getId());
      organization.setVersion(resource.getVersion());
      return organization;
   }

   @Override
   public void encode(final BsonWriter writer, final Organization organization, final EncoderContext encoderContext) {
      Document bson = encodeResource(organization);

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Organization> getEncoderClass() {
      return Organization.class;
   }

   @Override
   public Organization generateIdIfAbsentFromDocument(final Organization jsonOrganization) {
      Resource resource = generateIdIfAbsentFromDocument((Resource) jsonOrganization);
      jsonOrganization.setId(resource.getId());
      return jsonOrganization;
   }

   @Override
   public boolean documentHasId(final Organization jsonOrganization) {
      return documentHasId((Resource) jsonOrganization);
   }

   @Override
   public BsonValue getDocumentId(final Organization jsonOrganization) {
      return getDocumentId((Resource) jsonOrganization);
   }
}
