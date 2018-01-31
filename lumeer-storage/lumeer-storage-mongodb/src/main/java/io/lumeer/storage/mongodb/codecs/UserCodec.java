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

import io.lumeer.storage.mongodb.model.MongoUser;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserCodec implements CollectibleCodec<MongoUser> {

   public static final String ID = "_id";
   public static final String NAME = "name";
   public static final String EMAIL = "email";
   public static final String KEYCLOCK_ID = "keycloakId";
   public static final String ALL_GROUPS = "allGroups";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String GROUPS = "groups";

   private final Codec<Document> documentCodec;

   public UserCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public MongoUser generateIdIfAbsentFromDocument(final MongoUser user) {
      if (!documentHasId(user)) {
         user.setId(new ObjectId().toHexString());
      }
      return user;
   }

   @Override
   public boolean documentHasId(final MongoUser user) {
      return user.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final MongoUser user) {
      if (!documentHasId(user)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(user.getId()));
   }

   @Override
   public MongoUser decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String name = bson.getString(NAME);
      String email = bson.getString(EMAIL);
      String keycloakId = bson.getString(KEYCLOCK_ID);

      List<Document> documentList = bson.get(ALL_GROUPS, List.class);
      Map<String, Set<String>> allGroups = convertGroupsListToMap(documentList);

      return new MongoUser(id, name, email, keycloakId, allGroups);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final MongoUser user, final EncoderContext encoderContext) {
      Document bson = user.getId() != null ? new Document(ID, new ObjectId(user.getId())) : new Document();
      bson.append(NAME, user.getName())
          .append(EMAIL, user.getEmail())
          .append(KEYCLOCK_ID, user.getKeycloakId());

      if (user.getGroups() != null) {
         List<Document> groupsArray = user.getGroups().entrySet().stream().map(entry -> new Document(ORGANIZATION_ID, entry.getKey())
               .append(GROUPS, entry.getValue())
         ).collect(Collectors.toList());
         bson.append(ALL_GROUPS, groupsArray);
      }

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<MongoUser> getEncoderClass() {
      return MongoUser.class;
   }

   private Map<String, Set<String>> convertGroupsListToMap(List<Document> documentList) {
      if (documentList == null) {
         return Collections.emptyMap();
      }

      return documentList.stream()
                         .collect(Collectors.toMap(document -> document.getString(ORGANIZATION_ID), this::convertGroupsListToSet));
   }

   private Set<String> convertGroupsListToSet(Document document) {
      List<String> groups = document.get(GROUPS, List.class);
      if (groups == null) {
         return Collections.emptySet();
      }
      return new HashSet<>(groups);
   }
}

