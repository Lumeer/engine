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

import io.lumeer.api.model.AttributeFilter;
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

public class QueryStemCodec implements Codec<QueryStem> {

   public static final String COLLECTION_ID = "collectionId";
   public static final String LINK_TYPE_IDS = "linkTypeIds";
   public static final String DOCUMENT_IDS = "documentIds";
   public static final String FILTERS = "filters";

   private final Codec<Document> documentCodec;

   public QueryStemCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public QueryStem decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(reader, decoderContext);

      return QueryStemCodec.convertFromDocument(document);
   }

   public static QueryStem convertFromDocument(Document bson) {

      String collectionId = bson.getString(COLLECTION_ID);
      List<String> linkTypeIds = bson.get(LINK_TYPE_IDS, List.class);
      Set<String> documentIds = convertToSet(bson.get(DOCUMENT_IDS, List.class));

      Set<AttributeFilter> attributes = new ArrayList<Document>(bson.get(FILTERS, List.class)).stream()
                                                                                              .map(AttributeFilterCodec::convertFromDocument)
                                                                                              .collect(Collectors.toSet());

      return new QueryStem(collectionId, linkTypeIds, documentIds, attributes);
   }

   private static Set<String> convertToSet(List list) {
      return list != null ? new HashSet<>(list) : Collections.emptySet();
   }

   @Override
   public void encode(final BsonWriter writer, final QueryStem value, final EncoderContext encoderContext) {
      Document document = new Document()
            .append(COLLECTION_ID, value.getCollectionId())
            .append(LINK_TYPE_IDS, value.getLinkTypeIds())
            .append(DOCUMENT_IDS, new ArrayList<>(value.getDocumentIds()))
            .append(FILTERS, new ArrayList<>(value.getFilters()));

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<QueryStem> getEncoderClass() {
      return QueryStem.class;
   }

}
