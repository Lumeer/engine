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

import io.lumeer.api.model.User;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserCodec implements CollectibleCodec<User> {

   public static final String ID = "_id";
   public static final String NAME = "name";
   public static final String EMAIL = "email";
   public static final String KEYCLOAK_ID = "keycloakId";
   public static final String ALL_GROUPS = "allGroups";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String GROUPS = "groups";

   private final Codec<Document> documentCodec;

   public UserCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public User generateIdIfAbsentFromDocument(final User user) {
      if (!documentHasId(user)) {
         user.setId(new ObjectId().toHexString());
      }
      return user;
   }

   @Override
   public boolean documentHasId(final User user) {
      return user.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final User user) {
      if (!documentHasId(user)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(user.getId()));
   }

   @Override
   public User decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String name = bson.getString(NAME);
      String email = bson.getString(EMAIL);
      String keycloakId = bson.getString(KEYCLOAK_ID);

      List<Document> documentList = bson.get(ALL_GROUPS, List.class);
      Map<String, Set<String>> allGroups = convertGroupsListToMap(documentList);

      User user = new User(id, name, email, allGroups);
      user.setKeycloakId(keycloakId);

      return user;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final User user, final EncoderContext encoderContext) {
      Document bson = user.getId() != null ? new Document(ID, new ObjectId(user.getId())) : new Document();
      bson.append(NAME, user.getName())
          .append(EMAIL, user.getEmail())
          .append(KEYCLOAK_ID, user.getKeycloakId());

      if (user.getGroups() != null) {
         List<Document> groupsArray = user.getGroups().entrySet().stream().map(entry -> new Document(ORGANIZATION_ID, entry.getKey())
               .append(GROUPS, entry.getValue())
         ).collect(Collectors.toList());
         bson.append(ALL_GROUPS, groupsArray);
      } else {
         bson.append(ALL_GROUPS, Collections.emptyList());
      }

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<User> getEncoderClass() {
      return User.class;
   }

   private Map<String, Set<String>> convertGroupsListToMap(List<Document> documentList) {
      if (documentList == null) {
         return new HashMap<>();
      }

      return documentList.stream()
                         .collect(Collectors.toMap(document -> document.getString(ORGANIZATION_ID), this::convertGroupsListToSet));
   }

   private Set<String> convertGroupsListToSet(Document document) {
      List<String> groups = document.get(GROUPS, List.class);
      if (groups == null) {
         return new HashSet<>();
      }
      return new HashSet<>(groups);
   }
}

