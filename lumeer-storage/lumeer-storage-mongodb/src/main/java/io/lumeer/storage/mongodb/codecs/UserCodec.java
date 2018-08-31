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

import io.lumeer.api.model.DefaultWorkspace;
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
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
   public static final String GROUPS = "groups";
   public static final String WISHES = "wishes";
   public static final String AUTH_IDS = "authIds";
   public static final String AGREEMENT = "agreement";
   public static final String AGREEMENT_DATE = "agreementDate";
   public static final String NEWSLETTER = "newsletter";

   public static final String DEFAULT_ORGANIZATION_ID = "defaultOrganizationId";
   public static final String DEFAULT_PROJECT_ID = "defaultProjectId";

   public static final String ALL_GROUPS = "allGroups";
   public static final String ORGANIZATION_ID = "organizationId";

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
      List<String> authIds = bson.get(AUTH_IDS, List.class);

      List<Document> documentList = bson.get(ALL_GROUPS, List.class);
      Map<String, Set<String>> allGroups = convertListToMap(documentList, GROUPS);

      String defaultOrganizationId = bson.getString(DEFAULT_ORGANIZATION_ID);
      String defaultProjectId = bson.getString(DEFAULT_PROJECT_ID);

      List<String> wishes = bson.get(User.WISHES, List.class);

      Boolean agreement = bson.getBoolean(AGREEMENT);
      ZonedDateTime agreementDate = null;
      if (bson.getDate(AGREEMENT_DATE) != null) {
         agreementDate = ZonedDateTime.ofInstant(bson.getDate(AGREEMENT_DATE).toInstant(), ZoneOffset.UTC);
      }
      Boolean newsletter = bson.getBoolean(NEWSLETTER);

      User user = new User(id, name, email, allGroups, wishes, agreement, agreementDate, newsletter);
      user.setAuthIds(authIds != null ? new HashSet<>(authIds) : new HashSet<>());
      user.setDefaultWorkspace(new DefaultWorkspace(defaultOrganizationId, defaultProjectId));

      return user;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final User user, final EncoderContext encoderContext) {
      Document bson = user.getId() != null ? new Document(ID, new ObjectId(user.getId())) : new Document();
      bson.append(NAME, user.getName())
          .append(EMAIL, user.getEmail())
          .append(AUTH_IDS, user.getAuthIds())
          .append(WISHES, user.getWishes());

      if (user.getDefaultWorkspace() != null) {
         bson.append(DEFAULT_ORGANIZATION_ID, user.getDefaultWorkspace().getOrganizationId());
         bson.append(DEFAULT_PROJECT_ID, user.getDefaultWorkspace().getProjectId());
      }

      if (user.getGroups() != null) {
         bson.append(ALL_GROUPS, convertMapToList(user.getGroups(), GROUPS));
      } else {
         bson.append(ALL_GROUPS, Collections.emptyList());
      }

      bson.append(AGREEMENT, user.hasAgreement());
      if (user.getAgreementDate() != null) {
         bson.append(AGREEMENT_DATE, new Date(user.getAgreementDate().toInstant().toEpochMilli()));
      }
      bson.append(NEWSLETTER, user.hasNewsletter());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<User> getEncoderClass() {
      return User.class;
   }

   private List<Document> convertMapToList(Map<String, Set<String>> map, String key) {
      return map.entrySet().stream().map(entry -> new Document(ORGANIZATION_ID, entry.getKey())
            .append(key, entry.getValue())
      ).collect(Collectors.toList());
   }

   private Map<String, Set<String>> convertListToMap(List<Document> documentList, String key) {
      if (documentList == null) {
         return new HashMap<>();
      }

      return documentList.stream()
                         .collect(Collectors.toMap(document -> document.getString(ORGANIZATION_ID), document -> convertListToSet(document, key)));
   }

   private Set<String> convertListToSet(Document document, String key) {
      List<String> groups = document.get(key, List.class);
      if (groups == null) {
         return new HashSet<>();
      }
      return new HashSet<>(groups);
   }
}

