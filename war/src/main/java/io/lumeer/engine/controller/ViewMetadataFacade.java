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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
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

   @Inject
   private SequenceFacade sequenceFacade;

   @Inject
   private UserFacade userFacade;

   // example of view metadata structure:
   // -------------------------------------
   // {
   // “internal-name” : “thisismyview_0”,
   // “name” : “This is my view.”,
   // “sequence-number” : 111,
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
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   public String createInternalName(String originalViewName) throws ViewAlreadyExistsException {
      if (checkIfViewUserNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      String name = originalViewName.trim();
      name = name.replace(' ', '_').toLowerCase();
      name = Utils.normalize(name);
      name = name.replaceAll("[^_a-z0-9]+", "");
      int i = 0;
      while (checkIfViewInternalNameExists(name + "_" + i)) {
         i++;
      }
      name = name + "_" + i;

      return name;
   }

   /**
    * Creates initial metadata for view
    *
    * @param originalViewName
    *       name given by user
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   public void createInitialMetadata(String originalViewName) throws ViewAlreadyExistsException {
      Map<String, Object> metadata = new HashMap<>();
      if (checkIfViewUserNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      metadata.put(LumeerConst.View.VIEW_REAL_NAME_KEY, originalViewName);

      String internalName = createInternalName(originalViewName);
      metadata.put(LumeerConst.View.VIEW_INTERNAL_NAME_KEY, internalName);

      int sequenceNumber = sequenceFacade.getNext(LumeerConst.View.VIEW_SEQUENCE_NAME);
      metadata.put(LumeerConst.View.VIEW_SEQUENCE_NUMBER_KEY, sequenceNumber);

      String createUser = userFacade.getUserName();
      metadata.put(LumeerConst.View.VIEW_CREATE_USER_KEY, createUser);

      String date = Utils.getCurrentTimeString();
      metadata.put(LumeerConst.View.VIEW_CREATE_DATE_KEY, date);

      dataStorage.createDocument(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, new DataDocument(metadata));
   }

   /**
    * @param viewName
    *       internal view name
    * @return DataDocument with all metadata about given view
    * @throws ViewMetadataNotFoundException
    *       when metadata for view with given name does not exist
    */
   public DataDocument getViewMetadata(String viewName) throws ViewMetadataNotFoundException {
      // TODO: check access rights
      List<DataDocument> viewList = dataStorage.run(queryOneViewMetadata(viewName));
      if (viewList.isEmpty()) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataNotFoundString(viewName));
      }

      return viewList.get(0);
   }

   /**
    * @param viewName
    *       internal view name
    * @param metaKey
    *       key of value we want to get
    * @return specific value from view metadata
    * @throws ViewMetadataNotFoundException
    *       when metadata does not exist
    */
   public Object getViewMetadataValue(String viewName, String metaKey) throws ViewMetadataNotFoundException {
      // TODO: check access rights
      Object value = getViewMetadata(viewName).get(metaKey);
      if (value == null) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataValueNotFoundString(viewName, metaKey));
      }
      return value;
   }

   /**
    * Sets view metadata value. If the given key does not exist, it is created. Otherwise it is just updated
    *
    * @param viewName
    *       internal view name
    * @param metaKey
    *       key of value we want to set
    * @param value
    *       value we want to set
    * @throws ViewMetadataNotFoundException
    *       when metadata for the view does not exist
    * @throws UnsuccessfulOperationException
    *       when metadata cannot be set
    */
   public void setViewMetadataValue(String viewName, String metaKey, Object value) throws ViewMetadataNotFoundException, UnsuccessfulOperationException {
      // TODO: check access rights

      if (LumeerConst.View.VIEW_IMMUTABLE_KEYS.contains(metaKey)) { // we check if the meta key is not between fields that cannot be changed
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.viewMetaImmutableString(viewName, metaKey));
      }

      String id = getViewMetadata(viewName).getId();
      Map<String, Object> metadataMap = new HashMap<>();
      metadataMap.put(metaKey, value);

      // with every change, we change update user and date
      String currentUser = userFacade.getUserName();
      metadataMap.put(LumeerConst.View.VIEW_UPDATE_USER_KEY, currentUser);
      String date = Utils.getCurrentTimeString();
      metadataMap.put(LumeerConst.View.VIEW_UPDATE_DATE_KEY, date);

      dataStorage.updateDocument(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, new DataDocument(metadataMap), id, -1);
   }

   /**
    * Converts sequence number to internal name.
    *
    * @param sequenceNumber
    *       sequence number of the view
    * @return internal name of the view with given sequence number
    * @throws ViewMetadataNotFoundException
    *       when metadata for the view does not exist
    */
   public String getViewInternalNameFromSequenceNumber(int sequenceNumber) throws ViewMetadataNotFoundException {
      // TODO: check access rights
      List<DataDocument> viewList = dataStorage.run(queryViewMetadataFromSequenceNumber(sequenceNumber));
      if (viewList.isEmpty()) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataNotFoundString("sequence number: " + sequenceNumber));
      }

      String value = viewList.get(0).getString(LumeerConst.View.VIEW_INTERNAL_NAME_KEY);
      if (value == null) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataValueNotFoundString("sequence number: " + sequenceNumber, LumeerConst.View.VIEW_INTERNAL_NAME_KEY));
      }
      return value;
   }

   // returns MongoDb query for getting metadata for one given view and its sequence number
   private String queryViewMetadataFromSequenceNumber(int number) {
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME)
            .append("\",filter:{\"")
            .append(LumeerConst.View.VIEW_SEQUENCE_NUMBER_KEY)
            .append("\":\"")
            .append(number)
            .append("\"}}");
      String query = sb.toString();
      return query;
   }

   // returns MongoDb query for getting metadata for one given view
   private String queryOneViewMetadata(String viewName) {
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME)
            .append("\",filter:{\"")
            .append(LumeerConst.View.VIEW_INTERNAL_NAME_KEY)
            .append("\":\"")
            .append(viewName)
            .append("\"}}");
      String viewMetaQuery = sb.toString();
      return viewMetaQuery;
   }

   private List<DataDocument> getViewsMetadata() {
      return dataStorage.search(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, null, null, 0, 0);
   }

   private boolean checkIfViewUserNameExists(String originalViewName) {
      for (DataDocument v : getViewsMetadata()) {
         if (v.get(LumeerConst.View.VIEW_REAL_NAME_KEY).toString().equals(originalViewName)) {
            return true;
         }
      }
      return false;
   }

   private boolean checkIfViewInternalNameExists(String viewName) {
      for (DataDocument v : getViewsMetadata()) {
         if (v.get(LumeerConst.View.VIEW_INTERNAL_NAME_KEY).toString().equals(viewName)) {
            return true;
         }
      }
      return false;
   }
}
