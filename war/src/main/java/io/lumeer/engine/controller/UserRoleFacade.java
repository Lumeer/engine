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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Manipulates with user roles used in projects and organizations
 */
public class UserRoleFacade {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private DatabaseInitializer databaseInitializer;

   /**
    * Create new user role which consists of core roles in project
    *
    * @param organizationId
    *       Id of the organization
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Unique name of user role
    * @param coreRoles
    *       List of the core roles to set
    */
   public void createRole(final String organizationId, final String projectId, final String userRole, final List<String> coreRoles) {
      DataDocument document = new DataDocument(LumeerConst.Project.UserRoles.ATTR_PROJECT_ID, projectId)
            .append(LumeerConst.Project.UserRoles.ATTR_ORGANIZATION_ID, organizationId)
            .append(LumeerConst.Project.UserRoles.ATTR_USER_ROLE, userRole)
            .append(LumeerConst.Project.UserRoles.ATTR_CORE_ROLES, coreRoles);
      dataStorage.createDocument(LumeerConst.Project.UserRoles.COLLECTION_NAME, document);
   }

   /**
    * Create new user role which consists of core roles in organization
    *
    * @param organizationId
    *       Id of the organization
    * @param userRole
    *       Unique name of user role
    * @param coreRoles
    *       List of the core roles to set
    */

   public void createRole(final String organizationId, final String userRole, final List<String> coreRoles) {
      createRole(organizationId, null, userRole, coreRoles);
   }

   /**
    * Adds core roles to user role in project
    *
    * @param organizationId
    *       Id of the organization
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Name of user role
    * @param coreRoles
    *       List of the core roles to add
    */
   public void addCoreRolesToRole(final String organizationId, final String projectId, final String userRole, final List<String> coreRoles) {
      dataStorage.addItemsToArray(LumeerConst.Project.UserRoles.COLLECTION_NAME, userRoleFilter(organizationId, projectId, userRole), LumeerConst.Project.UserRoles.ATTR_CORE_ROLES, coreRoles);
   }

   /**
    * Adds core roles to user role in organization
    *
    * @param organizationId
    *       Id of the organization
    * @param userRole
    *       Name of user role
    * @param coreRoles
    *       List of the core roles to add
    */
   public void addCoreRolesToRole(final String organizationId, final String userRole, final List<String> coreRoles) {
      addCoreRolesToRole(organizationId, null, userRole, coreRoles);
   }

   /**
    * Removes core role from user role in project
    *
    * @param organizationId
    *       Id of the organization
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Name of user role
    * @param coreRoles
    *       List of the core roles to remove
    */
   public void removeCoreRolesFromRole(final String organizationId, final String projectId, final String userRole, final List<String> coreRoles) {
      dataStorage.removeItemsFromArray(LumeerConst.Project.UserRoles.COLLECTION_NAME, userRoleFilter(organizationId, projectId, userRole), LumeerConst.Project.UserRoles.ATTR_CORE_ROLES, coreRoles);
   }

   /**
    * Removes core role from user role in organization
    *
    * @param organizationId
    *       Id of the organization
    * @param userRole
    *       Name of user role
    * @param coreRoles
    *       List of the core roles to remove
    */
   public void removeCoreRolesFromRole(final String organizationId, final String userRole, final List<String> coreRoles) {
      removeCoreRolesFromRole(organizationId, null, userRole, coreRoles);
   }

   /**
    * Removes user role from project
    *
    * @param organizationId
    *       Id of the organization
    * @param projectId
    *       Id of the project
    * @param userRole
    *       Name of user role to delete
    */
   public void dropRole(final String organizationId, final String projectId, final String userRole) {
      dataStorage.dropDocument(LumeerConst.Project.UserRoles.COLLECTION_NAME, userRoleFilter(organizationId, projectId, userRole));
   }

   /**
    * Removes user role from organization
    *
    * @param organizationId
    *       Id of the organization
    * @param userRole
    *       Name of user role to delete
    */
   public void dropRole(final String organizationId, final String userRole) {
      dropRole(organizationId, null, userRole);
   }

   /**
    * Reads project user roles
    *
    * @param organizationId
    *       Id of the organization
    * @param projectId
    *       Id of the project
    * @return map of roles and their core roles
    */
   public Map<String, List<String>> readRoles(final String organizationId, final String projectId) {
      List<DataDocument> documents = dataStorage.searchIncludeAttrs(LumeerConst.Project.UserRoles.COLLECTION_NAME, projectIdFilter(organizationId, projectId), Arrays.asList(LumeerConst.Project.UserRoles.ATTR_USER_ROLE, LumeerConst.Project.UserRoles.ATTR_CORE_ROLES));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Project.UserRoles.ATTR_USER_ROLE), d -> d.getArrayList(LumeerConst.Project.UserRoles.ATTR_CORE_ROLES, String.class)));
   }

   /**
    * Reads organization user roles
    *
    * @param organizationId
    *       Id of the organization
    * @return map of roles and their core roles
    */
   public Map<String, List<String>> readRoles(final String organizationId) {
      return readRoles(organizationId, null);
   }

   private String projectIdFilter(final String organizationId, final String projectId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Project.ATTR_ORGANIZATION_ID, organizationId);
      filter.put(LumeerConst.Project.ATTR_PROJECT_ID, projectId);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private String userRoleFilter(final String organizationId, final String projectId, final String userRole) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Project.UserRoles.ATTR_ORGANIZATION_ID, organizationId);
      filter.put(LumeerConst.Project.UserRoles.ATTR_PROJECT_ID, projectId);
      filter.put(LumeerConst.Project.UserRoles.ATTR_USER_ROLE, userRole);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

}
