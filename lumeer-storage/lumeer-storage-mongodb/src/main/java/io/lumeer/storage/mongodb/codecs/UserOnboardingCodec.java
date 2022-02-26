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

import io.lumeer.api.model.UserOnboarding;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class UserOnboardingCodec implements Codec<UserOnboarding> {

   public static final String TEMPLATE = "template";
   public static final String INVITED_USERS = "invitedUsers";
   public static final String HELP_OPENED = "helpOpened";
   public static final String VIDEO_SHOWED = "videoShowed";
   public static final String VIDEO_PLAYED = "videoPlayed";
   public static final String VIDEO_PLAYED_SECONDS = "videoPlayedSeconds";

   private final Codec<Document> documentCodec;

   public UserOnboardingCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public UserOnboarding decode(final BsonReader reader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(reader, decoderContext);

      return UserOnboardingCodec.convertFromDocument(bson);
   }

   public static UserOnboarding convertFromDocument(final Document document) {
      String template = document.getString(TEMPLATE);
      Integer invitedUsers = document.getInteger(INVITED_USERS);
      boolean helpOpened = document.getBoolean(HELP_OPENED, false);
      boolean videoShowed = document.getBoolean(VIDEO_SHOWED, false);
      boolean videoPlayed = document.getBoolean(VIDEO_PLAYED, false);
      Integer videoPlayedSeconds = document.getInteger(VIDEO_PLAYED_SECONDS);

      return new UserOnboarding(template, invitedUsers, helpOpened, videoShowed, videoPlayed, videoPlayedSeconds);
   }

   @Override
   public void encode(final BsonWriter writer, final UserOnboarding value, final EncoderContext encoderContext) {
      Document bson = new Document()
            .append(TEMPLATE, value.getTemplate())
            .append(INVITED_USERS, value.getInvitedUsers())
            .append(HELP_OPENED, value.isHelpOpened())
            .append(VIDEO_SHOWED, value.isVideoShowed())
            .append(VIDEO_PLAYED, value.isVideoPlayed())
            .append(VIDEO_PLAYED_SECONDS, value.getVideoPlayedSeconds());

      documentCodec.encode(writer, bson, encoderContext);
   }

   @Override
   public Class<UserOnboarding> getEncoderClass() {
      return UserOnboarding.class;
   }
}

