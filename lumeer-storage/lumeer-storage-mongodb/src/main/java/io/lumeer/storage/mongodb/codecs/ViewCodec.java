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

import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ViewCodec extends ResourceCodec implements CollectibleCodec<View> {

   public static final String QUERY = "query";
   public static final String ADDITIONAL_QUERIES = "additionalQueries";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";
   public static final String SETTINGS = "settings";
   public static final String AUTHOR_ID = "authorId";
   public static final String LAST_TIME_USED = "lastTimeUsed";
   public static final String FOLDERS = "folders";

   public ViewCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public View decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      SimpleResource resource = decodeResource(bson, ResourceType.VIEW);

      Query query = QueryCodec.convertFromDocument(bson.get(QUERY, Document.class));
      Perspective perspective = decodePerspective(bson);
      Object config = bson.get(CONFIG);
      Object settings = bson.get(SETTINGS);
      String authorId = bson.getString(AUTHOR_ID);
      Date lastTimeUsed = bson.getDate(LAST_TIME_USED);
      List<String> folders = bson.getList(FOLDERS, String.class);
      List<Query> additionalQueries = Collections.emptyList();
      List queriesList = bson.get(ADDITIONAL_QUERIES, List.class);
      if (queriesList != null) {
         additionalQueries = new ArrayList<Document>(queriesList).stream()
                                                                 .map(QueryCodec::convertFromDocument)
                                                                 .collect(Collectors.toList());
      }

      View view = new View(resource.getCode(), resource.getName(), resource.getIcon(), resource.getColor(), resource.getDescription(), resource.getPriority(), resource.getPermissions(), query, additionalQueries, perspective, config, settings, authorId, folders);
      view.setId(resource.getId());
      view.setVersion(resource.getVersion());

      if (lastTimeUsed != null) {
         view.setLastTimeUsed(ZonedDateTime.ofInstant(lastTimeUsed.toInstant(), ZoneOffset.UTC));
      }

      return view;
   }

   private Perspective decodePerspective(Document bson) {
      String perspective = bson.getString(PERSPECTIVE);
      if ("postit".equals(perspective)) {
         return Perspective.Kanban;
      }
      return Objects.requireNonNullElse(Perspective.fromString(perspective), Perspective.Search);
   }

   @Override
   public void encode(final BsonWriter writer, final View value, final EncoderContext encoderContext) {
      Document bson = encodeResource(value)
            .append(QUERY, value.getQuery())
            .append(ADDITIONAL_QUERIES, value.getAdditionalQueries())
            .append(PERSPECTIVE, value.getPerspective() != null ? value.getPerspective().getValue() : null)
            .append(CONFIG, value.getConfig())
            .append(SETTINGS, value.getSettings())
            .append(AUTHOR_ID, value.getAuthorId())
            .append(FOLDERS, value.getFolders());

      if (value.getLastTimeUsed() != null) {
         bson.append(LAST_TIME_USED, Date.from(value.getLastTimeUsed().toInstant()));
      }

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<View> getEncoderClass() {
      return View.class;
   }

   @Override
   public View generateIdIfAbsentFromDocument(final View jsonView) {
      Resource resource = generateIdIfAbsentFromDocument((Resource) jsonView);
      jsonView.setId(resource.getId());
      return jsonView;
   }

   @Override
   public boolean documentHasId(final View jsonView) {
      return documentHasId((Resource) jsonView);
   }

   @Override
   public BsonValue getDocumentId(final View jsonView) {
      return getDocumentId((Resource) jsonView);
   }
}
