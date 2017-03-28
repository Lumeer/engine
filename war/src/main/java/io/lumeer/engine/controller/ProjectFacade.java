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
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.LumeerConst.Project;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
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
   private OrganisationFacade organisationFacade;

   @Inject
   private ApplicationInitFacade applicationInitFacade;

   private String projectId = "default";

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
      List<DataDocument> documents = dataStorage.searchIncludeAttrs(Project.COLLECTION_NAME, dataStorageDialect.fieldValueFilter(Project.ATTR_ORGANIZATION_ID, organizationId), Arrays.asList(Project.ATTR_PROJECT_NAME, Project.ATTR_PROJECT_ID));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(Project.ATTR_PROJECT_ID), d -> d.getString(Project.ATTR_PROJECT_NAME)));
   }

   /**
    * Reads the project id according to its name
    *
    * @param projectName
    *       name of the project
    * @return id of the project
    */
   public String readProjectId(final String projectName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectNameFilter(projectName), Collections.singletonList(Project.ATTR_PROJECT_ID));
      return document != null ? document.getString(Project.ATTR_PROJECT_ID) : null;
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
            .append(Project.ATTR_ORGANIZATION_ID, organisationFacade.getOrganisationId());
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
      // TODO check cannot be implemented without other facades changes
   }

   /**
    * Reads default project roles for newly added users
    *
    * @param projectId
    *       id of the project
    * @return List of default user roles
    */
   public List<String> readDefaultRoles(final String projectId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectIdFilter(projectId), Collections.singletonList(Project.ATTR_META_DEFAULT_ROLES));
      return document != null ? document.getArrayList(Project.ATTR_META_DEFAULT_ROLES, String.class) : null;
   }

   /**
    * Set default roles for newly added users
    *
    * @param projectId
    *       id of the project
    * @param userRoles
    *       List of the user roles to set
    */
   public void setDefaultRoles(final String projectId, final List<String> userRoles) {
      DataDocument document = new DataDocument(Project.ATTR_META_DEFAULT_ROLES, userRoles);
      dataStorage.updateDocument(Project.COLLECTION_NAME, document, projectIdFilter(projectId));
   }

   /**
    * Create new user role which consists of core roles
    *
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Unique name of user role
    * @param coreRoles
    *       List of the core roles to set
    */
   public void createRole(final String projectId, final String userRole, final List<String> coreRoles) {
      DataDocument document = new DataDocument(Project.UserRoles.ATTR_PROJECT_ID, projectId)
            .append(Project.UserRoles.ATTR_ORGANIZATION_ID, organisationFacade.getOrganisationId())
            .append(Project.UserRoles.ATTR_USER_ROLE, userRole)
            .append(Project.UserRoles.ATTR_CORE_ROLES, coreRoles);
      dataStorage.createDocument(Project.UserRoles.COLLECTION_NAME, document);
   }

   /**
    * Adds core roles to user role
    *
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Name of user role
    * @param coreRoles
    *       List of the core roles to add
    */
   public void addCoreRolesToRole(final String projectId, final String userRole, final List<String> coreRoles) {
      dataStorage.addItemsToArray(Project.UserRoles.COLLECTION_NAME, userRoleFilter(projectId, userRole), Project.UserRoles.ATTR_CORE_ROLES, coreRoles);
   }

   /**
    * Removes core role from user role
    *
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Name of user role
    * @param coreRoles
    *       List of the core roles to remove
    */
   public void removeCoreRolesFromRole(final String projectId, final String userRole, final List<String> coreRoles) {
      dataStorage.removeItemsFromArray(Project.UserRoles.COLLECTION_NAME, userRoleFilter(projectId, userRole), Project.UserRoles.ATTR_CORE_ROLES, coreRoles);
   }

   /**
    * Removes user role from project
    *
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Name of user role to delete
    */
   public void dropRole(final String projectId, final String userRole) {
      dataStorage.dropDocument(Project.UserRoles.COLLECTION_NAME, userRoleFilter(projectId, userRole));
   }

   /**
    * Reads project user roles
    *
    * @param projectId
    *       Id of the project
    */
   public Map<String, List<String>> readRoles(final String projectId) {
      List<DataDocument> documents = dataStorage.searchIncludeAttrs(Project.UserRoles.COLLECTION_NAME, projectIdFilter(projectId), Arrays.asList(Project.UserRoles.ATTR_USER_ROLE, Project.UserRoles.ATTR_CORE_ROLES));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(Project.UserRoles.ATTR_USER_ROLE), d -> d.getArrayList(Project.UserRoles.ATTR_CORE_ROLES, String.class)));
   }

   /**
    * Adds user to project with default roles
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    */
   public void addUserToProject(final String projectId, final String userName) {
      addUserToProject(projectId, userName, readDefaultRoles(projectId));
   }

   /**
    * Adds user to project with specific
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    */
   public void addUserToProject(final String projectId, final String userName, final List<String> userRoles) {
      DataDocument document = new DataDocument(Project.ATTR_USER, userName)
            .append(Project.ATTR_USER_ROLES, userRoles);
      dataStorage.addItemToArray(Project.COLLECTION_NAME, projectIdFilter(projectId), Project.ATTR_USERS, document);
   }

   /**
    * Adds roles to user
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    * @param userRoles
    *       Roles to add
    */
   public void addRolesToUser(final String projectId, final String userName, final List<String> userRoles) {
      dataStorage.addItemsToArray(Project.COLLECTION_NAME, userFilter(projectId, userName), dataStorageDialect.concatFields(Project.ATTR_USERS, "$", Project.ATTR_USER_ROLES), userRoles);
   }

   /**
    * Removes roles from user
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    * @param userRoles
    *       Roles to remove
    */
   public void removeRolesFromUser(final String projectId, final String userName, final List<String> userRoles) {
      dataStorage.removeItemsFromArray(Project.COLLECTION_NAME, userFilter(projectId, userName), dataStorageDialect.concatFields(Project.ATTR_USERS, "$", Project.ATTR_USER_ROLES), userRoles);
   }

   /**
    * Removes user from project
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    */
   public void dropUserFromProject(final String projectId, final String userName) {
      dataStorage.removeItemFromArray(Project.COLLECTION_NAME, projectIdFilter(projectId), Project.ATTR_USERS, new DataDocument(Project.ATTR_USER, userName));
   }

   /**
    * Reads roles for specific user
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    * @return list of roles
    */
   public List<String> readUserRoles(final String projectId, final String userName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, userFilter(projectId, userName), Collections.singletonList(dataStorageDialect.concatFields(Project.ATTR_USERS, "$")));
      if (document == null) {
         return null;
      }
      // we got only one subdocument otherwise there was null
      DataDocument userRoles = document.getArrayList(Project.ATTR_USERS, DataDocument.class).get(0);
      return userRoles.getArrayList(Project.ATTR_USER_ROLES, String.class);
   }

   /**
    * Reads users and theirs roles
    *
    * @param projectId
    *       Id of the project
    * @return map of users and theirs roles
    */
   public Map<String, List<String>> readUsersRoles(final String projectId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Project.COLLECTION_NAME, projectIdFilter(projectId), Collections.singletonList(Project.ATTR_USERS));
      List<DataDocument> users = document.getArrayList(Project.ATTR_USERS, DataDocument.class);
      return users != null ? users.stream().collect(Collectors.toMap(d -> d.getString(Project.ATTR_USER), d -> d.getArrayList(Project.ATTR_USER_ROLES, String.class))) : null;
   }

   /**
    * Check whether user has specific role
    *
    * @param projectId
    *       Id of the project
    * @param userName
    *       Name of the user
    * @param userRole
    *       Name of the user role
    * @return true if user has this role, false otherwise
    */
   public boolean hasUserRole(final String projectId, final String userName, final String userRole) {
      List<String> userRoles = readUserRoles(projectId, userName);
      if (userRoles == null) {
         return false;
      }
      if (userRoles.contains(userRole)) {
         return true;
      } else {
         Map<String, List<String>> roles = readRoles(projectId);
         for (String ur : userRoles) {
            if (roles.containsKey(ur) && roles.get(ur).contains(userRole)) {
               return true;
            }
         }
         return false;
      }
   }

   private String userFilter(String projectId, String userName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.ATTR_ORGANIZATION_ID, organisationFacade.getOrganisationId());
      filter.put(Project.ATTR_PROJECT_ID, projectId);
      filter.put(dataStorageDialect.concatFields(Project.ATTR_USERS, Project.ATTR_USER), userName);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private String userRoleFilter(String projectId, String userRole) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.UserRoles.ATTR_ORGANIZATION_ID, organisationFacade.getOrganisationId());
      filter.put(Project.UserRoles.ATTR_PROJECT_ID, projectId);
      filter.put(Project.UserRoles.ATTR_USER_ROLE, userRole);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private String projectIdFilter(String projectId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.ATTR_ORGANIZATION_ID, organisationFacade.getOrganisationId());
      filter.put(Project.ATTR_PROJECT_ID, projectId);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private String projectNameFilter(String projectName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Project.ATTR_ORGANIZATION_ID, organisationFacade.getOrganisationId());
      filter.put(Project.ATTR_PROJECT_NAME, projectName);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

}
