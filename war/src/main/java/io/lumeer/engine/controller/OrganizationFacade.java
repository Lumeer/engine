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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Manages organisations and keeps track of currently selected organisation.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RequestScoped
public class OrganisationFacade {

   private static String organisationId = "ACME";

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private DatabaseInitializer databaseInitializer;

   public String readOrganisationId() {
      return organisationId;
   }

   public void setOrganisationId(final String organisationId) {
      this.organisationId = organisationId;
   }

   /**
    * Reads a map of all organisations in the system.
    *
    * @return map of values (id, name) of all organisations in the system
    */
   public Map<String, String> readOrganisationsMap() {
      List<DataDocument> documents = dataStorage.searchIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, null, Arrays.asList(LumeerConst.Organisation.ATTR_ORG_NAME, LumeerConst.Organisation.ATTR_ORG_ID));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Organisation.ATTR_ORG_ID), d -> d.getString(LumeerConst.Organisation.ATTR_ORG_NAME)));
   }

   /**
    * Reads the organisation id according to its name.
    *
    * @param organisationName
    *       name of the organisation
    * @return id of the given organisation
    */
   public String readOrganisationId(final String organisationName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationNameFilter(organisationName), Collections.singletonList(LumeerConst.Organisation.ATTR_ORG_ID));
      return document != null ? document.getString(LumeerConst.Organisation.ATTR_ORG_ID) : null;
   }

   /**
    * Reads the organisation name according to its id.
    *
    * @param organisationId
    *       id of the organisation
    * @return name of the given organisation
    */
   public String readOrganisationName(final String organisationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), Collections.singletonList(LumeerConst.Organisation.ATTR_ORG_NAME));
      return document != null ? document.getString(LumeerConst.Organisation.ATTR_ORG_NAME) : null;
   }

   /**
    * Reads the specific organisation metadata.
    *
    * @param organisationId
    *       id of the organisation
    * @param metaName
    *       name of the meta attribute
    * @return meta attribute value
    */
   public String readOrganisationMetadata(final String organisationId, final String metaName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), Collections.singletonList(metaName));
      return document != null ? document.getString(metaName) : null;
   }

   /**
    * Updates single organisation metadata.
    *
    * @param organisationId
    *       id of the organisation
    * @param metaName
    *       name of the meta attribute to update
    * @param value
    *       meta attribute value
    */
   public void updateOrganisationMetadata(final String organisationId, final String metaName, final String value) {
      updateOrganisationMetadata(organisationId, new DataDocument(metaName, value));
   }

   /**
    * Updates multiple organisation metadata.
    *
    * @param organisationId
    *       id of the organisation
    * @param meta
    *       key-value pairs of metadata to update
    */
   public void updateOrganisationMetadata(final String organisationId, final DataDocument meta) {
      dataStorage.updateDocument(LumeerConst.Organisation.COLLECTION_NAME, meta, organisationIdFilter(organisationId));
   }

   /**
    * Removes the specific organisation metadata.
    *
    * @param organisationId
    *       id of the organisation
    * @param metaName
    *       name of the meta attribute to remove
    */
   public void dropOrganisationMetadata(final String organisationId, final String metaName) {
      dataStorage.dropAttribute(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), metaName);
   }

   /**
    * Creates new organisation in the system database.
    *
    * @param organisationId
    *       id of the new organisation to create
    * @param organisationName
    *       name of the new organisation to create
    */
   public void createOrganisation(final String organisationId, final String organisationName) {
      DataDocument document = new DataDocument(LumeerConst.Organisation.ATTR_ORG_ID, organisationId)
            .append(LumeerConst.Organisation.ATTR_ORG_NAME, organisationName)
            .append(LumeerConst.Organisation.ATTR_ORG_DATA, new DataDocument())
            .append(LumeerConst.Organisation.ATTR_USERS, new DataDocument());
      dataStorage.createDocument(LumeerConst.Organisation.COLLECTION_NAME, document);
   }

   /**
    * Renames a name of the given organisation according to its id.
    *
    * @param organisationId
    *       id of the given organisation
    * @param newOrganisationName
    *       new name of the organisation
    */
   public void renameOrganisation(final String organisationId, final String newOrganisationName) {
      DataDocument dataDocument = new DataDocument(LumeerConst.Organisation.ATTR_ORG_NAME, newOrganisationName);
      dataStorage.updateDocument(LumeerConst.Organisation.COLLECTION_NAME, dataDocument, organisationIdFilter(organisationId));
   }

   /**
    * Drops the organisation according to its id.
    *
    * @param organisationId
    *       id of the given organisation to drop
    */
   public void dropOrganisation(final String organisationId) {
      dataStorage.dropDocument(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId));
   }

   /**
    * Reads additional information about the given organisation.
    *
    * @param organisationId
    *       id of the organisation
    * @return DataDocument object including additional organisation info
    */
   public DataDocument readOrganisationInfoData(final String organisationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), Collections.singletonList(LumeerConst.Organisation.ATTR_ORG_DATA));
      return document != null ? document.getDataDocument(LumeerConst.Organisation.ATTR_ORG_DATA) : null;
   }

   /**
    * Reads specified additional information about the given organisation.
    *
    * @param organisationId
    *       id of the organisation
    * @param infoDataAttribute
    *       attribute of the organisation additional info
    * @return value of the organisation info attribute
    */
   public String readOrganisationInfoData(final String organisationId, final String infoDataAttribute) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), Collections.singletonList(LumeerConst.Organisation.ATTR_ORG_DATA));
      DataDocument infoDataDocument = document.getDataDocument(LumeerConst.Organisation.ATTR_ORG_DATA);
      return infoDataDocument != null ? infoDataDocument.getString(infoDataAttribute) : null;
   }

   /**
    * Updates whole organisation info document.
    *
    * @param organisationId
    *       id of the organisation
    * @param infoData
    *       info document including more attributes and values
    */
   public void updateOrganisationInfoData(final String organisationId, final DataDocument infoData) {
      DataDocument infoDataDocument = new DataDocument(LumeerConst.Organisation.ATTR_ORG_DATA, infoData);
      dataStorage.updateDocument(LumeerConst.Organisation.COLLECTION_NAME, infoDataDocument, organisationIdFilter(organisationId));
   }

   /**
    * Updates specified attribute in the organisation info document.
    *
    * @param organisationId
    *       id of the organisation
    * @param infoAttribute
    *       attribute of the organisation additional info to update
    * @param value
    *       new value of the given attribute
    */
   public void updateOrganisationInfoData(final String organisationId, final String infoAttribute, final String value) {
      updateOrganisationInfoData(organisationId, new DataDocument(infoAttribute, value));
   }

   /**
    * Drops the given attribute in the organisation info document.
    *
    * @param organisationId
    *       id of the organisation
    * @param infoAttribute
    *       attribute of the organisation additional info to drop
    */
   public void dropOrganisationInfoDataAttribute(final String organisationId, final String infoAttribute) {
      DataDocument infoDocument = readOrganisationInfoData(organisationId);
      infoDocument.remove(infoAttribute);
      DataDocument document = new DataDocument(LumeerConst.Organisation.ATTR_ORG_DATA, infoDocument);
      dataStorage.updateDocument(LumeerConst.Organisation.COLLECTION_NAME, document, organisationIdFilter(organisationId));
   }

   /**
    * Resets additional info of the given organisation.
    *
    * @param organisationId
    *       id of the organisation
    */
   public void resetOrganisationInfoData(final String organisationId) {
      DataDocument defaultInfoDocument = new DataDocument(LumeerConst.Organisation.ATTR_ORG_DATA, new DataDocument());
      dataStorage.updateDocument(LumeerConst.Organisation.COLLECTION_NAME, defaultInfoDocument, organisationIdFilter(organisationId));
   }

   /**
    * Reads users and their roles in the given organisation.
    *
    * @param organisationId
    *       id of the organisation
    * @return map of users and theirs roles
    */
   public Map<String, List<String>> readOrganisationUsers(final String organisationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), Collections.singletonList(LumeerConst.Organisation.ATTR_USERS));
      List<DataDocument> users = document.getArrayList(LumeerConst.Organisation.ATTR_USERS, DataDocument.class);
      return users != null ? users.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Organisation.ATTR_USERS_USERNAME), d -> d.getArrayList(LumeerConst.Organisation.ATTR_USERS_USER_ROLES, String.class))) : null;
   }

   public Map<String, List<String>> readUserOrganisations(final String userName) {
      // TODO:
      return null;
   }

   /**
    * Adds user to organisation with specific user roles.
    *
    * @param organisationId
    *       id of the organisation
    * @param userName
    *       name of the user to add
    * @param userRoles
    *       organisation roles of the given user
    */
   public void addUserToOrganisation(final String organisationId, final String userName, final List<String> userRoles) {
      DataDocument document = new DataDocument(LumeerConst.Organisation.ATTR_USERS_USERNAME, userName)
            .append(LumeerConst.Organisation.ATTR_USERS_USER_ROLES, userRoles);
      dataStorage.addItemToArray(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), LumeerConst.Organisation.ATTR_USERS, document);
   }

   /**
    * Adds user to organisation with default roles.
    *
    * @param organisationId
    *       id of the organisation
    * @param userName
    *       name of the user to add
    */
   public void addUserToOrganisation(final String organisationId, final String userName) {
      addUserToOrganisation(organisationId, userName, readDefaultRoles(organisationId));
   }

   public void removeUserFromOrganisation(final String organisationId, final String userName) {
      // TODO:
   }

   /**
    * Adds roles to the given user in the organisation.
    *
    * @param organisationId
    *       id of the organisation
    * @param userName
    *       name of the user
    * @param userRoles
    *       roles to set
    */
   public void addRolesToUser(final String organisationId, final String userName, final List<String> userRoles) {
      dataStorage.addItemsToArray(LumeerConst.Organisation.COLLECTION_NAME, userFilter(organisationId, userName), dataStorageDialect.concatFields(LumeerConst.Organisation.ATTR_USERS, "$", LumeerConst.Organisation.ATTR_USERS_USER_ROLES), userRoles);
   }

   /**
    * Reads user roles of the given user.
    *
    * @param organisationId
    *       id of the organisation
    * @param userName
    *       name of the given user
    * @return list of roles
    */
   public List<String> readUserRoles(final String organisationId, final String userName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, userFilter(organisationId, userName), Collections.singletonList(dataStorageDialect.concatFields(LumeerConst.Organisation.ATTR_USERS, "$")));
      if (document == null) {
         return null;
      }
      DataDocument userRoles = document.getArrayList(LumeerConst.Organisation.ATTR_USERS, DataDocument.class).get(0);
      return userRoles.getArrayList(LumeerConst.Organisation.ATTR_USERS_USER_ROLES, String.class);
   }

   /**
    * Removes roles of the given user in the organisation.
    *
    * @param organisationId
    *       id of the organisation
    * @param userName
    *       name of the user
    * @param userRoles
    *       roles to remove
    */
   public void removeRolesFromUser(final String organisationId, final String userName, final List<String> userRoles) {
      dataStorage.removeItemsFromArray(LumeerConst.Organisation.COLLECTION_NAME, userFilter(organisationId, userName), dataStorageDialect.concatFields(LumeerConst.Organisation.ATTR_USERS, "$", LumeerConst.Organisation.ATTR_USERS_USER_ROLES), userRoles);
   }

   /**
    * Sets default roles for newly added users of the given organisation.
    *
    * @param organisationId
    *       id of the organisation
    * @param userRoles
    *       list of user roles to set
    */
   public void setDefaultRoles(final String organisationId, final List<String> userRoles) {
      DataDocument document = new DataDocument(LumeerConst.Organisation.ATTR_META_DEFAULT_ROLES, userRoles);
      dataStorage.updateDocument(LumeerConst.Organisation.COLLECTION_NAME, document, organisationIdFilter(organisationId));
   }

   /**
    * Reads a list of default roles for newly added users.
    *
    * @param organisationId
    *       id of the organisation
    * @return list of default user roles
    */
   public List<String> readDefaultRoles(final String organisationId) {
      DataDocument defaultRoles = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organisation.COLLECTION_NAME, organisationIdFilter(organisationId), Collections.singletonList(LumeerConst.Organisation.ATTR_META_DEFAULT_ROLES));
      return defaultRoles != null ? defaultRoles.getArrayList(LumeerConst.Organisation.ATTR_META_DEFAULT_ROLES, String.class) : null;
   }

   private String organisationNameFilter(final String organisationName) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Organisation.ATTR_ORG_NAME, organisationName);
   }

   private String organisationIdFilter(final String organisationId) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Organisation.ATTR_ORG_ID, organisationId);
   }

   private String userFilter(final String organisationId, final String userName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Organisation.ATTR_ORG_ID, organisationId);
      filter.put(dataStorageDialect.concatFields(LumeerConst.Organisation.ATTR_USERS, LumeerConst.Organisation.ATTR_USERS_USERNAME), userName);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }
}
