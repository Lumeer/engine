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

import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.data.DataDocument;

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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class ResourceCommentCodec implements CollectibleCodec<ResourceComment> {

   public static final String ID = "_id";
   public static final String RESOURCE_TYPE = "resourceType";
   public static final String RESOURCE_ID = "resourceId";
   public static final String PARENT_ID = "parentId";
   public static final String CREATION_DATE = "creationDate";
   public static final String UPDATE_DATE = "updateDate";
   public static final String AUTHOR = "author";
   public static final String AUTHOR_NAME = "authorName";
   public static final String AUTHOR_EMAIL = "authorEmail";
   public static final String COMMENT = "comment";
   public static final String META_DATA = "metaData";

   private final Codec<Document> documentCodec;

   public ResourceCommentCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(org.bson.Document.class);
   }

   @Override
   public ResourceComment decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      final ResourceComment comment = new ResourceComment(bson.getString(COMMENT), null);
      comment.setId(bson.getObjectId(ID).toHexString());
      comment.setResourceType(ResourceType.fromString(bson.getString(RESOURCE_TYPE)));
      comment.setResourceId(bson.getString(RESOURCE_ID));
      comment.setParentId(bson.getString(PARENT_ID));

      final Date creationDate = bson.getDate(CREATION_DATE);
      comment.setCreationDate(creationDate != null ? ZonedDateTime.ofInstant(creationDate.toInstant(), ZoneOffset.UTC) : null);
      final Date updatedDate = bson.getDate(UPDATE_DATE);
      comment.setUpdateDate(updatedDate != null ? ZonedDateTime.ofInstant(updatedDate.toInstant(), ZoneOffset.UTC) : null);

      comment.setAuthor(bson.getString(AUTHOR));
      comment.setAuthorEmail(bson.getString(AUTHOR_EMAIL));
      comment.setAuthorName(bson.getString(AUTHOR_NAME));

      Document metaData = bson.get(META_DATA, Document.class);
      comment.setMetaData(new DataDocument(metaData != null ? metaData : new Document()));

      return comment;
   }


   @Override
   public void encode(final BsonWriter writer, final ResourceComment value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(RESOURCE_TYPE, value.getResourceType().toString())
          .append(RESOURCE_ID, value.getResourceId())
          .append(PARENT_ID, value.getParentId())
          .append(AUTHOR, value.getAuthor())
          .append(AUTHOR_EMAIL, value.getAuthorEmail())
          .append(AUTHOR_NAME, value.getAuthorName())
          .append(COMMENT, value.getComment())
          .append(META_DATA, value.getMetaData());

      if (value.getCreationDate() != null) {
         bson.append(CREATION_DATE, Date.from(value.getCreationDate().toInstant()));
      }

      if (value.getUpdateDate() != null) {
         bson.append(UPDATE_DATE, Date.from(value.getUpdateDate().toInstant()));
      }

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<ResourceComment> getEncoderClass() {
      return ResourceComment.class;
   }

   @Override
   public ResourceComment generateIdIfAbsentFromDocument(final ResourceComment document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final ResourceComment document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final ResourceComment document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }

}
