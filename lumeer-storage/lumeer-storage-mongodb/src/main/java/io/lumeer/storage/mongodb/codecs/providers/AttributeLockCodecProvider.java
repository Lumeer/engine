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

import io.lumeer.api.model.AttributeFilterEquation;
import io.lumeer.api.model.AttributeLock;
import io.lumeer.api.model.AttributeLockExceptionGroup;
import io.lumeer.storage.mongodb.codecs.AttributeFilterEquationCodec;
import io.lumeer.storage.mongodb.codecs.AttributeLockCodec;
import io.lumeer.storage.mongodb.codecs.AttributeLockExceptionGroupCodec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class AttributeLockCodecProvider implements CodecProvider {

   @Override
   public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
      if (clazz == AttributeLock.class) {
         return (Codec<T>) new AttributeLockCodec(registry);
      } else if (clazz == AttributeLockExceptionGroup.class) {
         return (Codec<T>) new AttributeLockExceptionGroupCodec(registry);
      } else if (clazz == AttributeFilterEquation.class) {
         return (Codec<T>) new AttributeFilterEquationCodec(registry);
      }

      return null;
   }

}
