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

import io.lumeer.api.model.FileAttachment;

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

public class FileAttachmentCodec implements CollectibleCodec<FileAttachment> {

   public static final String ID = "_id";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String PROJECT_ID = "projectId";
   public static final String COLLECTION_ID = "collectionId";
   public static final String DOCUMENT_ID = "documentId";
   public static final String ATTRIBUTE_ID = "attributeId";
   public static final String FILE_NAME = "fileName";
   public static final String ATTACHMENT_TYPE = "attachmentType";

   private final Codec<Document> documentCodec;

   public FileAttachmentCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public FileAttachment decode(final BsonReader reader, final DecoderContext decoderContext) {
      final Document bson = documentCodec.decode(reader, decoderContext);
      final String id = bson.getObjectId(ID).toHexString();
      final String organizationId = bson.getString(ORGANIZATION_ID);
      final String projectId = bson.getString(PROJECT_ID);
      final String collectionId = bson.getString(COLLECTION_ID);
      final String documentId = bson.getString(DOCUMENT_ID);
      final String attributeId = bson.getString(ATTRIBUTE_ID);
      final String fileName = bson.getString(FILE_NAME);
      final Integer typeInt = bson.getInteger(ATTACHMENT_TYPE);
      final FileAttachment.AttachmentType type = FileAttachment.AttachmentType.values()[typeInt != null ? typeInt : 0];

      final FileAttachment fileAttachment = new FileAttachment(organizationId, projectId, collectionId, documentId, attributeId, fileName, type);
      fileAttachment.setId(id);

      return fileAttachment;
   }

   @Override
   public void encode(final BsonWriter writer, final FileAttachment fileAttachment, final EncoderContext encoderContext) {
      final Document bson = fileAttachment.getId() != null ? new Document(ID, new ObjectId(fileAttachment.getId())) : new Document();
      bson.append(ORGANIZATION_ID, fileAttachment.getOrganizationId())
          .append(PROJECT_ID, fileAttachment.getProjectId())
          .append(COLLECTION_ID, fileAttachment.getCollectionId())
          .append(DOCUMENT_ID, fileAttachment.getDocumentId())
          .append(ATTRIBUTE_ID, fileAttachment.getAttributeId())
          .append(FILE_NAME, fileAttachment.getFileName())
          .append(ATTACHMENT_TYPE, fileAttachment.getAttachmentType().ordinal());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<FileAttachment> getEncoderClass() {
      return FileAttachment.class;
   }

   @Override
   public FileAttachment generateIdIfAbsentFromDocument(final FileAttachment fileAttachment) {
      if (!documentHasId(fileAttachment)) {
         fileAttachment.setId(new ObjectId().toHexString());
      }

      return fileAttachment;
   }

   @Override
   public boolean documentHasId(final FileAttachment fileAttachment) {
      return fileAttachment.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final FileAttachment fileAttachment) {
      if (!documentHasId(fileAttachment)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(fileAttachment.getId()));
   }

}
