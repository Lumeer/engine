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
import io.lumeer.engine.api.dto.Organization;

import java.util.Collections;
import java.util.List;
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

   private String organizationCode = "ACME";
   private String organizationId = null;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private DatabaseInitializer databaseInitializer;

   /**
    * Gets unique and immutable id of the organization - _id from DataDocument
    *
    * @param organizationCode
    *       organization code
    * @return id
    */
   public String getOrganizationId(final String organizationCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode), Collections.singletonList(LumeerConst.Organization.ATTR_ORG_ID));
      return document != null ? document.getString(LumeerConst.Organization.ATTR_ORG_ID) : null;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public String getOrganizationCode() {
      return organizationCode;
   }

   public void setOrganizationCode(final String organizationCode) {
      this.organizationCode = organizationCode;
      this.organizationId = getOrganizationId(organizationCode);
   }

   /**
    * Reads  all organizations in the system.
    *
    * @return list of organizations
    */
   public List<Organization> readOrganizations() {
      return dataStorage.search(LumeerConst.Organization.COLLECTION_NAME, null, null, 0, 0)
                        .stream().map(Organization::new).collect(Collectors.toList());
   }

   /**
    * Reads the organization name according to its id.
    *
    * @param organizationCode
    *       code of the organization
    * @return name of the given organization
    */
   public String readOrganizationName(final String organizationCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode), Collections.singletonList(LumeerConst.Organization.ATTR_ORG_NAME));
      return document != null ? document.getString(LumeerConst.Organization.ATTR_ORG_NAME) : null;
   }

   /**
    * Reads the specific organization metadata.
    *
    * @param organizationCode
    *       code of the organization
    * @param metaName
    *       name of the meta attribute
    * @return meta attribute value
    */
   public String readOrganizationMetadata(final String organizationCode, final String metaName) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode), Collections.singletonList(metaName));
      return document != null ? document.getString(metaName) : null;
   }

   /**
    * Updates single organization metadata.
    *
    * @param organizationCode
    *       code of the organization
    * @param metaName
    *       name of the meta attribute to update
    * @param value
    *       meta attribute value
    */
   public void updateOrganizationMetadata(final String organizationCode, final String metaName, final String value) {
      updateOrganizationMetadata(organizationCode, new DataDocument(metaName, value));
   }

   /**
    * Updates multiple organization metadata.
    *
    * @param organizationCode
    *       code of the organization
    * @param meta
    *       key-value pairs of metadata to update
    */
   public void updateOrganizationMetadata(final String organizationCode, final DataDocument meta) {
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, meta, organizationCodeFilter(organizationCode));
   }

   /**
    * Removes the specific organization metadata.
    *
    * @param organizationCode
    *       code of the organization
    * @param metaName
    *       name of the meta attribute to remove
    */
   public void dropOrganizationMetadata(final String organizationCode, final String metaName) {
      dataStorage.dropAttribute(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode), metaName);
   }

   /**
    * Creates new organization in the system database.
    *
    * @param organizationCode
    *       code of the new organization to create
    * @param organizationName
    *       name of the new organization to create
    * @return id of the organization
    */
   public String createOrganization(final String organizationCode, final String organizationName) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_ORG_CODE, organizationCode)
            .append(LumeerConst.Organization.ATTR_ORG_NAME, organizationName)
            .append(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());
      String id = dataStorage.createDocument(LumeerConst.Organization.COLLECTION_NAME, document);
      databaseInitializer.onOrganizationCreated(id);
      return id;
   }

   /**
    * Changes organization id
    *
    * @param oldOrganizationCode
    *       code of the organization to change
    * @param newOrganizationCode
    *       new code for organization
    */
   public void updateOrganizationCode(final String oldOrganizationCode, final String newOrganizationCode) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_ORG_CODE, newOrganizationCode);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, document, organizationCodeFilter(oldOrganizationCode));
   }

   /**
    * Renames a name of the given organization according to its id.
    *
    * @param organizationCode
    *       code of the given organization
    * @param newOrganizationName
    *       new name of the organization
    */
   public void renameOrganization(final String organizationCode, final String newOrganizationName) {
      DataDocument dataDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_NAME, newOrganizationName);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, dataDocument, organizationCodeFilter(organizationCode));
   }

   /**
    * Drops the organization according to its id.
    *
    * @param organizationCode
    *       code of the given organization to drop
    */
   public void dropOrganization(final String organizationCode) {
      String organizationIdentifier = getOrganizationId(organizationCode);
      if (organizationIdentifier != null) {
         dataStorage.dropDocument(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode));
         databaseInitializer.onOrganizationRemoved(organizationIdentifier);
      }
   }

   /**
    * Reads additional information about the given organization.
    *
    * @param organizationCode
    *       code of the organization
    * @return DataDocument object including additional organization info
    */
   public DataDocument readOrganizationInfoData(final String organizationCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode), Collections.singletonList(LumeerConst.Organization.ATTR_ORG_DATA));
      return document != null ? document.getDataDocument(LumeerConst.Organization.ATTR_ORG_DATA) : null;
   }

   /**
    * Reads specified additional information about the given organization.
    *
    * @param organizationCode
    *       code of the organization
    * @param infoDataAttribute
    *       attribute of the organization additional info
    * @return value of the organization info attribute
    */
   public String readOrganizationInfoData(final String organizationCode, final String infoDataAttribute) {
      DataDocument infoDataDocument = readOrganizationInfoData(organizationCode);
      return infoDataDocument != null ? infoDataDocument.getString(infoDataAttribute) : null;
   }

   /**
    * Updates whole organization info document.
    *
    * @param organizationCode
    *       code of the organization
    * @param infoData
    *       info document including more attributes and values
    */
   public void updateOrganizationInfoData(final String organizationCode, final DataDocument infoData) {
      DataDocument infoDataDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_DATA, infoData);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, infoDataDocument, organizationCodeFilter(organizationCode));
   }

   /**
    * Updates specified attribute in the organization info document.
    *
    * @param organizationCode
    *       code of the organization
    * @param infoAttribute
    *       attribute of the organization additional info to update
    * @param value
    *       new value of the given attribute
    */
   public void updateOrganizationInfoData(final String organizationCode, final String infoAttribute, final String value) {
      updateOrganizationInfoData(organizationCode, new DataDocument(infoAttribute, value));
   }

   /**
    * Drops the given attribute in the organization info document.
    *
    * @param organizationCode
    *       code of the organization
    * @param infoAttribute
    *       attribute of the organization additional info to drop
    */
   public void dropOrganizationInfoDataAttribute(final String organizationCode, final String infoAttribute) {
      dataStorage.dropAttribute(LumeerConst.Organization.COLLECTION_NAME, organizationCodeFilter(organizationCode), dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_ORG_DATA, infoAttribute));
   }

   /**
    * Resets additional info of the given organization (drops everything).
    *
    * @param organizationCode
    *       code of the organization
    */
   public void resetOrganizationInfoData(final String organizationCode) {
      DataDocument defaultInfoDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, defaultInfoDocument, organizationCodeFilter(organizationCode));
   }

   protected DataFilter organizationCodeFilter(final String organizationCode) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Organization.ATTR_ORG_CODE, organizationCode);
   }

}
