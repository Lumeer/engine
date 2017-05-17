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
   private DatabaseInitializer databaseInitializer;

   /**
    * Gets unique and immutable identificator of the organization - _id from DataDocument
    *
    * @param organizationId
    *       organization id
    * @return identificator
    */
   public String getOrganizationIdentificator(final String organizationId) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId), Collections.singletonList(LumeerConst.Document.ID));
      return document != null ? document.getString(LumeerConst.Document.ID) : null;
   }

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
      List<DataDocument> documents = dataStorage.search(LumeerConst.Organization.COLLECTION_NAME, null, Arrays.asList(LumeerConst.Organization.ATTR_ORG_NAME, LumeerConst.Organization.ATTR_ORG_ID));
      return documents.stream().collect(Collectors.toMap(d -> d.getString(LumeerConst.Organization.ATTR_ORG_ID), d -> d.getString(LumeerConst.Organization.ATTR_ORG_NAME)));
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
   public String createOrganization(final String organizationId, final String organizationName) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_ORG_ID, organizationId)
            .append(LumeerConst.Organization.ATTR_ORG_NAME, organizationName)
            .append(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());
      String id = dataStorage.createDocument(LumeerConst.Organization.COLLECTION_NAME, document);
      databaseInitializer.onOrganizationCreated(id);
      return id;
   }

   /**
    * Changes organization id
    *
    * @param oldOrganizationId
    *       id of the organization to change
    * @param newOrganizationId
    *       new id for organization
    */
   public void updateOrganizationId(final String oldOrganizationId, final String newOrganizationId) {
      DataDocument document = new DataDocument(LumeerConst.Organization.ATTR_ORG_ID, newOrganizationId);
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, document, organizationIdFilter(oldOrganizationId));
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
      String organizationIdentifier =  getOrganizationIdentificator(organizationId);
      if (organizationIdentifier != null) {
         dataStorage.dropDocument(LumeerConst.Organization.COLLECTION_NAME, organizationIdFilter(organizationId));
         databaseInitializer.onOrganizationRemoved(organizationIdentifier);
      }
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
    * Resets additional info of the given organization (drops everything).
    *
    * @param organizationId
    *       id of the organization
    */
   public void resetOrganizationInfoData(final String organizationId) {
      DataDocument defaultInfoDocument = new DataDocument(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());
      dataStorage.updateDocument(LumeerConst.Organization.COLLECTION_NAME, defaultInfoDocument, organizationIdFilter(organizationId));
   }

   private DataFilter organizationIdFilter(final String organizationId) {
      return dataStorageDialect.fieldValueFilter(LumeerConst.Organization.ATTR_ORG_ID, organizationId);
   }

}
