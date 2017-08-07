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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
         public static final String ATTR_FROM_COLLECTION_ID = "from_collection";
         public static final String ATTR_TO_COLLECTION_ID = "to_collection";
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

      public static final String REAL_NAME = "name";
      public static final String CODE = "code";

      public static final String ATTRIBUTES = "attributes";
      public static final String ATTRIBUTE_NAME = "name";
      public static final String ATTRIBUTE_FULL_NAME = "full-name";
      public static final String ATTRIBUTE_CONSTRAINTS = "constraints";
      public static final String ATTRIBUTE_COUNT = "count";

      public static final String ICON = "icon";
      public static final String COLOR = "color";
      public static final String DOCUMENT_COUNT = "document-count";
      public static final String LAST_TIME_USED = "last";
      public static final String RECENTLY_USED_DOCUMENTS = "recently";
      public static final String ATTRIBUTE_CHILDREN = "children";
      public static final String CUSTOM_META = "custom";

      public static final String CREATE_USER = Document.CREATE_BY_USER_KEY;
      public static final String CREATE_DATE = Document.CREATE_DATE_KEY;
      public static final String UPDATE_USER = Document.UPDATED_BY_USER_KEY;
      public static final String UPDATE_DATE = Document.UPDATED_BY_USER_KEY;

      public static final String COLLECTION_SHADOW_PREFFIX = "_shadow";
      public static final String COLLECTION_TRASH_PREFFIX = "_trash";

      public static final int DEFAULT_NUMBER_OF_RECENT_DOCUMENTS = 5;
   }

   public static class Security {
      public static final String ORGANIZATION_ROLES_COLLECTION_NAME = "_system-org-roles";
      public static final String ROLES_COLLECTION_NAME = "_roles";

      public static final String ORGANIZATION_ID_KEY = "org-id";

      public static final String PROJECT_ID_KEY = "pro-id";
      public static final String TYPE_ID_KEY = "source-id";

      public static final String TYPE_KEY = "type";
      public static final String TYPE_PROJECT = "project";
      public static final String TYPE_COLLECTION = "collection";
      public static final String TYPE_VIEW = "view";

      public static final String PERMISSIONS_KEY = "permissions";
      public static final String ROLE_MANAGE = "manage";
      public static final String ROLE_WRITE = "write";
      public static final String ROLE_READ = "read";
      public static final String ROLE_SHARE = "share";
      public static final String ROLE_CLONE = "clone";

      public static final String USERS_KEY = "users";
      public static final String GROUP_KEY = "groups";
      public static final String USERGROUP_NAME_KEY = "name";
      public static final String USERGROUP_ROLES_KEY = "roles";

      public static final String ORGANIZATION_RESOURCE = "organizations";
      public static final String PROJECT_RESOURCE = "projects";
      public static final String COLLECTION_RESOURCE = "collections";
      public static final String VIEW_RESOURCE = "views";

      private static final String[] ORGANIZATION_ROLES = new String[]
            { ROLE_MANAGE, ROLE_WRITE, ROLE_READ };
      private static final String[] PROJECT_ROLES = new String[]
            { ROLE_MANAGE, ROLE_WRITE, ROLE_READ };
      private static final String[] COLLECTION_ROLES = new String[]
            { ROLE_MANAGE, ROLE_READ, ROLE_SHARE, ROLE_WRITE };
      private static final String[] VIEW_ROLES = new String[]
            { ROLE_MANAGE, ROLE_READ, ROLE_CLONE };

      public static final Map<String, Set<String>> RESOURCE_ROLES =
            Collections.unmodifiableMap(new HashMap<String, Set<String>>() {
               {
                  put(ORGANIZATION_RESOURCE, new HashSet<>(Arrays.asList(ORGANIZATION_ROLES)));
                  put(PROJECT_RESOURCE, new HashSet<>(Arrays.asList(PROJECT_ROLES)));
                  put(COLLECTION_RESOURCE, new HashSet<>(Arrays.asList(COLLECTION_ROLES)));
                  put(VIEW_RESOURCE, new HashSet<>(Arrays.asList(VIEW_ROLES)));
               }
            });

   }

   public static class Project {

      public static final String COLLECTION_NAME = "_system-project";
      public static final String ATTR_PROJECT_ID = Document.ID;
      public static final String ATTR_PROJECT_CODE = "project-code";
      public static final String ATTR_ORGANIZATION_ID = "organization-id";
      public static final String ATTR_PROJECT_NAME = "project-name";
      public static final String METADATA_PREFIX = "_meta-";
      public static final String ATTR_META_ICON = METADATA_PREFIX + "icon";
      public static final String ATTR_META_COLOR = METADATA_PREFIX + "color";

   }

   public static class Organization {

      public static final String COLLECTION_NAME = "_system-organization";
      public static final String ATTR_ORG_ID = Document.ID;
      public static final String ATTR_ORG_CODE = "organization-code";
      public static final String ATTR_ORG_NAME = "organization-name";
      public static final String ATTR_ORG_DATA = "organization-info-data";

      public static final String METADATA_PREFIX = "_meta-";
      public static final String ATTR_META_ICON = METADATA_PREFIX + "icon";
      public static final String ATTR_META_COLOR = METADATA_PREFIX + "color";

   }

   public static class UserSettings {

      public static final String COLLECTION_NAME = "_system-usersettings";
      public static final String ATTR_USER = "user";
      public static final String ATTR_DEFAULT_ORGANIZATION = "default-organization";
      public static final String ATTR_DEFAULT_PROJECT = "default-project";

   }

   public static class UserGroup {

      public static final String COLLECTION_NAME = "_system-usergroup";
      public static final String ATTR_ORG_ID = "organization-id";
      public static final String ATTR_USERS = "users";
      public static final String ATTR_USERS_USER = "user";
      public static final String ATTR_USERS_GROUPS = "groups";

   }

   public static class Group {

      public static final String COLLECTION_NAME = "_system-group";
      public static final String ATTR_ORG_ID = "organization-id";
      public static final String ATTR_GROUPS = "groups";

   }

   public static class Configuration {

      public static final String NAMEVALUE = "namevalue";
      public static final String CONFIGS = "configs";
      public static final String CONFIGS_CONFIG_KEY = "key";
      public static final String CONFIGS_CONFIG_VALUE = "value";
      public static final String CONFIGS_CONFIG_DESCRIPTION = "description";
      public static final String CONFIGS_CONFIG_FLAG_RESTRICTED = "restricted";

   }
}
