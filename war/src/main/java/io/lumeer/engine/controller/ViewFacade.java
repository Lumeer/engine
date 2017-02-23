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
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;
import io.lumeer.engine.rest.dao.ViewDao;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@SessionScoped
public class ViewFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   @Inject
   private SequenceFacade sequenceFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private SecurityFacade securityFacade;

   /**
    * Creates initial metadata for the view
    *
    * @param originalViewName
    *       name given by user
    * @param viewType
    *       The type of the view.
    * @return view id
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   public int createView(final String originalViewName, final String viewType, final DataDocument configuration) throws ViewAlreadyExistsException {
      if (!dataStorage.hasCollection(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME)) {
         dataStorage.createCollection(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME);
         // we create indexes on frequently used fields
         int indexType = 1;
         dataStorage.createIndex(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, new DataDocument(LumeerConst.View.VIEW_ID_KEY, indexType));
         dataStorage.createIndex(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, new DataDocument(LumeerConst.View.VIEW_TYPE_KEY, indexType));
         dataStorage.createIndex(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, new DataDocument(LumeerConst.View.VIEW_NAME_KEY, indexType));
      }

      if (checkIfViewNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      int viewId = sequenceFacade.getNext(LumeerConst.View.VIEW_SEQUENCE_NAME); // generates id
      final String createUser = getCurrentUser();
      DataDocument metadataDocument = new DataDocument(LumeerConst.View.VIEW_NAME_KEY, originalViewName)
            .append(LumeerConst.View.VIEW_ID_KEY, viewId)
            .append(LumeerConst.View.VIEW_CREATE_USER_KEY, createUser)
            .append(LumeerConst.View.VIEW_CREATE_DATE_KEY, new Date())
            .append(LumeerConst.View.VIEW_TYPE_KEY, viewType) // sets view type to default
            .append(LumeerConst.View.VIEW_CONFIGURATION_KEY, configuration != null ? configuration : new DataDocument());

      // create user has complete access
      securityFacade.setRightsRead(metadataDocument, createUser);
      securityFacade.setRightsWrite(metadataDocument, createUser);
      securityFacade.setRightsExecute(metadataDocument, createUser);

      dataStorage.createDocument(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, metadataDocument);

      return viewId;
   }

   /**
    * Creates a copy of the view. User must have read right to original view, and will have read, write and execute rights on the copy.
    *
    * @param viewId
    *       id of view to be copied
    * @param newName
    *       name of the copy of the view
    * @return id of view copy
    * @throws ViewMetadataNotFoundException
    *       when metadata about copied view was not found
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read copied view
    */
   public int copyView(int viewId, String newName) throws ViewMetadataNotFoundException, ViewAlreadyExistsException, UnauthorizedAccessException {
      final DataDocument originalView = getViewMetadataWithoutAccessCheck(viewId);
      if (securityFacade.checkForRead(originalView, getCurrentUser())) {
         return createView(newName, originalView.getString(LumeerConst.View.VIEW_TYPE_KEY), originalView.getDataDocument(LumeerConst.View.VIEW_CONFIGURATION_KEY));
      } else {
         throw new UnauthorizedAccessException();
      }
   }

   /**
    * @param viewId
    *       view id
    * @return type of the given view
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   public String getViewType(int viewId) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      return (String) (getViewMetadataValue(viewId, LumeerConst.View.VIEW_TYPE_KEY));
   }

   /**
    * @param viewId
    *       view id
    * @param type
    *       view type
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the view
    */
   public void setViewType(int viewId, String type) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewId);
      setViewMetadataValue(viewDocument, LumeerConst.View.VIEW_TYPE_KEY, type);
      // TODO verify if the type can be changed - maybe it can be changed only from default?
   }

   /**
    * @param viewId
    *       view id
    * @return view name
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   public String getViewName(int viewId) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      return (String) (getViewMetadataValue(viewId, LumeerConst.View.VIEW_NAME_KEY));
   }

   /**
    * @param viewId
    *       view id
    * @param name
    *       new view name
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the view
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   public void setViewName(int viewId, String name) throws ViewMetadataNotFoundException, UnauthorizedAccessException, ViewAlreadyExistsException {
      if (checkIfViewNameExists(name)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(name));
      }

      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewId);
      setViewMetadataValue(viewDocument, LumeerConst.View.VIEW_NAME_KEY, name);
   }

   /**
    * @param viewId
    *       view id
    * @return view configuration
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   public DataDocument getViewConfiguration(int viewId) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      return (DataDocument) (getViewMetadataValue(viewId, LumeerConst.View.VIEW_CONFIGURATION_KEY));
   }

   /**
    * @param viewId
    *       view id
    * @param configuration
    *       view configuration
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the view
    */
   public void setViewConfiguration(int viewId, DataDocument configuration) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewId);
      setViewMetadataValue(viewDocument, LumeerConst.View.VIEW_CONFIGURATION_KEY, configuration);
   }

   /**
    * @param viewId
    *       view id
    * @param attributeName
    *       view configuration attribute name
    * @return value of view configuration attribute
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   public Object getViewConfigurationAttribute(int viewId, String attributeName) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      //return getViewMetadataValue(viewId, LumeerConst.View.VIEW_CONFIGURATION_KEY + "." + attributeName);
      Object value = getViewConfiguration(viewId).get(attributeName);
      if (value == null) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataValueNotFoundString(viewId, "configuration." + attributeName));
      }
      return value;
   }

   /**
    * @param viewId
    *       view id
    * @param attributeName
    *       view configuration attribute name
    * @param attributeValue
    *       value of view configuration attribute
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to write to the view
    */
   public void setViewConfigurationAttribute(int viewId, String attributeName, Object attributeValue) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewId);
      setViewMetadataValue(viewDocument, LumeerConst.View.VIEW_CONFIGURATION_KEY + "." + attributeName, attributeValue);
   }

   /**
    * @param viewId
    *       view id
    * @return DataDocument with all metadata about given view
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   public DataDocument getViewMetadata(int viewId) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      DataDocument viewDocument = getViewMetadataWithoutAccessCheck(viewId);

      if (!securityFacade.checkForRead(viewDocument, getCurrentUser())) {
         throw new UnauthorizedAccessException();
      }

      return viewDocument;
   }

   /**
    * @param viewId
    *       view id
    * @param metaKey
    *       key of value we want to get
    * @return specific value from view metadata
    * @throws ViewMetadataNotFoundException
    *       when view metadata was not found
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to read the view
    */
   public Object getViewMetadataValue(int viewId, String metaKey) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      Object value = getViewMetadata(viewId).get(metaKey); // access rights are checked in getViewMetadata
      if (value == null) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataValueNotFoundString(viewId, metaKey));
      }
      return value;
   }

   /**
    * @return list of ViewDao for all views
    */
   public List<ViewDao> getAllViews() {
      List<DataDocument> views = getViewsMetadata();
      return createListOfDaos(views);

   }

   /**
    * @param type
    *       type of the view
    * @return list of ViewDao for all views of given type
    */
   public List<ViewDao> getAllViewsOfType(String type) {
      List<DataDocument> views = getViewsOfTypeMetadata(type);
      return createListOfDaos(views);
   }

   // creates list of daos from list of documents and performs security checks
   private List<ViewDao> createListOfDaos(List<DataDocument> views) {
      List<ViewDao> viewsDaos = new ArrayList<>();
      for (DataDocument view : views) {
         if (securityFacade.checkForRead(view, getCurrentUser())) {
            ViewDao viewDao = new ViewDao(
                  view.getInteger(LumeerConst.View.VIEW_ID_KEY),
                  view.getString(LumeerConst.View.VIEW_NAME_KEY),
                  view.getString(LumeerConst.View.VIEW_TYPE_KEY),
                  view.getDataDocument(LumeerConst.View.VIEW_CONFIGURATION_KEY)
            );
            viewsDaos.add(viewDao);
         }
      }

      return viewsDaos;
   }

   // returns query for getting metadata for one given view
   private DataDocument queryOneViewMetadata(int viewId) {
      return new DataDocument()
            .append("find", LumeerConst.View.VIEW_METADATA_COLLECTION_NAME)
            .append("filter",
                  new DataDocument()
                        .append(LumeerConst.View.VIEW_ID_KEY, viewId));
   }

   // gets info about all views
   private List<DataDocument> getViewsMetadata() {
      return dataStorage.run(
            new DataDocument()
                  .append("find", LumeerConst.View.VIEW_METADATA_COLLECTION_NAME));
   }

   // gets info about all views of given type
   private List<DataDocument> getViewsOfTypeMetadata(String type) {
      return dataStorage.run(
            new DataDocument()
                  .append("find", LumeerConst.View.VIEW_METADATA_COLLECTION_NAME)
                  .append("filter",
                        new DataDocument()
                              .append(LumeerConst.View.VIEW_TYPE_KEY, type)));
   }

   private boolean checkIfViewNameExists(String originalViewName) {
      for (DataDocument v : getViewsMetadata()) {
         if (v.get(LumeerConst.View.VIEW_NAME_KEY).toString().equals(originalViewName)) {
            return true;
         }
      }
      return false;
   }

   // gets info about one view without checking access rights
   private DataDocument getViewMetadataWithoutAccessCheck(int viewId) throws ViewMetadataNotFoundException {
      List<DataDocument> viewList = dataStorage.run(queryOneViewMetadata(viewId));
      if (viewList.isEmpty()) {
         throw new ViewMetadataNotFoundException(ErrorMessageBuilder.viewMetadataNotFoundString(viewId));
      }

      return viewList.get(0);
   }

   /**
    * Updates access rights of a view.
    *
    * @param viewDocument
    *       The view document with updated rights.
    * @throws ViewMetadataNotFoundException
    *       The view was not found in the database.
    * @throws UnauthorizedAccessException
    *       The user is not authorized to updated access rights.
    */
   public void updateViewAccessRights(final DataDocument viewDocument) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      setViewMetadataValue(viewDocument, LumeerConst.Document.USER_RIGHTS, viewDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS));
   }

   // sets info about one view without checking special metadata keys
   private void setViewMetadataValue(DataDocument viewDocument, String metaKey, Object value) throws ViewMetadataNotFoundException, UnauthorizedAccessException {
      if (!securityFacade.checkForWrite(viewDocument, getCurrentUser())) {
         throw new UnauthorizedAccessException();
      }

      DataDocument metadataDocument = new DataDocument(metaKey, value)
            .append(LumeerConst.View.VIEW_UPDATE_USER_KEY, getCurrentUser())    // with every change, we change update user and date
            .append(LumeerConst.View.VIEW_UPDATE_DATE_KEY, new Date());

      String id = viewDocument.getId();
      dataStorage.updateDocument(LumeerConst.View.VIEW_METADATA_COLLECTION_NAME, metadataDocument, id);
   }

   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }
}
