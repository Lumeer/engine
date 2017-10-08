/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.engine.controller;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Project;

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

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

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
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(projectCode), Collections.singletonList(LumeerConst.Project.ATTR_PROJECT_ID));
      return document != null ? document.getString(LumeerConst.Project.ATTR_PROJECT_ID) : null;
   }

   /**
    * Gets unique and immutable id of the project - _id from DataDocument
    *
    * @param organizationId
    *       Organization identificator
    * @param projectCode
    *       project code
    * @return id
    */
   public String getProjectId(final String organizationId, final String projectCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(organizationId, projectCode), Collections.singletonList(LumeerConst.Project.ATTR_PROJECT_ID));
      return document != null ? document.getString(LumeerConst.Project.ATTR_PROJECT_ID) : null;
   }

   /**
    * Gets unique project code
    *
    * @param organizationId
    *       Organization identificator
    * @param projectId
    *       Project identificator.
    * @return Project code
    */
   public String getProjectCode(final String organizationId, final String projectId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Project.COLLECTION_NAME, projectIdFilter(organizationId, projectId), Collections.singletonList(LumeerConst.Project.ATTR_PROJECT_CODE));
      return document != null ? document.getString(LumeerConst.Project.ATTR_PROJECT_CODE) : null;
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
    * Reads all projects in an organization that is specified as a parameter
    *
    * @param organizationCode
    *       code of the organization
    * @return list of all projects in an organization
    */
   public List<Project> readProjects(final String organizationCode) {
      return dataStorage.search(LumeerConst.Project
            .COLLECTION_NAME, organizationIdFilter(organizationFacade.getOrganizationId(organizationCode)), null, 0, 0)
                        .stream().map(Project::new)
                        .collect(Collectors.toList());
   }

   /**
    * Reads the project data according to its code
    *
    * @param projectCode
    *       code of the project
    * @return project data
    */
   public Project readProject(final String projectCode) {
      DataDocument dataDocument = dataStorage.readDocument(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(projectCode));
      return dataDocument != null ? new Project(dataDocument) : null;
   }

   /**
    * Creates new project in the system database.
    *
    * @param project
    *       project data
    * @return id of the organization
    */
   public String createProject(final Project project) {
      DataDocument dataDocument = project.toDataDocument()
                                         .append(LumeerConst.Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      String id = dataStorage.createDocument(LumeerConst.Project.COLLECTION_NAME, dataDocument);
      databaseInitializer.onProjectCreated(id);

      List<String> user = Collections.singletonList(userFacade.getUserEmail());
      securityFacade.addProjectUsersRole(project.getCode(), user, LumeerConst.Security.ROLE_READ);
      securityFacade.addProjectUsersRole(project.getCode(), user, LumeerConst.Security.ROLE_MANAGE);
      securityFacade.addProjectUsersRole(project.getCode(), user, LumeerConst.Security.ROLE_WRITE);

      return id;
   }

   /**
    * Updates existing project in the system database.
    *
    * @param projectCode
    *       code of the project
    * @param project
    *       project data
    */
   public void updateProject(final String projectCode, final Project project) {
      dataStorage.updateDocument(LumeerConst.Project.COLLECTION_NAME, project.toDataDocument(), projectCodeFilter(projectCode));
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
      DataDocument document = new DataDocument(LumeerConst.Project.ATTR_PROJECT_CODE, newProjectCode);
      dataStorage.updateDocument(LumeerConst.Project.COLLECTION_NAME, document, projectCodeFilter(oldProjectCode));
   }

   /**
    * Reads the project name according to its code
    *
    * @param projectCode
    *       code of the project
    * @return name of the project
    */
   public String readProjectName(final String projectCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(projectCode), Collections.singletonList(LumeerConst.Project.ATTR_PROJECT_NAME));
      return document != null ? document.getString(LumeerConst.Project.ATTR_PROJECT_NAME) : null;
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
      dataStorage.updateDocument(LumeerConst.Project.COLLECTION_NAME, meta, projectCodeFilter(projectCode));
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
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(projectCode), Collections.singletonList(metaName));
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
      dataStorage.dropAttribute(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(projectCode), metaName);
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
      DataDocument dataDocument = new DataDocument(LumeerConst.Project.ATTR_PROJECT_NAME, newProjectName);
      dataStorage.updateDocument(LumeerConst.Project.COLLECTION_NAME, dataDocument, projectCodeFilter(projectCode));
   }

   /**
    * Drops the project if it hasn't assigned any collection or view
    *
    * @param projectCode
    *       code of the project to drop
    */
   public void dropProject(final String projectCode) {
      dataStorage.dropDocument(LumeerConst.Project.COLLECTION_NAME, projectCodeFilter(projectCode));
   }

   private DataFilter projectCodeFilter(String projectCode) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Project.ATTR_ORGANIZATION_ID, organizationFacade.getOrganizationId());
      filter.put(LumeerConst.Project.ATTR_PROJECT_CODE, projectCode);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter projectCodeFilter(String organiationId, String projectCode) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Project.ATTR_ORGANIZATION_ID, organiationId);
      filter.put(LumeerConst.Project.ATTR_PROJECT_CODE, projectCode);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter projectIdFilter(String organizationId, String projectId) {
      return dataStorageDialect.combineFilters(
            dataStorageDialect.fieldValueFilter(LumeerConst.Project.ATTR_ORGANIZATION_ID, organizationId),
            dataStorageDialect.documentIdFilter(projectId)
      );
   }

   private DataFilter organizationIdFilter(final String organizationId) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Project.ATTR_ORGANIZATION_ID, organizationId);
   }
}
