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
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@SessionScoped
public class ViewMetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   public static final String VIEW_METADATA_COLLECTION_NAME = "viewmetadatacollection";
   private static final String VIEW_NAME_PREFIX = "view.";

   public static final String VIEW_ID_NAME_KEY = "id";
   public static final String VIEW_REAL_NAME_KEY = "name";
   public static final String VIEW_TYPE_KEY = "type";

   public static final String VIEW_ALL_STYLES_KEY = "styles";
   public static final String VIEW_STYLE_TYPE_KEY = "type";
   public static final String VIEW_STYLE_VALUE_KEY = "style";
   public static final String VIEW_STYLE_CONDITION_KEY = "condition";

   public static final String VIEW_USER_RIGHTS_KEY = "rights";
   public static final String VIEW_GROUP_RIGHTS_KEY = "group-rights";
   public static final String VIEW_CREATE_DATE_KEY = "create-date";
   public static final String VIEW_CREATE_USER_KEY = "create-user";
   public static final String VIEW_UPDATE_DATE_KEY = "update-date";
   public static final String VIEW_UPDATE_USER_KEY = "update-user";

   // example of view metadata structure:
   // -------------------------------------
   // {
   // “id” : “view.thisismyview_0”,
   // “name” : “This is my view.”,
   //	“type” : “table”,
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
   //    ],
   //	   “create-date” : date,
   //	   “update-date” : date,
   //	   “create-user” : user_name,
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
    * Converts view name given by user to internal representation.
    * First, the name is trimmed of whitespaces.
    * Spaces are replaced by "_". Converted to lowercase.
    * Diacritics are replaced by ASCII characters.
    * Everything except a-z, 0-9 and _ is removed.
    * Number is added to the end of the name to ensure it is unique.
    *
    * @param originalViewName
    *       name given by user
    * @return internal view name
    */
   public String createInternalName(String originalViewName) throws ViewAlreadyExistsException {
      if (checkIfViewUserNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      String name = originalViewName.trim();
      name = name.replace(' ', '_').toLowerCase();
      name = Utils.normalize(name);
      name = name.replaceAll("[^_a-z0-9]+", "");
      name = VIEW_NAME_PREFIX + name;
      int i = 0;
      while (checkIfViewIdExists(name + "_" + i)) {
         i++;
      }
      name = name + "_" + i;

      return name;
   }

   /**
    * Creates initial metadata for view (so far only user name and id name)
    * @param originalViewName name given by user
    * @throws ViewAlreadyExistsException
    */
   public void createInitialMetadata(String originalViewName) throws ViewAlreadyExistsException {
      Map<String, Object> metadata = new HashMap<>();
      if (checkIfViewUserNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }
      String id = createInternalName(originalViewName);
      metadata.put(VIEW_REAL_NAME_KEY, originalViewName);
      metadata.put(VIEW_ID_NAME_KEY, id);

      // TODO add other metadata

      dataStorage.createDocument(VIEW_METADATA_COLLECTION_NAME, new DataDocument(metadata));
   }

   /**
    *
    * @param viewId internal view name
    * @return DataDocument with all metadata about given view
    * @throws ViewMetadataNotFoundException
    */
   public DataDocument getViewMetadata(String viewId) throws ViewMetadataNotFoundException {
      List<DataDocument> viewList = dataStorage.run(queryOneViewMetadata(viewId));
      if (viewList.isEmpty()) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataNotFoundString(viewId));
      }

      return viewList.get(0);
   }

   /**
    * @param viewId
    *       internal view name
    * @param metaKey
    *       key of value we want to get
    * @return specific value from view metadata
    * @throws ViewMetadataNotFoundException
    */
   public Object getViewMetadataValue(String viewId, String metaKey) throws ViewMetadataNotFoundException {
      Object value = getViewMetadata(viewId).get(metaKey);
      if (value == null) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataValueNotFoundString(viewId, metaKey));
      }
      return value;
   }

   /**
    * Sets view metadata value. If the given key does not exist, it is created. Otherwise it is just updated
    *
    * @param viewId
    *       internal view name
    * @param metaKey
    *       key of value we want to set
    * @param value
    *       value we want to set
    * @throws ViewMetadataNotFoundException
    */
   public void setViewMetadataValue(String viewId, String metaKey, Object value) throws ViewMetadataNotFoundException {
      String id = getViewMetadata(viewId).getId();
      Map<String, Object> metadataMap = new HashMap<>();
      metadataMap.put(metaKey, value);
      dataStorage.updateDocument(VIEW_METADATA_COLLECTION_NAME, new DataDocument(metadataMap), id, -1);
   }

   // returns MongoDb query for getting metadata for one given view
   private String queryOneViewMetadata(String viewId) {
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(VIEW_METADATA_COLLECTION_NAME)
            .append("\",filter:{\"")
            .append(VIEW_ID_NAME_KEY)
            .append("\":\"")
            .append(viewId)
            .append("\"}}");
      String findTypeQuery = sb.toString();
      return findTypeQuery;
   }

   private List<DataDocument> getViewsMetadata() {
      return dataStorage.search(VIEW_METADATA_COLLECTION_NAME, null, null, 0, 0);
   }

   private boolean checkIfViewUserNameExists(String originalViewName) {
      for (DataDocument v : getViewsMetadata()) {
         if (v.get(VIEW_REAL_NAME_KEY).toString().equals(originalViewName)) {
            return true;
         }
      }
      return false;
   }

   private boolean checkIfViewIdExists(String viewId) {
      for (DataDocument v : getViewsMetadata()) {
         if (v.get(VIEW_ID_NAME_KEY).toString().equals(viewId)) {
            return true;
         }
      }
      return false;
   }
}
