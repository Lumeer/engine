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
package io.lumeer.core.facade;

import static io.lumeer.core.facade.ConfigurationFacade.*;
import static io.lumeer.core.facade.ConfigurationFacade.ORGANIZATION_CONFIG_COLLECTION;

import io.lumeer.api.model.Config;
import io.lumeer.core.facade.configuration.ConfigurationManipulator;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.StorageConnection;

import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SystemDatabaseConfigurationFacade {

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured data storage.
    */
   public List<StorageConnection> getDataStorage(final String organizationId) {
      final String hosts = getSystemConfigurationString(DB_HOSTS_PROPERTY, organizationId).orElse("localhost:27017");
      final String db = getSystemConfigurationString(DB_USER_PROPERTY, organizationId).orElse("pepa");
      final String pwd = getSystemConfigurationString(DB_PASSWORD_PROPERTY, organizationId).orElse("");

      return ConfigurationFacade.getStorageConnections(hosts, db, pwd);
   }

   /**
    * Gets configuration value either from organization and when there is none present, it backs up to property files.
    *
    * @param key
    *       Property to obtain.
    * @return Configuration value.
    */
   private Optional<String> getSystemConfigurationString(final String key, final String organizationId) {
      final Config organizationConfig = getOrganizationConfiguration(key, organizationId);

      if (organizationConfig != null) {
         return ConfigurationFacade.createOptionalString(organizationConfig);
      } else {
         return ConfigurationFacade.createOptionalString(new Config(key, defaultConfigurationProducer.get(key)));
      }
   }

   public String getDataStorageDatabase(final String organizationId) {
      return getSystemConfigurationString(DB_NAME_PROPERTY, organizationId).orElse("lumeer");
   }

   public Boolean getDataStorageUseSsl(final String organizationId) {
      return Boolean.valueOf(getSystemConfigurationString(DB_USE_SSL, organizationId).orElse("false"));
   }

   private Config getOrganizationConfiguration(final String key, final String organizationId) {
      return configurationManipulator.getConfiguration(ORGANIZATION_CONFIG_COLLECTION, organizationId, key);
   }
}
