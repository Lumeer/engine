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
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Manages organizations and keeps track of currently selected organization.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RequestScoped
public class OrganizationFacade {

   private String organizationId = "ACME";

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private UserRoleFacade userRoleFacade;

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   /**
    * Reads a map of all organizations in the system.
    *
    * @return map of values (id, name) of all organizations in the system
    */
   public Map<String, String> readOrganizationsMap() {
      List<DataDocument> documents = dataStorage.searchIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, null, Arrays.asList(LumeerConst.Organization.ATTR_ORG_NAME, LumeerConst.Organization.ATTR_ORG_ID));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Organization.ATTR_ORG_ID), d -> d.getString(LumeerConst.Organization.ATTR_ORG_NAME)));
   }

   /**
    * Reads the organization id according to its name.
    *
    * @param organizationName
    *       name of the organization
    * @return id of the given organization
    */
   public String readOrganizationId(final String organizationName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationNameFilter(organizationName), Collections.singletonList(LumeerConst.Organization.ATTR_ORG_ID));
      return document != null ? document.getString(LumeerConst.Organization.ATTR_ORG_ID) : null;
   }

   /**
    * Reads the organization name according to its id.
    *
    * @param organizationId
    *       id of the organization
    * @return name of the given organization
    */
   public String readOrganizationName(final String organizationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), Collections.singletonList(LumeerConst.Organization.ATTR_ORG_NAME));
      return document != null ? document.getString(LumeerConst.Organization.ATTR_ORG_NAME) : null;
   }

   /**
    * Reads the specific organization metadata.
    *
    * @param organizationId
    *       id of the organization
    * @param metaName
    *       name of the meta attribute
    * @return meta attribute value
    */
   public String readOrganizationMetadata(final String organizationId, final String metaName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), Collections.singletonList(metaName));
      return document != null ? document.getString(metaName) : null;
   }

   /**
    * Updates single organization metadata.
    *
    * @param organizationId
    *       id of the organization
    * @param metaName
    *       name of the meta attribute to update
    * @param value
    *       meta attribute value
    */
   public void updateOrganizationMetadata(final String organizationId, final String metaName, final String value) {
      updateOrganizationMetadata(organizationId, new DataDocument(metaName, value));
   }

   /**
    * Updates multiple organization metadata.
    *
    * @param organizationId
    *       id of the organization
    * @param meta
    *       key-value pairs of metadata to update
    */
   public void updateOrganizationMetadata(final String organizationId, final DataDocument meta) {
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, meta, organizationIdFilter(organizationId));
   }

   /**
    * Removes the specific organization metadata.
    *
    * @param organizationId
    *       id of the organization
    * @param metaName
    *       name of the meta attribute to remove
    */
   public void dropOrganizationMetadata(final String organizationId, final String metaName) {
      dataStorage.dropAttribute(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), metaName);
   }

   /**
    * Creates new organization in the system database.
    *
    * @param organizationId
    *       id of the new organization to create
    * @param organizationName
    *       name of the new organization to create
    */
   public void createOrganization(final String organizationId, final String organizationName) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_ORG_ID, organizationId)
            .append(LumeerConst.Organization.ATTR_ORG_NAME, organizationName)
            .append(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument())
            .append(LumeerConst.Organization.ATTR_USERS, new DataDocument());
      dataStorage.createDocument(LumeerConst.Organization.COLLECTION_NAME, document);
   }

   /**
    * Renames a name of the given organization according to its id.
    *
    * @param organizationId
    *       id of the given organization
    * @param newOrganizationName
    *       new name of the organization
    */
   public void renameOrganization(final String organizationId, final String newOrganizationName) {
      DataDocument dataDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_NAME, newOrganizationName);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, dataDocument, organizationIdFilter(organizationId));
   }

   /**
    * Drops the organization according to its id.
    *
    * @param organizationId
    *       id of the given organization to drop
    */
   public void dropOrganization(final String organizationId) {
      dataStorage.dropDocument(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId));
      // TODO: check cannot be implemented without other facades changes
   }

   /**
    * Reads additional information about the given organization.
    *
    * @param organizationId
    *       id of the organization
    * @return DataDocument object including additional organization info
    */
   public DataDocument readOrganizationInfoData(final String organizationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), Collections.singletonList(LumeerConst.Organization.ATTR_ORG_DATA));
      return document != null ? document.getDataDocument(LumeerConst.Organization.ATTR_ORG_DATA) : null;
   }

   /**
    * Reads specified additional information about the given organization.
    *
    * @param organizationId
    *       id of the organization
    * @param infoDataAttribute
    *       attribute of the organization additional info
    * @return value of the organization info attribute
    */
   public String readOrganizationInfoData(final String organizationId, final String infoDataAttribute) {
      DataDocument infoDataDocument = readOrganizationInfoData(organizationId);
      return infoDataDocument != null ? infoDataDocument.getString(infoDataAttribute) : null;
   }

   /**
    * Updates whole organization info document.
    *
    * @param organizationId
    *       id of the organization
    * @param infoData
    *       info document including more attributes and values
    */
   public void updateOrganizationInfoData(final String organizationId, final DataDocument infoData) {
      DataDocument infoDataDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_DATA, infoData);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, infoDataDocument, organizationIdFilter(organizationId));
   }

   /**
    * Updates specified attribute in the organization info document.
    *
    * @param organizationId
    *       id of the organization
    * @param infoAttribute
    *       attribute of the organization additional info to update
    * @param value
    *       new value of the given attribute
    */
   public void updateOrganizationInfoData(final String organizationId, final String infoAttribute, final String value) {
      updateOrganizationInfoData(organizationId, new DataDocument(infoAttribute, value));
   }

   /**
    * Drops the given attribute in the organization info document.
    *
    * @param organizationId
    *       id of the organization
    * @param infoAttribute
    *       attribute of the organization additional info to drop
    */
   public void dropOrganizationInfoDataAttribute(final String organizationId, final String infoAttribute) {
      dataStorage.dropAttribute(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_ORG_DATA, infoAttribute));
   }

   /**
    * Resets additional info of the given organization.
    *
    * @param organizationId
    *       id of the organization
    */
   public void resetOrganizationInfoData(final String organizationId) {
      DataDocument defaultInfoDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, defaultInfoDocument, organizationIdFilter(organizationId));
   }

   /**
    * Reads users and their roles in the given organization.
    *
    * @param organizationId
    *       id of the organization
    * @return map of users and theirs roles
    */
   public Map<String, List<String>> readOrganizationUsers(final String organizationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), Collections.singletonList(LumeerConst.Organization.ATTR_USERS));
      List<DataDocument> users = document.getArrayList(LumeerConst.Organization.ATTR_USERS, DataDocument.class);
      return users != null ? users.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Organization.ATTR_USERS_USERNAME), d -> d.getArrayList(LumeerConst.Organization.ATTR_USERS_USER_ROLES, String.class))) : null;
   }

   /**
    * Reads a list of organizations the user is assigned to.
    *
    * @param userName
    *       name of the user
    * @return list of organizations
    */
   public Map<String, String> readUserOrganizations(final String userName) {
      List<DataDocument> documents = dataStorage.searchIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationFilterByUser(userName), Arrays.asList(LumeerConst.Organization.ATTR_ORG_ID, LumeerConst.Organization.ATTR_ORG_NAME));
      return documents != null ? documents.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Organization.ATTR_ORG_ID), d -> d.getString(LumeerConst.Organization.ATTR_ORG_NAME))) : null;
   }

   /**
    * Adds user to organization with specific user roles.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the user to add
    * @param userRoles
    *       organization roles of the given user
    */
   public void addUserToOrganization(final String organizationId, final String userName, final List<String> userRoles) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_USERS_USERNAME, userName)
            .append(LumeerConst.Organization.ATTR_USERS_USER_ROLES, userRoles);
      dataStorage.addItemToArray(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), LumeerConst.Organization.ATTR_USERS, document);
   }

   /**
    * Adds user to organization with default roles.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the user to add
    */
   public void addUserToOrganization(final String organizationId, final String userName) {
      addUserToOrganization(organizationId, userName, readDefaultRoles(organizationId));
   }

   /**
    * Removes the given user from the organization.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the user to remove from organization
    */
   public void removeUserFromOrganization(final String organizationId, final String userName) {
      dataStorage.removeItemFromArray(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), LumeerConst.Organization.ATTR_USERS, new DataDocument(LumeerConst.Organization.ATTR_USERS_USERNAME, userName));
   }

   /**
    * Adds roles to the given user in the organization.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the user
    * @param userRoles
    *       roles to set
    */
   public void addRolesToUser(final String organizationId, final String userName, final List<String> userRoles) {
      dataStorage.addItemsToArray(LumeerConst.Organization.COLLECTION_NAME, userFilter(organizationId, userName), dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_USERS, "$", LumeerConst.Organization.ATTR_USERS_USER_ROLES), userRoles);
   }

   /**
    * Reads user roles of the given user.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the given user
    * @return list of roles
    */
   public List<String> readUserRoles(final String organizationId, final String userName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, userFilter(organizationId, userName), Collections.singletonList(dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_USERS, "$")));
      if (document == null) {
         return null;
      }
      DataDocument userRoles = document.getArrayList(LumeerConst.Organization.ATTR_USERS, DataDocument.class).get(0);
      return userRoles.getArrayList(LumeerConst.Organization.ATTR_USERS_USER_ROLES, String.class);
   }

   /**
    * Removes roles of the given user in the organization.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the user
    * @param userRoles
    *       roles to remove
    */
   public void removeRolesFromUser(final String organizationId, final String userName, final List<String> userRoles) {
      dataStorage.removeItemsFromArray(LumeerConst.Organization.COLLECTION_NAME, userFilter(organizationId, userName), dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_USERS, "$", LumeerConst.Organization.ATTR_USERS_USER_ROLES), userRoles);
   }

   /**
    * Sets default roles for newly added users of the given organization.
    *
    * @param organizationId
    *       id of the organization
    * @param userRoles
    *       list of user roles to set
    */
   public void setDefaultRoles(final String organizationId, final List<String> userRoles) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_META_DEFAULT_ROLES, userRoles);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, document, organizationIdFilter(organizationId));
   }

   /**
    * Reads a list of default roles for newly added users.
    *
    * @param organizationId
    *       id of the organization
    * @return list of default user roles
    */
   public List<String> readDefaultRoles(final String organizationId) {
      DataDocument defaultRoles = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), Collections.singletonList(LumeerConst.Organization.ATTR_META_DEFAULT_ROLES));
      return defaultRoles != null ? defaultRoles.getArrayList(LumeerConst.Organization.ATTR_META_DEFAULT_ROLES, String.class) : null;
   }

   /**
    * Checks whether the given user has specific role in the organization.
    *
    * @param organizationId
    *       id of the organization
    * @param userName
    *       name of the user
    * @param userRole
    *       name of the specific user role
    * @return true if the role is assigned to the given user, false otherwise
    */
   public boolean hasUserRoleInOrganization(final String organizationId, final String userName, final String userRole) {
      List<String> userRoles = readUserRoles(organizationId, userName);
      if (userRoles == null) {
         return false;
      }
      if (userRoles.contains(userRole)) {
         return true;
      } else {
         Map<String, List<String>> roles = userRoleFacade.readRoles(this.getOrganizationId());
         for (String ur : userRoles) {
            if (roles.containsKey(ur) && roles.get(ur).contains(userRole)) {
               return true;
            }
         }
         return false;
      }
   }

   private DataFilter organizationNameFilter(final String organizationName) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Organization.ATTR_ORG_NAME, organizationName);
   }

   private DataFilter organizationIdFilter(final String organizationId) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Organization.ATTR_ORG_ID, organizationId);
   }

   private DataFilter userFilter(final String organizationId, final String userName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Organization.ATTR_ORG_ID, organizationId);
      filter.put(dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_USERS, LumeerConst.Organization.ATTR_USERS_USERNAME), userName);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter organizationFilterByUser(final String userName) {
      return dataStorageDialect.fieldValueFilter(dataStorageDialect.concatFields(LumeerConst.Project.ATTR_USERS, LumeerConst.Project.ATTR_USERS_USERNAME), userName);
   }
}
