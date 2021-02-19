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

import io.lumeer.core.facade.ConfigurationFacade;

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

   private static final Logger log = Logger.getLogger(DefaultConfigurationProducer.class.getName());

   private static Map<String, String> defaultConfiguration = null;

   private static final String ENVIRONMENT = "environment";

   public static final String GOPAY_API = "gopay_api";
   public static final String GOPAY_ID = "gopay_id";
   public static final String GOPAY_CLIENT_ID = "gopay_client_id";
   public static final String GOPAY_CLIENT_CREDENTIALS = "gopay_client_credentials";

   public static final String MAILCHIMP_APIKEY = "mailchimp_apikey";
   public static final String MAILCHIMP_SUBDOMAIN = "mailchimp_subdomain";
   public static final String MAILCHIMP_LIST_CS = "mailchimp_list_cs";
   public static final String MAILCHIMP_LIST_EN = "mailchimp_list_en";

   public static final String MAILERLITE_APIKEY = "mailerlite_apikey";
   public static final String MAILERLITE_GROUP_CS = "mailerlite_group_cs";
   public static final String MAILERLITE_GROUP_EN = "mailerlite_group_en";
   public static final String MAILERLITE_SEQUENCE_CS = "mailerlite_sequence_cs";
   public static final String MAILERLITE_SEQUENCE_EN = "mailerlite_sequence_en";

   public static final String FRESHDESK_DOMAIN = "freshdesk_domain";
   public static final String FRESHDESK_APIKEY = "freshdesk_apikey";

   public static final String PUSHER_APP_ID = "pusher_app_id";
   public static final String PUSHER_KEY = "pusher_key";
   public static final String PUSHER_SECRET = "pusher_secret";
   public static final String PUSHER_CLUSTER = "pusher_cluster";

   public static final String S3_KEY = "s3_key";
   public static final String S3_SECRET = "s3_secret";
   public static final String S3_BUCKET = "s3_bucket";
   public static final String S3_REGION = "s3_region";
   public static final String S3_ENDPOINT = "s3_endpoint";

   public static final String SMTP_USER = "smtp_user";
   public static final String SMTP_PASSWORD = "smtp_password";
   public static final String SMTP_SERVER = "smtp_server";
   public static final String SMTP_PORT = "smtp_port";
   public static final String SMTP_FROM = "smtp_from";

   public static final String SENTRY_DSN = "sentry_dsn";

   public static final String LOCALE = "locale";

   public static final String TEMPLATE_ORG_EN = "template_org_en";
   public static final String TEMPLATE_ORG_CS = "template_org_cs";

   public static final String SAMPLE_DATA_ORG_EN = "sample_data_org_en";
   public static final String SAMPLE_DATA_ORG_CS = "sample_data_org_cs";

   public DefaultConfigurationProducer() {
      synchronized (this) {
         if (defaultConfiguration == null) {
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
      }
   }

   public String get(final String key) {
      return defaultConfiguration.get(key);
   }

   public DeployEnvironment getEnvironment() {
      final String value = get(ENVIRONMENT);

      if (value != null) {
         final DeployEnvironment env = DeployEnvironment.valueOf(value.toUpperCase());

         return env != null ? env : DeployEnvironment.DEVEL;
      }

      return DeployEnvironment.DEVEL;
   }

   public enum DeployEnvironment {
      DEVEL, STAGING, PRODUCTION;
   }
}
