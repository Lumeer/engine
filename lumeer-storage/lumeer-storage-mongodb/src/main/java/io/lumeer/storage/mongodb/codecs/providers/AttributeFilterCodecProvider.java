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

import io.lumeer.api.model.AttributeFilter;
import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.LinkAttributeFilter;
import io.lumeer.storage.mongodb.codecs.AttributeFilterCodec;
import io.lumeer.storage.mongodb.codecs.CollectionAttributeFilterCodec;
import io.lumeer.storage.mongodb.codecs.LinkAttributeFilterCodec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class AttributeFilterCodecProvider implements CodecProvider {

   @Override
   public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
      if (clazz == CollectionAttributeFilter.class) {
         return (Codec<T>) new CollectionAttributeFilterCodec(registry);
      } else if (clazz == LinkAttributeFilter.class) {
         return (Codec<T>) new LinkAttributeFilterCodec(registry);
      } else if (clazz == AttributeFilter.class) {
         return (Codec<T>) new AttributeFilterCodec(registry);
      }

      return null;
   }

}
