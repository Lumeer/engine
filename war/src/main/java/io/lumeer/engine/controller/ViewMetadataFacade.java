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
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@SessionScoped
public class ViewMetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   private static final String META_TYPE_KEY = "meta-type";
   private static final String VIEW_METADATA_PREFIX = "meta.";
   private static final String VIEW_NAME_PREFIX = "view.";

   private static final String VIEW_TYPE_META_TYPE_VALUE = "type";
   private static final String VIEW_TYPE_KEY = "type";

   private static final String VIEW_STYLES_META_TYPE_VALUE = "styles";
   private static final String VIEW_ALL_STYLES_KEY = "styles";
   private static final String VIEW_STYLE_TYPE_KEY = "type";
   private static final String VIEW_STYLE_VALUE_KEY = "style";
   private static final String VIEW_STYLE_CONDITION_KEY = "condition";

   private static final String VIEW_REAL_NAME_META_TYPE_VALUE = "name";
   private static final String VIEW_REAL_NAME_KEY = "name";

   private static final String VIEW_ACCESS_RIGHTS_META_TYPE_VALUE = "rights";

   // example of view metadata structure:
   // -------------------------------------
   // {
   //	“meta-type” : “type”,
   //	“type” : “table”
   // },
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
    * Same as createInternalName, just with view prefix
    *
    * @param originalViewName
    *       name given by user
    * @return
    *       internal name of view
    */
   public String viewNameToInternalForm(String originalViewName) {
      String name = originalViewName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
      return VIEW_NAME_PREFIX + name;
   }

   /**
    * @param viewName
    *       internal view name
    * @return name of collection with metadata for given view
    */
   public String viewMetadataCollectionName(String viewName) {
      return VIEW_METADATA_PREFIX + viewName;
   }

   /**
    * @param viewName
    *       internal view name
    * @return view type
    */
   public String getViewType(String viewName) {
      String query = queryViewType(viewName);
      List<DataDocument> viewInfo = dataStorage.run(query);

      DataDocument viewDocument = viewInfo.get(0);
      String type = viewDocument.getString(VIEW_TYPE_KEY);
      return type;
   }

   // returns MongoDb query for getting view type
   private String queryViewType(String viewName) {
      String metadataCollectionName = viewMetadataCollectionName(viewName);
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(VIEW_TYPE_META_TYPE_VALUE)
            .append("\"}}");
      String findTypeQuery = sb.toString();
      return findTypeQuery;
   }
}
