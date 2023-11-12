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

import io.lumeer.core.facade.configuration.ConfigurationManipulator;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.StorageConnection;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SystemConfigurationFacade {

   private static final String SYSTEM_DB_HOSTS_PROPERTY = "sys_db_hosts";
   private static final String SYSTEM_DB_NAME_PROPERTY = "sys_db_name";
   private static final String SYSTEM_DB_USER_PROPERTY = "sys_db_user";
   private static final String SYSTEM_DB_PASSWORD_PROPERTY = "sys_db_passwd";
   private static final String SYSTEM_DB_USE_SSL = "sys_db_ssl";

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured system data storage.
    */
   public List<StorageConnection> getSystemDataStorage() {
      final String hosts = defaultConfigurationProducer.get(SYSTEM_DB_HOSTS_PROPERTY);
      final String db = defaultConfigurationProducer.get(SYSTEM_DB_USER_PROPERTY);
      final String pwd = defaultConfigurationProducer.get(SYSTEM_DB_PASSWORD_PROPERTY);

      return ConfigurationFacade.getStorageConnections(hosts, db, pwd);
   }

   public String getSystemDataStorageDatabase() {
      return defaultConfigurationProducer.get(SYSTEM_DB_NAME_PROPERTY);
   }

   public Boolean getSystemDataStorageUseSsl() {
      return Boolean.valueOf(defaultConfigurationProducer.get(SYSTEM_DB_USE_SSL));
   }
}
