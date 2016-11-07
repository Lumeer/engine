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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class MetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   // table name prefixes, column names and other constants used in metadata

   public final String META_TYPE_KEY = "meta-type";

   // ------------------------------------------------------------
   // ACCESS RIGHTS GENERALLY
   // ------------------------------------------------------------
   public final String CREATE_DATE_KEY = "create-date";
   public final String UPDATE_DATE_KEY = "update-date";
   public final String CREATE_BY_USER_KEY = "creator-user";
   public final String UPDATED_BY_USER_KEY = "update-user";
   public final String ACCESS_RIGHTS_KEY = "rights";

   // ------------------------------------------------------------
   // COLLECTION METADATA
   // ------------------------------------------------------------
   public final String COLLECTION_METADATA_PREFIX = "meta.";
   public final String COLLECTION_COLUMNS_META_TYPE_VALUE = "columns";
   public final String COLLECTION_COLUMN_NAME_KEY = "name";
   public final String COLLECTION_COLUMN_TYPE_KEY = "type";
   // column types according to DataDocument methods, empty is default and is considered String
   // TODO: What about nested columns? Should we return them as a string?
   public final String[] COLLECTION_COLUMN_TYPE_VALUES = { "int", "long", "double", "boolean", "date", "" };
   public final String COLLECTION_REAL_NAME_META_TYPE_VALUE = "name";
   public final String COLLECTION_REAL_NAME_KEY = "name";
   public final String COLLECTION_NAME_PREFIX = "collection.";
   // TODO: access rights

   // example of collection metadata structure:
   // -------------------------------------
   // {
   //  “meta-type” : “columns”,
   //  “name” : “column1”,
   //  “type” : “int”
   // },
   // {
   //  “meta-type” : “columns”,
   //  “name” : “column2”,
   //  “type” : “”
   // },
   // {
   // “meta-type” : “name”,
   // “name” : “This is my collection name.”
   // }

   // ------------------------------------------------------------
   // DOCUMENT METADATA
   // ------------------------------------------------------------
   public final String DOCUMENT_METADATA_PREFIX = "meta-";
   public final String DOCUMENT_CREATE_DATE_KEY = DOCUMENT_METADATA_PREFIX + CREATE_DATE_KEY;
   public final String DOCUMENT_UPDATE_DATE_KEY = DOCUMENT_METADATA_PREFIX + UPDATE_DATE_KEY;
   public final String DOCUMENT_CREATE_BY_USER_KEY = DOCUMENT_METADATA_PREFIX + CREATE_BY_USER_KEY;
   public final String DOCUMENT_UPDATED_BY_USER_KEY = DOCUMENT_METADATA_PREFIX + UPDATED_BY_USER_KEY;
   public final String DOCUMENT_RIGHTS_KEY = DOCUMENT_METADATA_PREFIX + ACCESS_RIGHTS_KEY;

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
   public final String VIEW_METADATA_PREFIX = "meta.";
   public final String VIEW_STYLES_META_TYPE_VALUE = "styles";
   public final String VIEW_ACCESS_RIGHTS_META_TYPE_VALUE = "rights";
   public final String VIEW_ALL_STYLES_KEY = "styles";
   public final String VIEW_STYLE_TYPE_KEY = "type";
   public final String VIEW_STYLE_VALUE_KEY = "style";
   public final String VIEW_STYLE_CONDITION_KEY = "condition";
   public final String VIEW_NAME_PREFIX = "view.";

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

   //   /**
   //    * Converts collection name given by user to internal representation.
   //    * First, spaces, diacritics, special characters etc. are removed from name
   //    * which is converted to lowercase. Secondly, we add prefix and suffix
   //    * so we get the name int the form _collection-{name}-{creatorUserId}
   //    * to also make sure it's unique.
   //    *
   //    * @param originalCollectionName
   //    *       name given by user
   //    * @param creatorUserId
   //    *       id of the user who created the collection
   //    * @return internal collection name
   //    */
   //   public String collectionNameToInternalForm(String originalCollectionName, String creatorUserId) {
   //      String name = originalCollectionName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
   //      return COLLECTION_NAME_PREFIX + name + "-" + creatorUserId;
   //   }
   //
   //   /**
   //    * Same as collectionNameToInternalForm, just with view prefix
   //    * @param originalViewName
   //    * @param creatorUserId
   //    * @return
   //    */
   //   public String viewNameToInternalForm(String originalViewName, String creatorUserId) {
   //      String name = originalViewName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
   //      return VIEW_NAME_PREFIX + name + "-" + creatorUserId;
   //   }

   /**
    * Converts collection name given by user to internal representation.
    * First, spaces, diacritics, special characters etc. are removed from name
    * which is converted to lowercase. Secondly, we add collection prefix.
    *
    * @param originalCollectionName
    *       name given by user
    * @return internal collection name
    */
   public String collectionNameToInternalForm(String originalCollectionName) {
      String name = originalCollectionName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
      return COLLECTION_NAME_PREFIX + name;
   }

   /**
    * Same as collectionNameToInternalForm, just with view prefix
    *
    * @param originalViewName
    *       name given by user
    * @return
    */
   public String viewNameToInternalForm(String originalViewName) {
      String name = originalViewName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
      return VIEW_NAME_PREFIX + name;
   }

   /**
    * Returns info about collection columns
    * @param collectionName internal collection name
    * @return map - keys are column names, values are types
    */
   public Map<String, String> getCollectionColumnsInfo(String collectionName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      String query = queryCollectionColumnsInfo(metadataCollectionName);
      List<DataDocument> columnsInfoDocuments = dataStorage.search(query);

      Map<String, String> columnsInfo = new HashMap<>();

      for (int i = 0; i < columnsInfoDocuments.size(); i++) {
         String name = columnsInfoDocuments.get(i).getString(COLLECTION_COLUMN_NAME_KEY);
         String type = columnsInfoDocuments.get(i).getString(COLLECTION_COLUMN_TYPE_KEY);
         columnsInfo.put(name, type);
      }

      return columnsInfo;
   }

   /**
    * Adds a column to collection metadata.
    * @param collectionName internal collection names
    * @param columnName added column name
    * @param columnType added column type
    * @return true if add is successful
    */
   public boolean addCollectionColumn(String collectionName, String columnName, String columnType) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      String query = queryCollectionColumnInfo(metadataCollectionName, columnName);
      List<DataDocument> columnInfo = dataStorage.search(query);

      // return false if the column already exists
      if (!columnInfo.isEmpty()) {
         return false;
      }

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(META_TYPE_KEY, COLLECTION_COLUMNS_META_TYPE_VALUE);
      metadata.put(COLLECTION_COLUMN_NAME_KEY, columnName);
      metadata.put(COLLECTION_COLUMN_TYPE_KEY, columnType);
      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.createDocument(metadataCollectionName, metadataDocument);

      return true;
   }

   /**
    * Renames existing column in collection metadata.
    * @param collectionName internal collection names
    * @param oldName old column name
    * @param newName new column name
    * @return true if rename is successful
    */
   public boolean renameCollectionColumn(String collectionName, String oldName, String newName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      String query = queryCollectionColumnInfo(metadataCollectionName, oldName);
      List<DataDocument> columnInfo = dataStorage.search(query);

      // the column does not exist
      if (columnInfo.isEmpty()) {
         return false;
      }

      DataDocument columnDocument = columnInfo.get(0);
      String documentId = columnDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      if (!newName.isEmpty()) {
         metadata.put(COLLECTION_COLUMN_NAME_KEY, newName);
         DataDocument metadataDocument = new DataDocument(metadata);
         dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId);
         return true;
      }

      return false;
   }

   /**
    * Changes column type in metadata.
    * @param collectionName internal collection name
    * @param columnName column name
    * @param newType new column type
    * @return true if retype is successful
    */
   public boolean retypeCollectionColumn(String collectionName, String columnName, String newType) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      String query = queryCollectionColumnInfo(metadataCollectionName, columnName);
      List<DataDocument> columnInfo = dataStorage.search(query);

      // the column does not exist
      if (columnInfo.isEmpty()) {
         return false;
      }

      DataDocument columnDocument = columnInfo.get(0);
      String documentId = columnDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      if (!newType.isEmpty()) {
         metadata.put(COLLECTION_COLUMN_TYPE_KEY, newType);
         DataDocument metadataDocument = new DataDocument(metadata);
         dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId);

         return true;
      }

      return false;
   }

   /**
    * Deletes a column from collection metadata
    * @param collectionName internal collection name
    * @param columnName column to be deleted
    * @return true if delete is successful
    */
   public boolean dropCollectionColumn(String collectionName, String columnName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      String query = queryCollectionColumnInfo(metadataCollectionName, columnName);
      List<DataDocument> columnInfo = dataStorage.search(query);

      // the column does not exist
      if (columnInfo.isEmpty()) {
         return false;
      }

      DataDocument columnDocument = columnInfo.get(0);
      String documentId = columnDocument.get("_id").toString();

      dataStorage.dropDocument(metadataCollectionName, documentId);

      return true;
   }

   /**
    * Searches for original (given by user) collection name in metadata
    * @param collectionName internal collection name
    * @return original collection name
    */
   public String getOriginalCollectionName(String collectionName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      String query = queryCollectionNameInfo(metadataCollectionName);
      List<DataDocument> nameInfo = dataStorage.search(query);

      DataDocument nameDocument = nameInfo.get(0);
      String name = nameDocument.getString(COLLECTION_REAL_NAME_KEY);

      return name;
   }

    /**
    * Sets original (given by user) collection name in metadata
    * @param collectionOriginalName name given by user
    */
   public void setOriginalCollectionName(String collectionOriginalName) {
      String collectionInternalName = collectionNameToInternalForm(collectionOriginalName);
      String metadataCollectionName = collectionMetadataCollectionName(collectionInternalName);

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(META_TYPE_KEY, COLLECTION_REAL_NAME_META_TYPE_VALUE);
      metadata.put(COLLECTION_REAL_NAME_KEY, collectionOriginalName);
      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.createDocument(metadataCollectionName, metadataDocument);
   }

   // returns MongoDb query for getting info about specific column
   private String queryCollectionColumnInfo(String metadataCollectionName, String columnName) {
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_COLUMNS_META_TYPE_VALUE)
            .append("\",\"")
            .append(COLLECTION_COLUMN_NAME_KEY)
            .append("\":\"")
            .append(columnName)
            .append("\"}}");
      String findColumnQuery = sb.toString();
      return findColumnQuery;
   }

   // returns MongoDb query for getting real collection name
   private String queryCollectionNameInfo(String metadataCollectionName) {
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_REAL_NAME_META_TYPE_VALUE)
            .append("\"}}");
      String findNameQuery = sb.toString();
      return findNameQuery;
   }

   // returns MongoDb query for getting info about all columns
   private String queryCollectionColumnsInfo(String metadataCollectionName) {
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_COLUMNS_META_TYPE_VALUE)
            .append("\"}}");
      String findColumnsQuery = sb.toString();
      return findColumnsQuery;
   }

   /**
    *
    * @param collectionName internal collection name
    * @return name of metadata collection
    */
   public String collectionMetadataCollectionName(String collectionName) {
      return COLLECTION_METADATA_PREFIX + collectionName;
   }

   /**
    *
    * @param collectionName internal collection name
    * @return true if the name is a name of metadata collection
    */
   public boolean isMetadataCollection(String collectionName) {
      String prefix = collectionName.substring(0, COLLECTION_METADATA_PREFIX.length());
      return COLLECTION_METADATA_PREFIX.equals(prefix);
   }

}
