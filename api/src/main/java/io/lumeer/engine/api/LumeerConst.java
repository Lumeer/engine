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
package io.lumeer.engine.api;

/**
 * Lumeer constants.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public final class LumeerConst {

   public static final String LUMEER_VERSION = "1.0";
   public static final String METADATA_VERSION_KEY = "_metadata-version";

   public static final String DB_HOST_PROPERTY = "db_host";
   public static final String DB_PORT_PROPERTY = "db_port";
   public static final String DB_NAME_PROPERTY = "db_name";
   public static final String DB_USER_PROPERTY = "db_user";
   public static final String DB_PASSWORD_PROPERTY = "db_passwd";

   public static final String SYSTEM_DB_HOST_PROPERTY = "sys_db_host";
   public static final String SYSTEM_DB_PORT_PROPERTY = "sys_db_port";
   public static final String SYSTEM_DB_NAME_PROPERTY = "sys_db_name";
   public static final String SYSTEM_DB_USER_PROPERTY = "sys_db_user";
   public static final String SYSTEM_DB_PASSWORD_PROPERTY = "sys_db_passwd";

   private LumeerConst() {
      // we do not want any instances to be created
      throw new UnsupportedOperationException(String.format("Creation of %s is forbidden.", this.getClass().getCanonicalName()));
   }

   public static class LINKING {
      public static final String PREFIX = "_linking";

      public class MAIN_TABLE {
         public static final String NAME = "_system_linking";
         public static final String ATTR_COL1 = "collection1";
         public static final String ATTR_COL2 = "collection2";
         public static final String ATTR_COL_NAME = "collection_name";
         public static final String ATTR_COUNT = "count";
      }

      public class LINKING_TABLE {
         public static final String ATTR_DOC1 = "id_doc1";
         public static final String ATTR_DOC2 = "id_doc2";
      }
   }

   public static class DOCUMENT {
      public static final String METADATA_PREFIX = "meta-";
      public static final String CREATE_DATE_KEY = METADATA_PREFIX + "create-date";
      public static final String UPDATE_DATE_KEY = METADATA_PREFIX + "update-date";
      public static final String CREATE_BY_USER_KEY = METADATA_PREFIX + "create-user";
      public static final String UPDATED_BY_USER_KEY = METADATA_PREFIX + "update-user";

   }
}
