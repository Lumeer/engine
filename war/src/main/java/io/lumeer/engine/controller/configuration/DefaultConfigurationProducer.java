/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.controller.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultConfigurationProducer implements Serializable {

   private static final long serialVersionUID = -9139613375175238783L;

   private static final String DEFAULT_PROPERTY_FILE = "defaults-dev.properties";

   private Logger log = Logger.getLogger(DefaultConfigurationProducer.class.getName());

   private Map<String, String> defaultConfiguration = null;

   public DefaultConfigurationProducer() {
      defaultConfiguration = new HashMap<>();

      String envDefaults = System.getProperty("lumeer.defaults", System.getenv("LUMEER_DEFAULTS"));
      if (envDefaults == null) {
         envDefaults = DEFAULT_PROPERTY_FILE;
      }

      log.info("Loading default properties from: " + envDefaults);

      final Properties properties = new Properties();
      try {
         final InputStream input = DefaultConfigurationProducer.class.getResourceAsStream("/" + envDefaults);
         if (input != null) {
            properties.load(new InputStreamReader(input));
            properties.list(System.out);
            properties.forEach((key, value) -> {
               defaultConfiguration.put(key.toString(), value.toString());
            });
         } else {
            log.log(Level.WARNING, String.format("Default property file %s not found.", envDefaults));
         }
      } catch (IOException e) {
         log.log(Level.SEVERE, "Unable to load default property values: ", e);
      }
   }

   public String get(final String key) {
      return defaultConfiguration.get(key);
   }
}
