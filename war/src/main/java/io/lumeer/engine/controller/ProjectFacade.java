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

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.LumeerConst.Project;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Manipulates with project related data.
 */
@RequestScoped
public class ProjectFacade {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   private String projectId = "default";

   /**
    * Gets unique and immutable identificator of the project - _id from DataDocument
    *
    * @param projectId
    *       project id
    * @return identificator
    */
   public String getProjectIdentificator(final String projectId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectIdFilter(projectId), Collections.singletonList(LumeerConst.Document.ID));
      return document != null ? document.getString(LumeerConst.Document.ID) : null;
   }

   public String getCurrentProjectId() {
      return projectId;
   }

   public void setCurrentProjectId(final String projectId) {
      this.projectId = projectId;
   }

   /**
    * Reads all projects in an organisation that is specified as a parameter
    *
    * @param organizationId
    *       name of the organization
    * @return map(id, name) values of all projects in an organization
    */
   public Map<String, String> readProjectsMap(final String organizationId) {
      List<DataDocument> documents = dataStorage.search(Project.COLLECTION_NAME, dataStorageDialect.fieldValueFilter(Project.ATTR_ORGANIZATION_ID, organizationId), Arrays.asList(Project.ATTR_PROJECT_NAME, Project.ATTR_PROJECT_ID));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(Project.ATTR_PROJECT_ID), d -> d.getString(Project.ATTR_PROJECT_NAME)));
   }

   /**
    * Changes project id
    *
    * @param oldProjectId
    *       id of the project to change
    * @param newProjectId
    *       new id for project
    */
   public void updateProjectId(final String oldProjectId, final String newProjectId) {
      DataDocument document = new DataDocument(Project.ATTR_PROJECT_ID, newProjectId);
      dataStorage.updateDocument(Project.COLLECTION_NAME, document, projectIdFilter(oldProjectId));
   }

   /**
    * Reads the project name according to its id
    *
    * @param projectId
    *       id of the project
    * @return name of the project
    */
   public String readProjectName(final String projectId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectIdFilter(projectId), Collections.singletonList(Project.ATTR_PROJECT_NAME));
      return document != null ? document.getString(Project.ATTR_PROJECT_NAME) : null;
   }

   /**
    * Updates single project metadata
    *
    * @param projectId
    *       id of the project to make appropriate changes
    * @param metaName
    *       name of meta attribute to update
    * @param value
    *       value of meta attribute
    */
   public void updateProjectMetadata(final String projectId, final String metaName, final String value) {
      updateProjectMetadata(projectId, new DataDocument(metaName, value));
   }

   /**
    * Update multiple project metadata
    *
    * @param projectId
    *       id of the project to make appropriate changes
    * @param meta
    *       key-value pairs of metadata to update
    */
   public void updateProjectMetadata(final String projectId, DataDocument meta) {
      dataStorage.updateDocument(Project.COLLECTION_NAME, meta, projectIdFilter(projectId));
   }

   /**
    * Reads the specific project metadata
    *
    * @param projectId
    *       id of the project
    * @param metaName
    *       name of the meta attribute to read
    * @return meta attribute value
    */
   public String readProjectMetadata(final String projectId, final String metaName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectIdFilter(projectId), Collections.singletonList(metaName));
      return document != null ? document.getString(metaName) : null;
   }

   /**
    * Removes the specific project metadata
    *
    * @param projectId
    *       id of the project
    * @param metaName
    *       name of the meta attribute to remove
    */
   public void dropProjectMetadata(final String projectId, final String metaName) {
      dataStorage.dropAttribute(Project.COLLECTION_NAME, projectIdFilter(projectId), metaName);
   }

   /**
    * Renames project
    *
    * @param projectId
    *       id of the project to rename
    * @param newProjectName
    *       new name of the project
    */
   public void renameProject(final String projectId, final String newProjectName) {
      DataDocument dataDocument = new DataDocument(Project.ATTR_PROJECT_NAME, newProjectName);
      dataStorage.updateDocument(Project.COLLECTION_NAME, dataDocument, projectIdFilter(projectId));
   }

   /**
    * Creates new project
    *
    * @param projectId
    *       id of the project to create
    * @param projectName
    *       name of the project
    */
   public void createProject(final String projectId, final String projectName) {
      DataDocument document = new DataDocument(Project.ATTR_PROJECT_ID, projectId)
            .append(Project.ATTR_PROJECT_NAME, projectName)
            .append(Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      dataStorage.createDocument(Project.COLLECTION_NAME, document);
   }

   /**
    * Drops the project if it hasn't assigned any collection or view
    *
    * @param projectId
    *       id of the project to drop
    */
   public void dropProject(final String projectId) {
      dataStorage.dropDocument(Project.COLLECTION_NAME, projectIdFilter(projectId));
   }

   private DataFilter projectIdFilter(String projectId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      filter.put(Project.ATTR_PROJECT_ID, projectId);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter projectNameFilter(String projectName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      filter.put(Project.ATTR_PROJECT_NAME, projectName);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

}
