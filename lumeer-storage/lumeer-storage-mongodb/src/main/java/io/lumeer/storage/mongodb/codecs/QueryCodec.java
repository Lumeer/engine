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

import io.lumeer.api.dto.JsonQuery;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryCodec implements Codec<JsonQuery> {

   public static final String COLLECTION_CODES = "collections";
   public static final String FILTERS = "filters";
   public static final String FULLTEXT = "fulltext";
   public static final String PAGE = "page";
   public static final String PAGE_SIZE = "pageSize";

   private final Codec<Document> documentCodec;

   public QueryCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public JsonQuery decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return QueryCodec.convertFromDocument(document);
   }

   public static JsonQuery convertFromDocument(Document bson) {
      if (bson == null) {
         return new JsonQuery();
      }

      Set<String> collectionCodes = new HashSet<String>(bson.get(COLLECTION_CODES, List.class));
      Set<String> filters = new HashSet<String>(bson.get(FILTERS, List.class));
      String fulltext = bson.getString(FULLTEXT);
      Integer page = bson.getInteger(PAGE);
      Integer pageSize = bson.getInteger(PAGE_SIZE);

      return new JsonQuery(collectionCodes, filters, fulltext, page, pageSize);
   }

   @Override
   public void encode(final BsonWriter writer, final JsonQuery value, final EncoderContext encoderContext) {
      Document document = new Document()
            .append(COLLECTION_CODES, new ArrayList<>(value.getCollectionCodes()))
            .append(FILTERS, new ArrayList<>(value.getFilters()))
            .append(FULLTEXT, value.getFulltext())
            .append(PAGE, value.getPage())
            .append(PAGE_SIZE, value.getPageSize());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<JsonQuery> getEncoderClass() {
      return JsonQuery.class;
   }

}
