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

import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryCodec implements Codec<Query> {

   public static final String STEMS = "stems";
   public static final String FULLTEXTS = "fulltexts";
   public static final String PAGE = "page";
   public static final String PAGE_SIZE = "pageSize";

   private final Codec<Document> documentCodec;

   public QueryCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Query decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return QueryCodec.convertFromDocument(document);
   }

   public static Query convertFromDocument(Document bson) {
      if (bson == null) {
         return new Query();
      }

      Set<String> fulltexts = convertToSet(bson.get(FULLTEXTS, List.class));
      List<QueryStem> stems;
      if (bson.containsKey(STEMS)) {
         stems = new ArrayList<Document>(bson.get(STEMS, List.class)).stream()
                                                             .map(QueryStemCodec::convertFromDocument)
                                                             .collect(Collectors.toList());
      } else {
         stems = Collections.emptyList();
      }

      Integer page = bson.getInteger(PAGE);
      Integer pageSize = bson.getInteger(PAGE_SIZE);

      return new Query(stems, fulltexts, page, pageSize);
   }

   private static Set<String> convertToSet(List list) {
      return list != null ? new HashSet<>(list) : Collections.emptySet();
   }

   @Override
   public void encode(final BsonWriter writer, final Query value, final EncoderContext encoderContext) {
      Document document = new Document()
            .append(STEMS, value.getStems())
            .append(FULLTEXTS, new ArrayList<>(value.getFulltexts()))
            .append(PAGE, value.getPage())
            .append(PAGE_SIZE, value.getPageSize());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<Query> getEncoderClass() {
      return Query.class;
   }

}
