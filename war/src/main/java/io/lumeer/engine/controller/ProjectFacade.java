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
import io.lumeer.engine.api.LumeerConst.Project;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

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

   private String projectCode = "default";
   private String projectId = null;

   /**
    * Gets unique and immutable id of the project - _id from DataDocument
    *
    * @param projectCode
    *       project code
    * @return id
    */
   public String getProjectId(final String projectCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectCodeFilter(projectCode), Collections.singletonList(Project.ATTR_PROJECT_ID));
      return document != null ? document.getString(Project.ATTR_PROJECT_ID) : null;
   }

   public String getCurrentProjectId() {
      return projectId;
   }

   public String getCurrentProjectCode() {
      return projectCode;
   }

   public void setCurrentProjectCode(final String projectCode) {
      this.projectCode = projectCode;
      this.projectId = getProjectId(projectCode);
   }

   /**
    * Reads all projects in an organisation that is specified as a parameter
    *
    * @param organizationCode
    *       code of the organization
    * @return list of all projects in an organization
    */
   public List<io.lumeer.engine.api.dto.Project> readProjects(final String organizationCode) {
      return dataStorage.search(Project.COLLECTION_NAME, organizationIdFilter(organizationFacade.getOrganizationId(organizationCode)), null, 0 ,0)
                        .stream().map(io.lumeer.engine.api.dto.Project::new).collect(Collectors.toList());
   }

   /**
    * Changes project code
    *
    * @param oldProjectCode
    *       code of the project to change
    * @param newProjectCode
    *       new code for project
    */
   public void updateProjectCode(final String oldProjectCode, final String newProjectCode) {
      DataDocument document = new DataDocument(Project.ATTR_PROJECT_CODE, newProjectCode);
      dataStorage.updateDocument(Project.COLLECTION_NAME, document, projectCodeFilter(oldProjectCode));
   }

   /**
    * Reads the project name according to its code
    *
    * @param projectCode
    *       code of the project
    * @return name of the project
    */
   public String readProjectName(final String projectCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectCodeFilter(projectCode), Collections.singletonList(Project.ATTR_PROJECT_NAME));
      return document != null ? document.getString(Project.ATTR_PROJECT_NAME) : null;
   }

   /**
    * Updates single project metadata
    *
    * @param projectCode
    *       code of the project to make appropriate changes
    * @param metaName
    *       name of meta attribute to update
    * @param value
    *       value of meta attribute
    */
   public void updateProjectMetadata(final String projectCode, final String metaName, final String value) {
      updateProjectMetadata(projectCode, new DataDocument(metaName, value));
   }

   /**
    * Update multiple project metadata
    *
    * @param projectCode
    *       code of the project to make appropriate changes
    * @param meta
    *       key-value pairs of metadata to update
    */
   public void updateProjectMetadata(final String projectCode, DataDocument meta) {
      dataStorage.updateDocument(Project.COLLECTION_NAME, meta, projectCodeFilter(projectCode));
   }

   /**
    * Reads the specific project metadata
    *
    * @param projectCode
    *       code of the project
    * @param metaName
    *       name of the meta attribute to read
    * @return meta attribute value
    */
   public String readProjectMetadata(final String projectCode, final String metaName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectCodeFilter(projectCode), Collections.singletonList(metaName));
      return document != null ? document.getString(metaName) : null;
   }

   /**
    * Removes the specific project metadata
    *
    * @param projectCode
    *       code of the project
    * @param metaName
    *       name of the meta attribute to remove
    */
   public void dropProjectMetadata(final String projectCode, final String metaName) {
      dataStorage.dropAttribute(Project.COLLECTION_NAME, projectCodeFilter(projectCode), metaName);
   }

   /**
    * Renames project
    *
    * @param projectCode
    *       code of the project to rename
    * @param newProjectName
    *       new name of the project
    */
   public void renameProject(final String projectCode, final String newProjectName) {
      DataDocument dataDocument = new DataDocument(Project.ATTR_PROJECT_NAME, newProjectName);
      dataStorage.updateDocument(Project.COLLECTION_NAME, dataDocument, projectCodeFilter(projectCode));
   }

   /**
    * Creates new project
    *
    * @param projectCode
    *       code of the project to create
    * @param projectName
    *       name of the project
    */
   public void createProject(final String projectCode, final String projectName) {
      DataDocument document = new DataDocument(Project.ATTR_PROJECT_CODE, projectCode)
            .append(Project.ATTR_PROJECT_NAME, projectName)
            .append(Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      dataStorage.createDocument(Project.COLLECTION_NAME, document);
      databaseInitializer.onProjectCreated(projectCode);
   }

   /**
    * Drops the project if it hasn't assigned any collection or view
    *
    * @param projectCode
    *       code of the project to drop
    */
   public void dropProject(final String projectCode) {
      dataStorage.dropDocument(Project.COLLECTION_NAME, projectCodeFilter(projectCode));
   }

   private DataFilter projectCodeFilter(String projectCode) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      filter.put(Project.ATTR_PROJECT_CODE, projectCode);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter organizationIdFilter(final String organizationId) {
      return dataStorageDialect.fieldValueFilter(Project.ATTR_ORGANIZATION_ID, organizationId);
   }
}
