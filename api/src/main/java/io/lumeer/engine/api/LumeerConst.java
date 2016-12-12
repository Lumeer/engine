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

import java.util.Arrays;
import java.util.List;

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

   public static final String USER_LOCALE_PROPERTY = "locale";

   public static final String DEFAULT_LIMIT_PROPERTY = "result_limit";

   private LumeerConst() {
      // we do not want any instances to be created
      throw new UnsupportedOperationException(String.format("Creation of %s is forbidden.", this.getClass().getCanonicalName()));
   }

   public static class Linking {
      public static final String PREFIX = "_linking";

      public class MainTable {
         public static final String NAME = "_system-linking";
         public static final String ATTR_COL1 = "collection1";
         public static final String ATTR_COL2 = "collection2";
         public static final String ATTR_COL_NAME = "collection_name";
         public static final String ATTR_COUNT = "count";
      }

      public class LinkingTable {
         public static final String ATTR_DOC1 = "id_doc1";
         public static final String ATTR_DOC2 = "id_doc2";
      }
   }

   public static class Document {
      public static final String ID = "_id";
      public static final String METADATA_PREFIX = "_meta-";
      public static final String CREATE_DATE_KEY = METADATA_PREFIX + "create-date";
      public static final String UPDATE_DATE_KEY = METADATA_PREFIX + "update-date";
      public static final String CREATE_BY_USER_KEY = METADATA_PREFIX + "create-user";
      public static final String UPDATED_BY_USER_KEY = METADATA_PREFIX + "update-user";
      public static final List<String> METADATA_KEYS = Arrays.asList(METADATA_VERSION_KEY, CREATE_DATE_KEY, UPDATE_DATE_KEY, CREATE_BY_USER_KEY, UPDATED_BY_USER_KEY);
      public static final String USER_RIGHTS = METADATA_PREFIX + "rights";
   }

   public static class View {
      public static final String VIEW_METADATA_COLLECTION_NAME = "viewmetadatacollection";
      public static final String VIEW_SEQUENCE_NAME = "view-sequence";

      public static final String VIEW_INTERNAL_NAME_KEY = "internal-name";
      public static final String VIEW_REAL_NAME_KEY = "name";
      public static final String VIEW_SEQUENCE_NUMBER_KEY = "sequence-number";

      public static final String VIEW_USER_RIGHTS_KEY = "rights";
      //public static final String VIEW_GROUP_RIGHTS_KEY = "group-rights";
      public static final String VIEW_CREATE_DATE_KEY = Document.CREATE_DATE_KEY;
      public static final String VIEW_CREATE_USER_KEY = Document.CREATE_BY_USER_KEY;
      public static final String VIEW_UPDATE_DATE_KEY = Document.UPDATE_DATE_KEY;
      public static final String VIEW_UPDATE_USER_KEY = Document.UPDATED_BY_USER_KEY;

      public static final List<String> VIEW_IMMUTABLE_KEYS = Arrays.asList(
            VIEW_INTERNAL_NAME_KEY,
            VIEW_SEQUENCE_NUMBER_KEY,
            VIEW_CREATE_DATE_KEY,
            VIEW_CREATE_USER_KEY,
            VIEW_USER_RIGHTS_KEY,
            Document.METADATA_PREFIX + VIEW_USER_RIGHTS_KEY // this key is used by security facade to store access rights
      );
   }

   public static class Collection {
      public static final String META_TYPE_KEY = "meta-type";
      public static final String COLLECTION_NAME_PREFIX = "collection.";
      public static final String COLLECTION_METADATA_PREFIX = "meta.";

      public static final String COLLECTION_ATTRIBUTES_META_TYPE_VALUE = "attributes";

      public static final String COLLECTION_ATTRIBUTE_NAME_KEY = "name";
      public static final String COLLECTION_ATTRIBUTE_TYPE_KEY = "type";

      public static final String COLLECTION_ATTRIBUTE_TYPE_NUMBER = "number";
      public static final String COLLECTION_ATTRIBUTE_TYPE_DATE = "date";
      public static final String COLLECTION_ATTRIBUTE_TYPE_STRING = ""; // empty is default and is considered String
      public static final String COLLECTION_ATTRIBUTE_TYPE_LIST = "list";
      // public static final String COLLECTION_ATTRIBUTE_TYPE_NESTED = "nested"; // TODO
      public static final List<String> COLLECTION_ATTRIBUTE_TYPE_VALUES =
            Arrays.asList(COLLECTION_ATTRIBUTE_TYPE_NUMBER,
                  COLLECTION_ATTRIBUTE_TYPE_DATE,
                  COLLECTION_ATTRIBUTE_TYPE_STRING,
                  COLLECTION_ATTRIBUTE_TYPE_LIST);

      public static final String COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY = "constraints";

      public static final String COLLECTION_ATTRIBUTE_COUNT_KEY = "count";

      public static final String COLLECTION_REAL_NAME_META_TYPE_VALUE = "name";
      public static final String COLLECTION_REAL_NAME_KEY = "name";

      public static final String COLLECTION_LOCK_META_TYPE_VALUE = "lock";
      public static final String COLLECTION_LOCK_UPDATED_KEY = "updated";
   }
}
