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
import io.lumeer.api.model.LinkPermissionsType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Rule;

import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkTypeCodec implements CollectibleCodec<LinkType> {

   public static final String ID = "_id";
   public static final String NAME = "name";
   public static final String COLLECTION_IDS = "collectionIds";
   public static final String VERSION = "version";
   public static final String ATTRIBUTES = "attributes";
   public static final String LAST_ATTRIBUTE_NUM = "lastAttributeNum";
   public static final String RULES = "rules";
   public static final String ROLES = "roles";
   public static final String ROLES_TYPE = "rolesType";

   private final Codec<Document> documentCodec;

   public LinkTypeCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public LinkType decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String name = bson.getString(NAME);
      List<String> collectionCodes = bson.get(COLLECTION_IDS, List.class);
      List<Attribute> attributes = new ArrayList<Document>(bson.get(ATTRIBUTES, List.class)).stream()
                                                                                            .map(AttributeCodec::convertFromDocument)
                                                                                            .collect(Collectors.toList());
      Long version = bson.getLong(VERSION);
      Integer lastAttributeNum = bson.getInteger(LAST_ATTRIBUTE_NUM);

      Map<String, Rule> rules = new HashMap<>();
      Document rulesMap = bson.get(RULES, Document.class);
      if (rulesMap != null) {
         rulesMap.forEach((k, v) -> {
            final Rule rule = RuleCodec.convertFromDocument(rulesMap.get(k, Document.class));
            if (StringUtils.isEmpty(rule.getName())) {
               rule.setName(k);
            }
            rules.put(k, rule);
         });
      }

      Permissions permissions = PermissionsCodec.convertFromDocument(bson.get(ROLES, Document.class));
      LinkPermissionsType permissionsType = LinkPermissionsType.fromString(bson.getString(ROLES_TYPE));

      LinkType linkType = new LinkType(name, collectionCodes, attributes, rules, permissions, permissionsType);
      linkType.setId(id);
      linkType.setVersion(version == null ? 0 : version);
      linkType.setLastAttributeNum(lastAttributeNum);
      return linkType;
   }

   @Override
   public void encode(final BsonWriter writer, final LinkType value, final EncoderContext encoderContext) {
      Document bson = value.getId() != null ? new Document(ID, new ObjectId(value.getId())) : new Document();
      bson.append(NAME, value.getName())
          .append(COLLECTION_IDS, value.getCollectionIds())
          .append(ATTRIBUTES, value.getAttributes())
          .append(LAST_ATTRIBUTE_NUM, value.getLastAttributeNum())
          .append(ROLES, value.getPermissions())
          .append(ROLES_TYPE, value.getPermissionsType() != null ? value.getPermissionsType().toString() : null)
          .append(RULES, value.getRules());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<LinkType> getEncoderClass() {
      return LinkType.class;
   }

   @Override
   public LinkType generateIdIfAbsentFromDocument(final LinkType document) {
      if (!documentHasId(document)) {
         document.setId(new ObjectId().toHexString());
      }
      return document;
   }

   @Override
   public boolean documentHasId(final LinkType document) {
      return document.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final LinkType document) {
      if (!documentHasId(document)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(document.getId()));
   }
}

