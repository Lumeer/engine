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

import io.lumeer.api.model.ResourcesPermissions;
import io.lumeer.api.model.ViewSettings;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class ViewSettingsCodec implements Codec<ViewSettings> {
   public static final String ATTRIBUTES = "attributes";
   public static final String DATA = "data";
   public static final String MODALS = "modals";
   public static final String PERMISSIONS = "permissions";

   private final Codec<Document> documentCodec;

   public ViewSettingsCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public ViewSettings decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return ViewSettingsCodec.convertFromDocument(document);
   }

   public static ViewSettings convertFromDocument(final Document document) {
      if (document == null) {
         return new ViewSettings();
      }

      Object attributes = document.get(ATTRIBUTES);
      Object data = document.get(DATA);
      Object modals = document.get(MODALS);
      ResourcesPermissions permissions = ResourcesPermissionsCodec.convertFromDocument(document.get(PERMISSIONS, Document.class));

      return new ViewSettings(attributes, data, modals, permissions);
   }


   @Override
   public void encode(final BsonWriter writer, final ViewSettings value, final EncoderContext encoderContext) {
      Document bson = new Document(ATTRIBUTES, value.getAttributes())
            .append(DATA, value.getData())
            .append(MODALS, value.getModals())
            .append(PERMISSIONS, value.getPermissions());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<ViewSettings> getEncoderClass() {
      return ViewSettings.class;
   }

}
