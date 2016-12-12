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
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
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

   @Inject
   private SecurityFacade securityFacade;

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
   public String createInternalName(String originalViewName) {
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
    * @return internal view name
    */
   public String createInitialMetadata(String originalViewName) throws ViewAlreadyExistsException {
      if (checkIfViewUserNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      Map<String, Object> metadata = new HashMap<>();

      metadata.put(LumeerConst.View.VIEW_REAL_NAME_KEY, originalViewName);

      String internalName = createInternalName(originalViewName);
      metadata.put(LumeerConst.View.VIEW_INTERNAL_NAME_KEY, internalName);

      int sequenceNumber = sequenceFacade.getNext(LumeerConst.View.VIEW_SEQUENCE_NAME);
      metadata.put(LumeerConst.View.VIEW_SEQUENCE_NUMBER_KEY, sequenceNumber);

      String createUser = userFacade.getUserName();
      metadata.put(LumeerConst.View.VIEW_CREATE_USER_KEY, createUser);

      String date = Utils.getCurrentTimeString();
      metadata.put(LumeerConst.View.VIEW_CREATE_DATE_KEY, date);

      DataDocument metadataDocument = new DataDocument(metadata);

      securityFacade.setRightsRead(metadataDocument, createUser);
      securityFacade.setRightsWrite(metadataDocument, createUser);
      securityFacade.setRightsExecute(metadataDocument, createUser);

      dataStorage.createDocument(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, metadataDocument);

      return internalName;
   }

   /**
    * @param viewName
    *       internal view name
    * @return DataDocument with all metadata about given view
    * @throws ViewMetadataNotFoundException
    *       when metadata for view with given name does not exist
    */
   public DataDocument getViewMetadata(String viewName) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewName);

      String user = userFacade.getUserName();
      if (!securityFacade.checkForRead(viewDocument, user)) {
         throw new UnauthorizedAccessException();
      }

      return viewDocument;
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
   public Object getViewMetadataValue(String viewName, String metaKey) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      Object value = getViewMetadata(viewName).get(metaKey); // access rights are checked in getViewMetadata
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
   public void setViewMetadataValue(String viewName, String metaKey, Object value) throws ViewMetadataNotFoundException, UnsuccessfulOperationException, UnauthorizedAccessException {
      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewName);

      String user = userFacade.getUserName();
      if (!securityFacade.checkForWrite(viewDocument, user)) {
         throw new UnauthorizedAccessException();
      }

      if (LumeerConst.View.VIEW_IMMUTABLE_KEYS.contains(metaKey)) { // we check if the meta key is not between fields that cannot be changed
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.viewMetaImmutableString(viewName, metaKey));
      }

      String id = viewDocument.getId();
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

   private DataDocument getViewMetadataWithoutAccessCheck(String viewName) throws ViewMetadataNotFoundException {
      List<DataDocument> viewList = dataStorage.run(queryOneViewMetadata(viewName));
      if (viewList.isEmpty()) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataNotFoundString(viewName));
      }

      return viewList.get(0);
   }
}
