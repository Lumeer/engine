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

package io.lumeer.storage.mongodb.codecs.providers;

import io.lumeer.api.model.InitialUserData;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserOnboarding;
import io.lumeer.storage.mongodb.codecs.InitialUserDataCodec;
import io.lumeer.storage.mongodb.codecs.UserCodec;
import io.lumeer.storage.mongodb.codecs.UserOnboardingCodec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class UserCodecProvider implements CodecProvider {

   @Override
   public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
      if (clazz == User.class) {
         return (Codec<T>) new UserCodec(registry);
      }
      if (clazz == UserOnboarding.class) {
         return (Codec<T>) new UserOnboardingCodec(registry);
      }
      if (clazz == InitialUserData.class) {
         return (Codec<T>) new InitialUserDataCodec(registry);
      }

      return null;
   }

}
