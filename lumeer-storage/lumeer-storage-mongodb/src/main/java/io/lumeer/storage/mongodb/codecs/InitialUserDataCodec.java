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

import io.lumeer.api.model.InitialUserData;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.NotificationSetting;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InitialUserDataCodec implements Codec<InitialUserData> {

   public static final String PROJECT_ID = "projectId";
   public static final String LANGUAGE = "language";
   public static final String DASHBOARD = "dashboard";
   public static final String NOTIFICATIONS = "notifications";

   private final Codec<Document> configCodec;

   public InitialUserDataCodec(final CodecRegistry registry) {
      this.configCodec = registry.get(Document.class);
   }

   @Override
   public InitialUserData decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = configCodec.decode(bsonReader, decoderContext);

      String projectId = bson.getString(PROJECT_ID);
      Object dashboardConfig = bson.get(DASHBOARD);
      Language language = Language.fromString(bson.getString(LANGUAGE));
      List<Document> notificationsList = bson.getList(NOTIFICATIONS, Document.class);
      List<NotificationSetting> notifications = new ArrayList<>();
      if(notificationsList != null) {
         notifications = notificationsList.stream().map(NotificationSettingCodec::convertFromDocument).collect(Collectors.toList());
      }

      var data = new InitialUserData(dashboardConfig, notifications, language);
      data.setProjectId(projectId);

      return data;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final InitialUserData config, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(PROJECT_ID, config.getProjectId())
            .append(LANGUAGE, config.getLanguage() != null ? config.getLanguage().toString() : null)
            .append(DASHBOARD, config.getDashboard())
            .append(NOTIFICATIONS, config.getNotifications());


      configCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<InitialUserData> getEncoderClass() {
      return InitialUserData.class;
   }

}
