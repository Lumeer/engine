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

import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.LinkAttributeFilter;
import io.lumeer.api.model.QueryStem;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryStemCodec implements Codec<QueryStem> {

   public static final String ID = "stemId";
   public static final String COLLECTION_ID = "collectionId";
   public static final String LINK_TYPE_IDS = "linkTypeIds";
   public static final String DOCUMENT_IDS = "documentIds";
   public static final String FILTERS = "filters";
   public static final String LINK_FILTERS = "linkFilters";

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
      String stemId = bson.getString(ID);
      if (stemId == null) {
         stemId = new ObjectId().toString();
      }
      String collectionId = bson.getString(COLLECTION_ID);
      List<String> linkTypeIds = bson.get(LINK_TYPE_IDS, List.class);
      Set<String> documentIds = convertToSet(bson.get(DOCUMENT_IDS, List.class));

      List<CollectionAttributeFilter> attributes = new ArrayList<Document>(bson.get(FILTERS, List.class)).stream()
                                                                                                         .map(CollectionAttributeFilterCodec::convertFromDocument)
                                                                                                         .collect(Collectors.toList());

      List<LinkAttributeFilter> linkAttributes;
      if (bson.containsKey(LINK_FILTERS)) {
         linkAttributes = new ArrayList<Document>(bson.get(LINK_FILTERS, List.class)).stream()
                                                                                     .map(LinkAttributeFilterCodec::convertFromDocument)
                                                                                     .collect(Collectors.toList());
      } else {
         linkAttributes = new ArrayList<>();
      }

      return new QueryStem(stemId, collectionId, linkTypeIds, documentIds, attributes, linkAttributes);
   }

   private static Set<String> convertToSet(List list) {
      return list != null ? new HashSet<>(list) : Collections.emptySet();
   }

   @Override
   public void encode(final BsonWriter writer, final QueryStem value, final EncoderContext encoderContext) {
      Document document = new Document()
            .append(ID, Objects.requireNonNullElse(value.getId(), new ObjectId().toString()))
            .append(COLLECTION_ID, value.getCollectionId())
            .append(LINK_TYPE_IDS, value.getLinkTypeIds())
            .append(DOCUMENT_IDS, new ArrayList<>(value.getDocumentIds()))
            .append(FILTERS, value.getFilters())
            .append(LINK_FILTERS, value.getLinkFilters());

      documentCodec.encode(writer, document, encoderContext);
   }

   @Override
   public Class<QueryStem> getEncoderClass() {
      return QueryStem.class;
   }

}
