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
import io.lumeer.engine.rest.dao.ViewMetadata;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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

   @Inject
   private DatabaseInitializer databaseInitializer;

   /**
    * Creates initial metadata for the view
    *
    * @param originalViewName
    *       name given by user
    * @param viewType
    *       type of the view
    * @param description
    *       view description
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

      if (checkIfViewNameExists(originalViewName)) {
         throw new ViewAlreadyExistsException(ErrorMessageBuilder.viewUsernameAlreadyExistsString(originalViewName));
      }

      int viewId = sequenceFacade.getNext(LumeerConst.View.SEQUENCE_NAME); // generates id
      final String createUser = getCurrentUser();
      DataDocument metadataDocument = new DataDocument(LumeerConst.View.NAME_KEY, originalViewName)
            .append(LumeerConst.View.ID_KEY, viewId)
            .append(LumeerConst.View.DESCRIPTION_KEY, description)
            .append(LumeerConst.View.CREATE_USER_KEY, createUser)
            .append(LumeerConst.View.CREATE_DATE_KEY, new Date())
            .append(LumeerConst.View.UPDATE_USER_KEY, null)
            .append(LumeerConst.View.UPDATE_DATE_KEY, null)
            .append(LumeerConst.View.TYPE_KEY, viewType)
            .append(LumeerConst.View.CONFIGURATION_KEY, configuration != null ? configuration : new DataDocument());

      dataStorage.createDocument(metadataCollection(), metadataDocument);

      String project = projectFacade.getCurrentProjectCode();
      databaseInitializer.onViewCreated(project, viewId);
      securityFacade.addViewUserRole(project, viewId, createUser, LumeerConst.Security.ROLE_MANAGE);

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
      ViewMetadata originalView = getViewMetadata(viewId);
      return createView(
            newName,
            originalView.getType(),
            originalView.getDescription(),
            originalView.getConfiguration());
      // TODO: also copy access rights
   }

   /**
    * @param viewId
    *       view id
    * @return object with view metadata
    */
   public ViewMetadata getViewMetadata(int viewId) {
      DataDocument viewMetadata = getViewMetadataDocument(viewId);
      return viewMetadata != null ? new ViewMetadata(viewMetadata) : null;
   }

   /**
    * @param viewId
    *       view id
    * @return type of the given view
    */
   public String getViewType(int viewId) {
      return (String) getViewMetadataValue(viewId, LumeerConst.View.TYPE_KEY);
   }

   /**
    * @param viewId
    *       view id
    * @param type
    *       view type
    */
   public void setViewType(int viewId, String type) {
      setViewMetadataValue(viewId, LumeerConst.View.TYPE_KEY, type);
   }

   /**
    * @param viewId
    *       view id
    * @return view name
    */
   public String getViewName(int viewId) {
      return (String) getViewMetadataValue(viewId, LumeerConst.View.NAME_KEY);
   }

   /**
    * @param viewId
    *       view id
    * @param name
    *       new view name
    * @throws ViewAlreadyExistsException
    *       if view with given name already exists
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
      return (String) getViewMetadataValue(viewId, LumeerConst.View.DESCRIPTION_KEY);
   }

   /**
    * @param viewId
    *       view id
    * @return view configuration
    */
   public DataDocument getViewConfiguration(int viewId) {
      return (DataDocument) getViewMetadataValue(viewId, LumeerConst.View.CONFIGURATION_KEY);
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
      return getViewConfiguration(viewId).get(attributeName);
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
    * @return list of ViewMetadata for all views from current project
    */
   public List<ViewMetadata> getAllViews() {
      return getAllViews(projectFacade.getCurrentProjectCode());
   }

   /**
    * @param type
    *       type of the view
    * @return list of ViewMetadata for all views of given type from current project
    */
   public List<ViewMetadata> getAllViewsOfType(String type) {
      return getAllViewsOfType(projectFacade.getCurrentProjectCode(), type);
   }

   /**
    * @param projectId
    *       project id
    * @return list of ViewMetadata for all views from given project
    */
   public List<ViewMetadata> getAllViews(String projectId) {
      List<DataDocument> views = dataStorage.search(
            metadataCollection(projectId), null, null, 0, 0);
      return createListOfViews(filterViewsForUser(views)); // TODO: filter in query
   }

   /**
    * @param projectId
    *       project id
    * @param type
    *       type of the view
    * @return list of ViewMetadata for all views of given type from given project
    */
   public List<ViewMetadata> getAllViewsOfType(String projectId, String type) {
      List<DataDocument> views = dataStorage.search(
            metadataCollection(projectId),
            dataStorageDialect.fieldValueFilter(LumeerConst.View.TYPE_KEY, type),
            null, 0, 0);

      return createListOfViews(filterViewsForUser(views)); // TODO: filter in query
   }

   /**
    * Filters out only views that can be read by current user
    *
    * @param views
    *       list of all views
    * @return filtered list of views
    */
   private List<DataDocument> filterViewsForUser(List<DataDocument> views) {
      final String user = getCurrentUser();

      return views.stream()
                  // TODO: .filter(view -> checkViewForRead(view.getInteger(LumeerConst.View.ID_KEY), user))
                  .collect(Collectors.toList());
   }

   // creates list of ViewMetadata objects from list of DataDocuments
   private List<ViewMetadata> createListOfViews(List<DataDocument> viewsDocuments) {
      return viewsDocuments.stream().map(ViewMetadata::new).collect(Collectors.toList());
   }

   private boolean checkIfViewNameExists(String viewName) {
      return dataStorage.collectionHasDocument(metadataCollection(), dataStorageDialect.fieldValueFilter(LumeerConst.View.NAME_KEY, viewName));
   }

   private Object getViewMetadataValue(int viewId, String key) {
      DataDocument metadata = dataStorage.readDocumentIncludeAttrs(
            metadataCollection(),
            dataStorageDialect.fieldValueFilter(LumeerConst.View.ID_KEY, viewId),
            Arrays.asList(key));
      return metadata != null ? metadata.get(key) : null;
   }

   private void setViewMetadataValue(int viewId, String metaKey, Object value) {
      DataDocument metadataDocument = new DataDocument(metaKey, value);
      setUpdateTimeAndUser(metadataDocument);

      dataStorage.updateDocument(
            metadataCollection(),
            metadataDocument,
            dataStorageDialect.fieldValueFilter(LumeerConst.View.ID_KEY, viewId));
   }

   private DataDocument setUpdateTimeAndUser(DataDocument updatedDocument) {
      return updatedDocument
            .append(LumeerConst.View.UPDATE_USER_KEY, getCurrentUser())    // with every change, we change update user and date
            .append(LumeerConst.View.UPDATE_DATE_KEY, new Date());
   }

   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }

   /**
    * @return name of view metadata collection for current organization
    */
   public String metadataCollection() {
      return metadataCollection(projectFacade.getCurrentProjectCode());
   }

   /**
    * @param projectCode
    *       project code
    * @return name of view metadata collection for given project id
    */
   public String metadataCollection(String projectCode) {
      return LumeerConst.View.METADATA_COLLECTION_PREFIX + projectFacade.getProjectId(projectCode);
   }
}
