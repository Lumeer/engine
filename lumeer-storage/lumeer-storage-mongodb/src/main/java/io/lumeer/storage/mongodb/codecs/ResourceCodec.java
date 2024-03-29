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

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.common.SimpleResource;

import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public abstract class ResourceCodec {

   public static final String ID = "_id";
   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String VERSION = "version";
   public static final String COLOR = "color";
   public static final String PRIORITY = "priority";
   public static final String DESCRIPTION = "description";
   public static final String PERMISSIONS = "permissions";
   public static final String ROLES = "roles";
   public static final String CREATION_DATE = "creationDate";
   public static final String UPDATE_DATE = "updateDate";
   public static final String CREATED_BY = "createdBy";
   public static final String UPDATED_BY = "updatedBy";

   protected final Codec<Document> documentCodec;

   protected ResourceCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   protected SimpleResource decodeResource(final Document bson, final ResourceType resourceType) {
      String id = bson.getObjectId(ID).toHexString();
      String code = bson.getString(CODE);
      String name = bson.getString(NAME);
      String icon = bson.getString(ICON);
      String color = bson.getString(COLOR);
      Long version = bson.getLong(VERSION);
      Long order = bson.getLong(PRIORITY);
      String description = bson.getString(DESCRIPTION);
      Permissions permissions;
      if (bson.containsKey(ROLES)) {
         permissions = PermissionsCodec.convertFromDocument(bson.get(ROLES, Document.class));
      } else {
         permissions = PermissionsCodec.convertFromDocumentLegacy(bson.get(PERMISSIONS, Document.class), resourceType);
      }
      Date creationDate = bson.getDate(CREATION_DATE);
      ZonedDateTime creationZonedDate = creationDate != null ? ZonedDateTime.ofInstant(creationDate.toInstant(), ZoneOffset.UTC) : null;
      String createdBy = bson.getString(CREATED_BY);
      Date updateDate = bson.getDate(UPDATE_DATE);
      ZonedDateTime updatedZonedDate = updateDate != null ? ZonedDateTime.ofInstant(updateDate.toInstant(), ZoneOffset.UTC) : null;
      String updatedBy = bson.getString(UPDATED_BY);

      SimpleResource resource = new SimpleResource(code, name, icon, color, description, order, permissions);
      resource.setId(id);
      resource.setVersion(version == null ? 0 : version);
      resource.setCreatedBy(createdBy);
      resource.setCreationDate(creationZonedDate);
      resource.setUpdatedBy(updatedBy);
      resource.setUpdateDate(updatedZonedDate);
      return resource;
   }

   protected Document encodeResource(Resource value) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(CODE, value.getCode())
          .append(NAME, value.getName())
          .append(ICON, value.getIcon())
          .append(COLOR, value.getColor())
          .append(PRIORITY, value.getPriority())
          .append(DESCRIPTION, value.getDescription())
          .append(ROLES, value.getPermissions())
          .append(CREATED_BY, value.getCreatedBy())
          .append(UPDATED_BY, value.getUpdatedBy());

      if (value.getCreationDate() != null) {
         bson.append(CREATION_DATE, Date.from(value.getCreationDate().toInstant()));
      }
      if (value.getUpdateDate() != null) {
         bson.append(UPDATE_DATE, Date.from(value.getUpdateDate().toInstant()));
      }

      return bson;
   }

   protected Resource generateIdIfAbsentFromDocument(final Resource document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   protected boolean documentHasId(final Resource document) {
      return document.getId() != null;
   }

   protected BsonValue getDocumentId(final Resource document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}
