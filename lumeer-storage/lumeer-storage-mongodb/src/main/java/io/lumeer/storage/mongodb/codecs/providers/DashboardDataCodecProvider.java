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

import io.lumeer.api.model.DashboardData;
import io.lumeer.storage.mongodb.codecs.DashboardDataCodec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class DashboardDataCodecProvider implements CodecProvider {

   @Override
   public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
      if (clazz == DashboardData.class) {
         return (Codec<T>) new DashboardDataCodec(registry);
      }

      return null;
   }

}
