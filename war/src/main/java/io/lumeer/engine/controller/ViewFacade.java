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

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
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
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private SequenceFacade sequenceFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private ProjectFacade projectFacade;

   /**
    * Creates initial metadata for the view
    *
    * @param originalViewName
    *       name given by user
    * @param viewType
    *       type of the view
    * @param configuration
    *       configuration of the view
    * @return view id
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   public int createView(
         final String originalViewName,
         final String viewType,
         final String description,
         final DataDocument configuration) throws ViewAlreadyExistsException {

      if (!dataStorage.hasCollection(metadataCollection())) {
         dataStorage.createCollection(metadataCollection());
         // we create indexes on frequently used fields
         int indexType = LumeerConst.Index.ASCENDING;
         dataStorage.createIndex(metadataCollection(), new DataDocument(LumeerConst.View.ID_KEY, indexType), false);
         dataStorage.createIndex(metadataCollection(), new DataDocument(LumeerConst.View.TYPE_KEY, indexType), false);
         dataStorage.createIndex(metadataCollection(), new DataDocument(LumeerConst.View.NAME_KEY, indexType), false);
      }

      if (checkIfViewNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      int viewId = sequenceFacade.getNext(LumeerConst.View.SEQUENCE_NAME); // generates id
      final String createUser = getCurrentUser();
      DataDocument metadataDocument = new DataDocument(LumeerConst.View.NAME_KEY, originalViewName)
            .append(LumeerConst.View.ID_KEY, viewId)
            .append(LumeerConst.View.DESCRIPTION_KEY, description)
            .append(LumeerConst.View.PROJECT_ID, projectFacade.getCurrentProjectId())
            .append(LumeerConst.View.CREATE_USER_KEY, createUser)
            .append(LumeerConst.View.CREATE_DATE_KEY, new Date())
            .append(LumeerConst.View.TYPE_KEY, viewType)
            .append(LumeerConst.View.CONFIGURATION_KEY, configuration != null ? configuration : new DataDocument());

      // create user has complete access
      securityFacade.setRightsRead(metadataDocument, createUser);
      securityFacade.setRightsWrite(metadataDocument, createUser);
      securityFacade.setRightsExecute(metadataDocument, createUser);

      dataStorage.createDocument(metadataCollection(), metadataDocument);

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
    * @throws ViewAlreadyExistsException
    *       when view with given name already exists
    */
   public int copyView(int viewId, String newName) throws ViewAlreadyExistsException {
      ViewDao originalView = getViewMetadata(viewId);
      return createView(
            newName,
            originalView.getType(),
            originalView.getDescription(),
            originalView.getConfiguration());
   }

   public ViewDao getViewMetadata(int viewId) {
      DataDocument viewMetadata = getViewMetadataDocument(viewId);
      if (viewMetadata == null) {
         return null;
      }
      return new ViewDao(viewMetadata);
   }

   /**
    * @param viewId
    *       view id
    * @return type of the given view
    */
   public String getViewType(int viewId) {
      ViewDao metadata = getViewMetadata(viewId);
      return metadata != null ? metadata.getType() : null;
   }

   /**
    * @param viewId
    *       view id
    * @param type
    *       view type
    */
   public void setViewType(int viewId, String type) {
      setViewMetadataValue(viewId, LumeerConst.View.TYPE_KEY, type);
      // TODO verify if the type can be changed - maybe it can be changed only from default?
   }

   /**
    * @param viewId
    *       view id
    * @return view name
    */
   public String getViewName(int viewId) {
      ViewDao metadata = getViewMetadata(viewId);
      return metadata != null ? metadata.getName() : null;
   }

   /**
    * @param viewId
    *       view id
    * @param name
    *       new view name
    */
   public void setViewName(int viewId, String name) throws ViewAlreadyExistsException {
      if (checkIfViewNameExists(name)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(name));
      }

      setViewMetadataValue(viewId, LumeerConst.View.NAME_KEY, name);
   }

   /**
    * @param viewId
    *       view id
    * @param description
    *       view description
    */
   public void setViewDescription(int viewId, String description) {
      setViewMetadataValue(viewId, LumeerConst.View.DESCRIPTION_KEY, description);
   }

   /**
    * @param viewId
    *       view id
    * @return view description
    */
   public String getViewDescription(int viewId) {
      ViewDao metadata = getViewMetadata(viewId);
      return metadata != null ? metadata.getDescription() : null;
   }

   /**
    * @param viewId
    *       view id
    * @return view configuration
    */
   public DataDocument getViewConfiguration(int viewId) {
      ViewDao metadata = getViewMetadata(viewId);
      return metadata != null ? metadata.getConfiguration() : null;
   }

   /**
    * @param viewId
    *       view id
    * @param configuration
    *       view configuration
    */
   public void setViewConfiguration(int viewId, DataDocument configuration) {
      setViewMetadataValue(viewId, LumeerConst.View.CONFIGURATION_KEY, configuration);
   }

   /**
    * @param viewId
    *       view id
    * @param attributeName
    *       view configuration attribute name
    * @return value of view configuration attribute
    */
   public Object getViewConfigurationAttribute(int viewId, String attributeName) {
      ViewDao metadata = getViewMetadata(viewId);
      return metadata != null ? metadata.getConfiguration().get(attributeName) : null;
   }

   /**
    * @param viewId
    *       view id
    * @param attributeName
    *       view configuration attribute name
    * @param attributeValue
    *       value of view configuration attribute
    */
   public void setViewConfigurationAttribute(int viewId, String attributeName, Object attributeValue) {
      setViewMetadataValue(viewId,
            dataStorageDialect.concatFields(LumeerConst.View.CONFIGURATION_KEY, attributeName),
            attributeValue);
   }

   /**
    * @param viewId
    *       view id
    * @return DataDocument with all metadata about given view
    */
   public DataDocument getViewMetadataDocument(int viewId) {
      return dataStorage.readDocument(metadataCollection(), dataStorageDialect.fieldValueFilter(LumeerConst.View.ID_KEY, viewId));
   }

   /**
    * @return list of ViewDao for all views
    */
   public List<ViewDao> getAllViews() {
      List<DataDocument> views = dataStorage.search(metadataCollection(), null, null, 0, 0);
      return createListOfDaos(views);
   }

   /**
    * @param type
    *       type of the view
    * @return list of ViewDao for all views of given type
    */
   public List<ViewDao> getAllViewsOfType(String type) {
      List<DataDocument> views = dataStorage.search(
            metadataCollection(),
            dataStorageDialect.fieldValueFilter(LumeerConst.View.TYPE_KEY, type),
            null, 0, 0);
      return createListOfDaos(views);
   }

   // creates list of daos from list of documents and performs security checks
   private List<ViewDao> createListOfDaos(List<DataDocument> views) {
      List<ViewDao> viewsDaos = new ArrayList<>();

      for (DataDocument view : views) {
         viewsDaos.add(new ViewDao(view));
      }

      return viewsDaos;
   }

   private boolean checkIfViewNameExists(String viewName) {
      return dataStorage.getAttributeValues(metadataCollection(), LumeerConst.View.NAME_KEY).contains(viewName);
   }

   /**
    * Updates access rights of a view.
    *
    * @param viewDocument
    *       The view document with updated rights.
    */
   public void updateViewAccessRights(final DataDocument viewDocument) {
      setViewMetadataValue(viewDocument.getInteger(LumeerConst.View.ID_KEY), LumeerConst.Document.USER_RIGHTS, viewDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS));
   }

   public boolean checkViewForRead(int viewId, String user) {
      return securityFacade.checkForRead(getViewMetadataDocument(viewId), user);
   }

   public boolean checkViewForWrite(int viewId, String user) {
      return securityFacade.checkForWrite(getViewMetadataDocument(viewId), user);
   }

   public boolean checkViewForAccessChange(int viewId, String user) {
      return securityFacade.checkForExecute(getViewMetadataDocument(viewId), user);
   }

   private void setViewMetadataValue(int viewId, String metaKey, Object value) {
      DataDocument metadataDocument = new DataDocument(metaKey, value)
            .append(LumeerConst.View.UPDATE_USER_KEY, getCurrentUser())    // with every change, we change update user and date
            .append(LumeerConst.View.UPDATE_DATE_KEY, new Date());

      dataStorage.updateDocument(
            metadataCollection(),
            metadataDocument,
            dataStorageDialect.fieldValueFilter(LumeerConst.View.ID_KEY, viewId));
   }

   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }

   /**
    * @return name of metadata collection for current project
    */
   public String metadataCollection() {
      return metadataCollection(projectFacade.getCurrentProjectId());
   }

   /**
    * @param projectId
    *       project id
    * @return name of metadata collection for given project id
    */
   private String metadataCollection(String projectId) {
      return LumeerConst.View.METADATA_COLLECTION_PREFIX + projectId;
   }
}
