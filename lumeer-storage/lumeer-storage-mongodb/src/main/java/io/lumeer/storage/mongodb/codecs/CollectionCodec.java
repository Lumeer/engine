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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.common.SimpleResource;
import io.lumeer.engine.api.data.DataDocument;

import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionCodec extends ResourceCodec implements CollectibleCodec<Collection> {

   public static final String ATTRIBUTES = "attributes";
   public static final String DOCUMENTS_COUNT = "docCount";
   public static final String LAST_TIME_USED = "lastTimeUsed";
   public static final String LAST_ATTRIBUTE_NUM = "lastAttributeNum";
   public static final String DEFAULT_ATTRIBUTE_ID = "defaultAttributeId";
   public static final String RULES = "rules";
   public static final String DATA_DESCRIPTION = "dataDescription";
   public static final String PURPOSE = "purpose";

   public CollectionCodec(final CodecRegistry registry) {
      super(registry);
   }

   @Override
   public Collection decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);
      SimpleResource resource = decodeResource(bson);

      Set<Attribute> attributes;
      List attributeList = bson.get(ATTRIBUTES, List.class);
      if (attributeList != null) {
         attributes = new ArrayList<Document>(attributeList).stream()
                                                            .map(AttributeCodec::convertFromDocument)
                                                            .collect(Collectors.toSet());
      } else {
         attributes = Collections.emptySet();
      }

      Map<String, Rule> rules = new HashMap<>();
      Document rulesMap = bson.get(RULES, Document.class);
      if (rulesMap != null) {
         rulesMap.forEach((k, v) -> {
            final Rule rule = RuleCodec.convertFromDocument(rulesMap.get(k, Document.class));
            rule.setId(k);
            if (StringUtils.isEmpty(rule.getName())) {
               rule.setName(k);
            }
            rules.put(k, rule);
         });
      }

      Integer documentsCount = bson.getInteger(DOCUMENTS_COUNT);
      Integer lastAttributeNum = bson.getInteger(LAST_ATTRIBUTE_NUM);
      Date lastTimeUsed = bson.getDate(LAST_TIME_USED);
      String defaultAttributeId = bson.getString(DEFAULT_ATTRIBUTE_ID);
      String dataDescription = bson.getString(DATA_DESCRIPTION);
      CollectionPurpose purpose;
      try {
         purpose = CollectionPurposeCodec.convertFromDocument(bson.get(PURPOSE, Document.class));
      } catch (ClassCastException e) {
         purpose = new CollectionPurpose(CollectionPurposeType.None, new DataDocument());
      }

      Collection collection = new Collection(resource.getCode(), resource.getName(), resource.getIcon(), resource.getColor(), resource.getDescription(), resource.getPermissions(), attributes, rules, dataDescription, purpose);
      collection.setId(resource.getId());
      collection.setDocumentsCount(documentsCount);
      if (lastTimeUsed != null) {
         collection.setLastTimeUsed(ZonedDateTime.ofInstant(lastTimeUsed.toInstant(), ZoneOffset.UTC));
      }
      collection.setDefaultAttributeId(defaultAttributeId);
      collection.setLastAttributeNum(lastAttributeNum);
      collection.setAttributes(attributes);
      collection.setVersion(resource.getVersion());

      return collection;
   }

   @Override
   public void encode(final BsonWriter writer, final Collection collection, final EncoderContext encoderContext) {
      Document bson = encodeResource(collection)
            .append(DOCUMENTS_COUNT, collection.getDocumentsCount())
            .append(DEFAULT_ATTRIBUTE_ID, collection.getDefaultAttributeId())
            .append(LAST_ATTRIBUTE_NUM, collection.getLastAttributeNum())
            .append(ATTRIBUTES, collection.getAttributes())
            .append(RULES, collection.getRules())
            .append(DATA_DESCRIPTION, collection.getDataDescription())
            .append(PURPOSE, collection.getPurpose());

      if (collection.getLastTimeUsed() != null) {
         bson.append(LAST_TIME_USED, Date.from(collection.getLastTimeUsed().toInstant()));
      }

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<Collection> getEncoderClass() {
      return Collection.class;
   }

   @Override
   public Collection generateIdIfAbsentFromDocument(final Collection jsonCollection) {
      Resource resource = generateIdIfAbsentFromDocument((Resource) jsonCollection);
      jsonCollection.setId(resource.getId());
      return jsonCollection;
   }

   @Override
   public boolean documentHasId(final Collection jsonCollection) {
      return documentHasId((Resource) jsonCollection);
   }

   @Override
   public BsonValue getDocumentId(final Collection jsonCollection) {
      return getDocumentId((Resource) jsonCollection);
   }
}
