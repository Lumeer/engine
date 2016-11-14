/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.controller;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class ConfigurationFacade {

   @Inject
   private UserFacade userFacade;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   private static final Map<String, String> DEFAULT_VALUES = new HashMap<>();

   static {
      DEFAULT_VALUES.put("SOME_KEY", "SOME_DEFAULT_VALUE");
   }

   public Optional<String> getConfigurationString(final String key) {
      final Object value = readValueFromDb(key);

      if (value == null && DEFAULT_VALUES.containsKey(key)) {
         return Optional.of(DEFAULT_VALUES.get(key));
      } else if (value != null) {
         return Optional.of(value.toString());
      }

      return Optional.empty();
   }

   public Optional<Integer> getConfigurationInteger(final String key) {
      final Optional<String> value = getConfigurationString(key);

      if (value.isPresent()) {
         return Optional.of(Integer.parseInt(value.get()));
      } else {
         return Optional.empty();
      }
   }

   public Optional<DataDocument> getConfigurationDocument(final String key) {
      final Object value = readValueFromDb(key);

      if (value != null && value instanceof DataDocument) {
         return Optional.of((DataDocument) value);
      }

      return Optional.empty();
   }

   private Object readValueFromDb(final String key) {
      final String user = userFacade.getUserEmail();

      // TODO implement using systemDataStorage

      return null;
   }
}
