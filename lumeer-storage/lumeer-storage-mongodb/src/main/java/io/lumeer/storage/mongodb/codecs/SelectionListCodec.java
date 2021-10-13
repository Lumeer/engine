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

import io.lumeer.api.model.SelectOption;
import io.lumeer.api.model.SelectionList;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SelectionListCodec implements CollectibleCodec<SelectionList> {

   public static final String ID = "_id";
   public static final String NAME = "name";
   public static final String PROJECT_ID = "projectId";
   public static final String DESCRIPTION = "description";
   public static final String DISPLAY_VALUES = "displayValues";
   public static final String OPTIONS = "options";

   private final Codec<Document> documentCodec;

   public SelectionListCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public SelectionList generateIdIfAbsentFromDocument(final SelectionList selectionList) {
      if (!documentHasId(selectionList)) {
         selectionList.setId(new ObjectId().toHexString());
      }
      return selectionList;
   }

   @Override
   public boolean documentHasId(final SelectionList list) {
      return list.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final SelectionList selectionList) {
      if (!documentHasId(selectionList)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(selectionList.getId()));
   }

   @Override
   public SelectionList decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String name = bson.getString(NAME);
      String description = bson.getString(DESCRIPTION);
      String projectId = bson.getString(PROJECT_ID);
      Boolean displayValues = bson.getBoolean(DISPLAY_VALUES);

      List<SelectOption> options;
      List optionsList = bson.get(OPTIONS, List.class);
      if (optionsList != null) {
         options = new ArrayList<Document>(optionsList).stream()
                                                            .map(SelectOptionCodec::convertFromDocument)
                                                            .collect(Collectors.toList());
      } else {
         options = Collections.emptyList();
      }

      return new SelectionList(id, name, description, null, projectId, displayValues, options);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final SelectionList selectionList, final EncoderContext encoderContext) {
      Document bson = selectionList.getId() != null ? new Document(ID, new ObjectId(selectionList.getId())) : new Document();
      bson.append(NAME, selectionList.getName())
          .append(DESCRIPTION, selectionList.getDescription())
          .append(PROJECT_ID, selectionList.getProjectId())
          .append(DISPLAY_VALUES, selectionList.getDisplayValues())
          .append(OPTIONS, selectionList.getOptions());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<SelectionList> getEncoderClass() {
      return SelectionList.class;
   }

}

