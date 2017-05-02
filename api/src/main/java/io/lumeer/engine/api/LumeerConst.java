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

   public static final String DB_HOST_PROPERTY = "db_host";
   public static final String DB_PORT_PROPERTY = "db_port";
   public static final String DB_NAME_PROPERTY = "db_name";
   public static final String DB_USER_PROPERTY = "db_user";
   public static final String DB_PASSWORD_PROPERTY = "db_passwd";
   public static final String DB_USE_SSL = "db_ssl";

   public static final String SYSTEM_DB_HOST_PROPERTY = "sys_db_host";
   public static final String SYSTEM_DB_PORT_PROPERTY = "sys_db_port";
   public static final String SYSTEM_DB_NAME_PROPERTY = "sys_db_name";
   public static final String SYSTEM_DB_USER_PROPERTY = "sys_db_user";
   public static final String SYSTEM_DB_PASSWORD_PROPERTY = "sys_db_passwd";
   public static final String SYSTEM_DB_USE_SSL = "sys_db_ssl";

   public static final String USER_LOCALE_PROPERTY = "locale";

   public static final String DEFAULT_LIMIT_PROPERTY = "result_limit";

   public static final int SORT_ASCENDING_ORDER = 1;
   public static final int SORT_DESCENDING_ORDER = -1;

   public static final String NUMBER_OF_RECENT_DOCS_PROPERTY = "number_recently_used_documents";

   private LumeerConst() {
      // we do not want any instances to be created
      throw new UnsupportedOperationException(String.format("Creation of %s is forbidden.", this.getClass().getCanonicalName()));
   }

   /**
    * Index type
    *
    * @see <a href="https://docs.mongodb.com/manual/core/index-single/">https://docs.mongodb.com/manual/core/index-single/</a>
    */
   public static class Index {
      public static final int ASCENDING = 1;
      public static final int DESCENDING = -1;
   }

   public static class Linking {
      public static final String PREFIX = "_linking";

      /**
       * Which direction of link to work with.
       */
      public enum LinkDirection {
         BOTH, FROM, TO;

         public static LinkDirection fromString(final String s) {
            return LinkDirection.valueOf(s);
         }
      }

      public class Type {
         public static final String NAME = "_system-linking";
         public static final String ATTR_FROM_COLLECTION = "from_collection";
         public static final String ATTR_TO_COLLECTION = "to_collection";
         public static final String ATTR_PROJECT = "project_id";
         public static final String ATTR_ROLE = "role";
      }

      public class Instance {
         public static final String ATTR_TYPE_ID = "type_id";
         public static final String ATTR_FROM_ID = "from_id";
         public static final String ATTR_TO_ID = "to_id";
         public static final String ATTR_ATTRIBUTES = "attributes";
      }
   }

   public static class Document {
      public static final String ID = "_id";
      public static final String METADATA_PREFIX = "_meta-";
      public static final String CREATE_DATE_KEY = METADATA_PREFIX + "create-date";
      public static final String UPDATE_DATE_KEY = METADATA_PREFIX + "update-date";
      public static final String CREATE_BY_USER_KEY = METADATA_PREFIX + "create-user";
      public static final String UPDATED_BY_USER_KEY = METADATA_PREFIX + "update-user";
      public static final String USER_RIGHTS = METADATA_PREFIX + "rights";
      public static final String COLLECTION_NAME = METADATA_PREFIX + "collection"; // used in cases where we need to note the source collection in the document
      public static final String METADATA_VERSION_KEY = METADATA_PREFIX + "version";
      public static final List<String> METADATA_KEYS = Arrays.asList(METADATA_VERSION_KEY, CREATE_DATE_KEY, UPDATE_DATE_KEY, CREATE_BY_USER_KEY, UPDATED_BY_USER_KEY, USER_RIGHTS);
   }

   public static class View {
      public static final String METADATA_COLLECTION_PREFIX = "meta.view_";
      public static final String SEQUENCE_NAME = "view-sequence";

      public static final String NAME_KEY = "name";
      public static final String DESCRIPTION_KEY = "description";
      public static final String ID_KEY = "view-id";
      public static final String TYPE_KEY = "type";
      public static final String TYPE_DEFAULT_VALUE = "default";
      public static final String CONFIGURATION_KEY = "configuration";

      public static final String CREATE_DATE_KEY = Document.CREATE_DATE_KEY;
      public static final String CREATE_USER_KEY = Document.CREATE_BY_USER_KEY;
      public static final String UPDATE_DATE_KEY = Document.UPDATE_DATE_KEY;
      public static final String UPDATE_USER_KEY = Document.UPDATED_BY_USER_KEY;
   }

   public static class Collection {
      public static final String METADATA_COLLECTION_PREFIX = "meta.collection_";
      public static final String NAME_PREFIX = "collection.";

      public static final String REAL_NAME_KEY = "name";
      public static final String INTERNAL_NAME_KEY = "internal-name";
      public static final String PROJECT_ID_KEY = "project-id";

      public static final String ATTRIBUTES_KEY = "attributes";
      public static final String ATTRIBUTE_NAME_KEY = "attribute-name";
      public static final String ATTRIBUTE_CONSTRAINTS_KEY = "attribute-constraints";
      public static final String ATTRIBUTE_COUNT_KEY = "attribute-count";

      public static final String LAST_TIME_USED_KEY = "last-time-used";
      public static final String RECENTLY_USED_DOCUMENTS_KEY = "recently-used-documents";
      public static final String ATTRIBUTE_CHILDREN_KEY = "child-attributes";
      public static final String CUSTOM_META_KEY = "custom";

      public static final String CREATE_USER_KEY = Document.CREATE_BY_USER_KEY;
      public static final String CREATE_DATE_KEY = Document.CREATE_DATE_KEY;

      public static final String COLLECTION_SHADOW_PREFFIX = "_shadow";
      public static final String COLLECTION_TRASH_PREFFIX = "_trash";

      public static final int DEFAULT_NUMBER_OF_RECENT_DOCUMENTS = 5;
   }

   public static class Security {
      public static final String ORGANIZATION_ROLES_COLLECTION_NAME = "_system-organization-roles";
      public static final String ROLES_COLLECTION_NAME = "_roles";

      public static final String ORGANIZATION_ID_KEY = "organization-id";

      public static final String PROJECT_ID_KEY = "project-id";
      public static final String SOURCE_ID_KEY = "source-id";
      public static final String SOURCE_TYPE_KEY = "source-type";
      public static final String SOURCE_TYPE_PROJECT = "project";
      public static final String SOURCE_TYPE_COLLECTION = "collection";
      public static final String SOURCE_TYPE_VIEW = "view";

      public static final String ROLES_KEY = "roles";
      public static final String MANAGE_KEY = "manage";
      public static final String WRITE_KEY = "write";
      public static final String READ_KEY = "read";
      public static final String SHARE_KEY = "share";
      public static final String CLONE_KEY = "clone";

      public static final String USERS_KEY = "users";
      public static final String GROUP_KEY = "groups";

      /****** OLD ******/
      public static final String RULE = "rule";
      public static final String USER_ID = "user_email";

      public static final int READ = 4;
      public static final int WRITE = 2;
      public static final int EXECUTE = 1;
   }

   public static class Project {

      public static final String COLLECTION_NAME = "_system-project";
      public static final String ATTR_PROJECT_ID = "project-id";
      public static final String ATTR_ORGANIZATION_ID = "organization-id";
      public static final String ATTR_PROJECT_NAME = "project-name";
      public static final String ATTR_USERS = "users";
      public static final String ATTR_USERS_USERNAME = "user";
      public static final String ATTR_USERS_USER_ROLES = "user-roles";
      public static final String METADATA_PREFIX = "_meta-";
      public static final String ATTR_META_ICON = METADATA_PREFIX + "icon";
      public static final String ATTR_META_COLOR = METADATA_PREFIX + "color";
      public static final String ATTR_META_DEFAULT_ROLES = METADATA_PREFIX + "default-roles";
      
   }

   public static class Organization {

      public static final String COLLECTION_NAME = "_system-organization";
      public static final String ATTR_ORG_ID = "organization-id";
      public static final String ATTR_ORG_NAME = "organization-name";
      public static final String ATTR_ORG_DATA = "organization-info-data";

      public static final String METADATA_PREFIX = "_meta-";
      public static final String ATTR_META_ICON = METADATA_PREFIX + "icon";
      public static final String ATTR_META_COLOR = METADATA_PREFIX + "color";
      public static final String ATTR_META_DEFAULT_ROLES = METADATA_PREFIX + "default-roles";

      public static final String ATTR_USERS = "users";
      public static final String ATTR_USERS_USERNAME = "user";
      public static final String ATTR_USERS_USER_ROLES = "user-roles";
   }
}
