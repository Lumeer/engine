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

import io.lumeer.api.model.TemplateMetadata;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class TemplateMetadataCodec implements Codec<TemplateMetadata> {

   public static final String IS_EDITABLE = "isEditable";
   public static final String IS_TEMPLATE = "isTemplate";
   public static final String SHOW_TOP_PANEL = "showTopPanel";
   public static final String TAGS = "tags";
   public static final String IMAGE_URL = "imageUrl";
   public static final String RELATIVE_DATE = "relativeDate";
   public static final String DEFAULT_VIEW = "defaultView";
   public static final String ALLOWED_DOMAINS = "allowedDomains";

   private final Codec<Document> documentCodec;

   public TemplateMetadataCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public TemplateMetadata decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return TemplateMetadataCodec.convertFromDocument(document);
   }

   public static TemplateMetadata convertFromDocument(Document bson) {
      boolean editable = true; // bson.getBoolean(IS_EDITABLE);
      boolean isTemplate = bson.getBoolean(IS_TEMPLATE, false);
      boolean showTopPanel = bson.getBoolean(SHOW_TOP_PANEL);
      String imageUrl = bson.getString(IMAGE_URL);
      List<String> tags = bson.getList(TAGS, String.class);
      Long relativeDate = bson.getLong(RELATIVE_DATE);
      String defaultView = bson.getString(DEFAULT_VIEW);
      String allowedDomains = bson.getString(ALLOWED_DOMAINS);

      return new TemplateMetadata(imageUrl, showTopPanel, editable, isTemplate, tags, relativeDate, defaultView, allowedDomains);
   }

   @Override
   public void encode(final BsonWriter writer, final TemplateMetadata value, final EncoderContext encoderContext) {
      Document document = new Document(IS_EDITABLE, value.isEditable())
            .append(IS_TEMPLATE, value.isTemplate())
            .append(SHOW_TOP_PANEL, value.isShowTopPanel())
            .append(RELATIVE_DATE, value.getRelativeDate())
            .append(IMAGE_URL, value.getImageUrl())
            .append(TAGS, value.getTags())
            .append(DEFAULT_VIEW, value.getDefaultView())
            .append(ALLOWED_DOMAINS, value.getAllowedDomains());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<TemplateMetadata> getEncoderClass() {
      return TemplateMetadata.class;
   }

}
