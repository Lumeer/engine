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
package io.lumeer.engine.controller.configuration;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Config;
import io.lumeer.engine.util.Resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Manipulates specified configuration properties.
 */
@ApplicationScoped
public class ConfigurationManipulator implements Serializable {

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   /**
    * Removes the whole attribute located in 'config' field in configuration entry.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param identificator
    *       the unique name value of stored configuration entry
    * @param attributeName
    *       the name of attribute located in 'config' field
    */
   public void resetConfigurationAttribute(final String collectionName, final String identificator, final String attributeName) {
      systemDataStorage.removeItemFromArray(collectionName, entryFilter(identificator),
            LumeerConst.Configuration.CONFIGS, new DataDocument(LumeerConst.Configuration.CONFIGS_CONFIG_KEY, attributeName));
   }

   /**
    * Removes all attributes and values of the 'config' field in configuration entry.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param identificator
    *       the unique name value of stored configuration entry
    */
   public void resetConfiguration(final String collectionName, final String identificator) {
      setConfigurationEntry(collectionName, identificator, Collections.emptyList());
   }

   /**
    * Sets a new configuration
    *
    * @param collectionName
    *       the name of collection in system database
    * @param identificator
    *       the unique name value of stored configuration entry
    * @param config
    *       configuration to store
    */
   public void setConfiguration(final String collectionName, final String identificator, Config config) {
      List<Config> configs = getConfigurations(collectionName, identificator).stream()
                                                                             .filter(c -> !config.getKey().equals(c.getKey()))
                                                                             .collect(Collectors.toList());
      configs.add(config);
      setConfigurationEntry(collectionName,identificator, configs);
   }

   /**
    * Sets a new configurations
    *
    * @param collectionName
    *       the name of collection in system database
    * @param identificator
    *       the unique name value of stored configuration entry
    * @param configs
    *       configurations to store
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setConfigurations(final String collectionName, final String identificator, final List<Config> configs, final boolean reset) {
      List<Config> newConfigs = new ArrayList<>(configs);
      if(!reset) {
         List<String> keys = configs.stream()
                                    .map(Config::getKey)
                                    .collect(Collectors.toList());

         getConfigurations(collectionName, identificator).stream()
                                                         .filter(c -> !keys.contains(c.getKey()))
                                                         .forEach(newConfigs::add);
      }

      setConfigurationEntry(collectionName, identificator, newConfigs);
   }

   /**
    * Returns an Object value of the given key in 'config' field for unique nameValue.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param identificator
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key located in 'config' field
    * @return Config object value of the given key
    */
   public Config getConfiguration(final String collectionName, final String identificator, final String key) {
      DataDocument document = systemDataStorage.readDocumentIncludeAttrs(collectionName, entryFilter(identificator, key),
            Collections.singletonList(dataStorageDialect.concatFields(LumeerConst.Configuration.CONFIGS, "$")));
      // we got only one subdocument otherwise there was null
      return document != null ? new Config(document.getArrayList(LumeerConst.Configuration.CONFIGS, DataDocument.class).get(0)) : null;
   }

   /**
    * Returns an DataDocument containing all configurations for unique nameValue.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param identificator
    *       the unique name value of stored configuration entry
    * @return List storing all configurations
    */
   public List<Config> getConfigurations(final String collectionName, final String identificator) {
      DataDocument document = systemDataStorage.readDocument(collectionName, entryFilter(identificator));
      List<DataDocument> configs = document != null ? document.getArrayList(LumeerConst.Configuration.CONFIGS, DataDocument.class) : Collections.emptyList();

      return configs.stream().map(Config::new).collect(Collectors.toList());
   }

   public boolean hasConfigurationAttributeFlag(final String collectionName, final String identificator, final String attributeName, final String flagName) {
      Config config = getConfiguration(collectionName, identificator, attributeName);
      if (config == null) {
         return false;
      }
      // in case of multiple possible flag there will be switch
      return flagName.equals(LumeerConst.Configuration.CONFIGS_CONFIG_FLAG_RESTRICTED) && config.isRestricted();
   }

   private DataFilter entryFilter(final String identificator) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Configuration.IDENTIFICATOR, identificator);
   }

   private DataFilter entryFilter(final String identificator, final String key) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Configuration.IDENTIFICATOR, identificator);
      filter.put(dataStorageDialect.concatFields(LumeerConst.Configuration.CONFIGS, LumeerConst.Configuration.CONFIGS_CONFIG_KEY), key);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private void setConfigurationEntry(final String collectionName, final String identificator, List<Config> configs) {
      List<DataDocument> configsDocuments = configs.stream()
                                                   .map(Config::toDataDocument)
                                                   .collect(Collectors.toList());

      DataDocument configDocument = new DataDocument()
            .append(LumeerConst.Configuration.IDENTIFICATOR, identificator)
            .append(LumeerConst.Configuration.CONFIGS, configsDocuments);
      systemDataStorage.updateDocument(collectionName, configDocument, entryFilter(identificator));
   }

}
