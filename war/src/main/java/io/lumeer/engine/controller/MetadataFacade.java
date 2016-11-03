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
package io.lumeer.engine.controller;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class MetadataFacade implements Serializable {

   // table name prefixes, column names and other constants used in metadata

   public final String META_TYPE_COLUMN_NAME = "meta-type";

   // ------------------------------------------------------------
   // ACCESS RIGHTS GENERALLY
   // ------------------------------------------------------------
   public final String CREATE_DATE_COLUMN = "create-date";
   public final String UPDATE_DATE_COLUMN = "update-date";
   public final String CREATE_BY_USER_COLUMN = "creator-user";
   public final String UPDATED_BY_USER_COLUMN = "update-user";
   public final String ACCESS_RIGHTS_COLUMN = "rights";

   // ------------------------------------------------------------
   // COLLECTION METADATA
   // ------------------------------------------------------------
   public final String COLLECTION_METADATA_PREFIX = "_meta-";
   public final String META_TYPE_VALUE_FOR_COLLECTION_COLUMNS = "columns";
   public final String NAME_OF_COLUMN_WITH_ALL_COLLECTION_COLUMNS_INFO = "columns";
   public final String NAME_OF_COLUMN_WITH_COLLECTION_COLUMN_NAME = "name";
   public final String NAME_OF_COLUMN_WITH_COLLECTION_COLUMN_TYPE = "type";
   // column types according to DataDocument methods, empty is default and is considered String
   // TODO: What about nested columns? Should we return them as a string?
   public final String[] COLUMN_TYPES = { "int", "long", "double", "boolean", "date", "" };
   public final String META_TYPE_VALUE_FOR_COLLECTION_NAME = "name";
   public final String NAME_OF_COLUMN_WITH_COLLECTION_NAME = "name";
   public final String COLLECTION_NAME_PREFIX = "_collection-";
   // TODO: access rights

   // example of collection metadata structure:
   // -------------------------------------
   // {
   // “meta-type” : “columns”,
   // “columns” : [
   //    {
   //    “name” : “column1”,
   //    “type” : “int”
   //    },
   //   {
   //    “name” : “column2”,
   //    “type” : “”
   //   }
   //  ]
   // },
   // {
   // “meta-type” : “name”,
   // “name” : “This is my collection name.”
   // }

   // ------------------------------------------------------------
   // DOCUMENT METADATA
   // ------------------------------------------------------------
   public final String DOCUMENT_METADATA_PREFIX = "_meta-";
   public final String DOCUMENT_CREATE_DATE_COLUMN = DOCUMENT_METADATA_PREFIX + CREATE_DATE_COLUMN;
   public final String DOCUMENT_UPDATE_DATE_COLUMN = DOCUMENT_METADATA_PREFIX + UPDATE_DATE_COLUMN;
   public final String DOCUMENT_CREATE_BY_USER_COLUMN = DOCUMENT_METADATA_PREFIX + CREATE_BY_USER_COLUMN;
   public final String DOCUMENT_UPDATED_BY_USER_COLUMN = DOCUMENT_METADATA_PREFIX + UPDATED_BY_USER_COLUMN;
   public final String DOCUMENT_RIGHTS_COLUMN = DOCUMENT_METADATA_PREFIX + ACCESS_RIGHTS_COLUMN;

   // example of document metadata structure:
   // -------------------------------------
   // {
   //	“_meta-create-date” : date,
   //	“_meta-update-date” : date,
   //	“_meta-creator-user” : user_name,
   //	“_meta-update-user” : user_name,
   //	“_meta-rights” : [
   //      user_name1 : 1  //execute permissions
   //      user_name2 : 2  //write permissions
   //      user_name3 : 4  //read permissions
   //      user_name4 : 3  //execute and write permissions = 1 + 2
   //      user_name5 : 5  //read and execute permissions = 1 + 4
   //      user_name6 : 6  //write and read permissions = 2 + 4
   //      user_name7 : 7  //full permissions = 1 + 2 + 4
   //      others     : 0  //others is forced to be there, maybe if not presented, that means 0 permissions
   //    ],
   // “_meta-group-rights” : [
   //      group_name1: 1  //execute permissions
   //      group_name2: 2  //write permissions
   //      group_name3: 4  //read permissions
   //      group_name4: 3  //execute and write permissions = 1 + 2
   //      group_name5: 5  //read and execute permissions = 1 + 4
   //      group_name6: 6  //write and read permissions = 2 + 4
   //      group_name7: 7  //full permissions = 1 + 2 + 4
   //	   ]
   //	… (rest of the document) …
   // }

   // ------------------------------------------------------------
   // VIEW METADATA
   // ------------------------------------------------------------
   public final String VIEW_METADATA_PREFIX = "_meta-";
   public final String META_TYPE_VALUE_FOR_STYLES = "styles";
   public final String META_TYPE_VALUE_FOR_ACCESS_RIGHTS = "rights";
   public final String NAME_OF_COLUMN_WITH_STYLES = "styles";
   public final String NAME_OF_COLUMN_WITH_STYLE_TYPE = "type";
   public final String NAME_OF_COLUMN_WITH_STYLE_VALUE = "style";
   public final String NAME_OF_COLUMN_WITH_STYLE_CONDITION = "condition";
   public final String VIEW_NAME_PREFIX = "_view-";

   // example of view metadata structure:
   // -------------------------------------
   // {
   // “meta-type” : “styles”,
   // “styles” : [
   //    {
   //    “type” : “column”,
   //    “style” : “color:red;”,
   //    “condition” :
   //       {
   //       … a representation of condition …
   //       }
   //    },
   //    {
   //    “type” : “row”,
   //    “style” : “color:green;”,
   //    “condition” :
   //       {
   //          … a representation of condition …
   //       }
   //     }
   //    ]
   // },
   // {
   //    “meta-type” : “rights”,
   //	   “create-date” : date,
   //	   “update-date” : date,
   //	   “creator-user” : user_name,
   //	   “update-user” : user_name,
   //	   “rights” : [
   //       user_name1 : 1  //execute permissions
   //       user_name2 : 2  //write permissions
   //       user_name3 : 4  //read permissions
   //       user_name4 : 3  //execute and write permissions = 1 + 2
   //       user_name5 : 5  //read and execute permissions = 1 + 4
   //       user_name6 : 6  //write and read permissions = 2 + 4
   //       user_name7 : 7  //full permissions = 1 + 2 + 4
   //       others     : 0  //others is forced to be there, maybe if not presented, that means 0 permissions
   //    ],
   //    “group-rights” : [
   //       group_name1: 1  //execute permissions
   //       group_name2: 2  //write permissions
   //       group_name3: 4  //read permissions
   //       group_name4: 3  //execute and write permissions = 1 + 2
   //       group_name5: 5  //read and execute permissions = 1 + 4
   //       group_name6: 6  //write and read permissions = 2 + 4
   //       group_name7: 7  //full permissions = 1 + 2 + 4
   //	   ]
   // }

   /**
    * Converts collection name given by user to internal representation.
    * First, spaces, diacritics, special characters etc. are removed from name
    * which is converted to lowercase. Secondly, we add prefix and suffix
    * so we get the name int the form _collection-{name}-{creatorUserId}
    * to also make sure it's unique.
    *
    * @param originalCollectionName
    *       name given by user
    * @param creatorUserId
    *       id of the user who created the collection
    * @return internal collection name
    */
   public String collectionNameToInternalForm(String originalCollectionName, String creatorUserId) {
      String name = originalCollectionName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
      return COLLECTION_NAME_PREFIX + name + "-" + creatorUserId;
   }

   /**
    * Same as collectionNameToInternalForm, just with view prefix
    * @param originalViewName
    * @param creatorUserId
    * @return
    */
   public String viewNameToInternalForm(String originalViewName, String creatorUserId) {
      String name = originalViewName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
      return VIEW_NAME_PREFIX + name + "-" + creatorUserId;
   }

}
