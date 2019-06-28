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
package io.lumeer.core.facade.configuration;

import io.lumeer.api.model.Config;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Manipulates specified configuration properties.
 */
@ApplicationScoped
public class ConfigurationManipulator implements Serializable {

   private static final String NAMEVALUE = "namevalue";
   private static final String CONFIGS = "configs";

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
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param attributeName
    *       the name of attribute located in 'config' field
    */
   public void resetConfigurationAttribute(final String collectionName, final String nameValue, final String attributeName) {
      systemDataStorage.removeItemFromArray(collectionName, entryFilter(nameValue), CONFIGS, new DataDocument(Config.CONFIGS_CONFIG_KEY, attributeName));
   }

   /**
    * Removes all attributes and values of the 'config' field in configuration entry.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    */
   public void resetConfiguration(final String collectionName, final String nameValue) {
      setConfigurationEntry(collectionName, nameValue, Collections.emptyList());
   }

   /**
    * Sets a new configuration
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param config
    *       configuration to store
    */
   public void setConfiguration(final String collectionName, final String nameValue, Config config) {
      List<Config> configs = getConfigurations(collectionName, nameValue).stream()
                                                                         .filter(c -> !config.getKey().equals(c.getKey()))
                                                                         .collect(Collectors.toList());
      configs.add(config);
      setConfigurationEntry(collectionName, nameValue, configs);
   }

   /**
    * Sets a new configurations
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param configs
    *       configurations to store
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setConfigurations(final String collectionName, final String nameValue, final List<Config> configs, final boolean reset) {
      List<Config> newConfigs = new ArrayList<>(configs);
      if (!reset) {
         List<String> keys = configs.stream()
                                    .map(Config::getKey)
                                    .collect(Collectors.toList());

         getConfigurations(collectionName, nameValue).stream()
                                                     .filter(c -> !keys.contains(c.getKey()))
                                                     .forEach(newConfigs::add);
      }

      setConfigurationEntry(collectionName, nameValue, newConfigs);
   }

   /**
    * Returns an Object value of the given key in 'config' field for unique nameValue.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key located in 'config' field
    * @return Config object value of the given key
    */
   public Config getConfiguration(final String collectionName, final String nameValue, final String key) {
      DataDocument document = systemDataStorage.readDocumentIncludeAttrs(collectionName, entryFilter(nameValue, key),
            Collections.singletonList(dataStorageDialect.concatFields(CONFIGS, "$")));
      // we got only one subdocument otherwise there was null
      return document != null ? new Config(document.getArrayList(CONFIGS, DataDocument.class).get(0)) : null;
   }

   /**
    * Returns an DataDocument containing all configurations for unique nameValue.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @return List storing all configurations
    */
   public List<Config> getConfigurations(final String collectionName, final String nameValue) {
      DataDocument document = systemDataStorage.readDocument(collectionName, entryFilter(nameValue));
      List<DataDocument> configs = document != null ? document.getArrayList(CONFIGS, DataDocument.class) : Collections.emptyList();

      return configs.stream().map(Config::new).collect(Collectors.toList());
   }

   private DataFilter entryFilter(final String nameValue) {
      return dataStorageDialect.fieldValueFilter(NAMEVALUE, nameValue);
   }

   private DataFilter entryFilter(final String nameValue, final String key) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(NAMEVALUE, nameValue);
      filter.put(dataStorageDialect.concatFields(CONFIGS, Config.CONFIGS_CONFIG_KEY), key);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private void setConfigurationEntry(final String collectionName, final String nameValue, List<Config> configs) {
      List<DataDocument> configsDocuments = configs.stream()
                                                   .map(Config::toDataDocument)
                                                   .collect(Collectors.toList());

      DataDocument configDocument = new DataDocument()
            .append(NAMEVALUE, nameValue)
            .append(CONFIGS, configsDocuments);
      systemDataStorage.updateDocument(collectionName, configDocument, entryFilter(nameValue));
   }

}
