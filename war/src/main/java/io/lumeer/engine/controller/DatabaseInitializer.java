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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.LumeerConst.Configuration;
import io.lumeer.engine.api.LumeerConst.Group;
import io.lumeer.engine.api.LumeerConst.Index;
import io.lumeer.engine.api.LumeerConst.Organization;
import io.lumeer.engine.api.LumeerConst.Project;
import io.lumeer.engine.api.LumeerConst.Security;
import io.lumeer.engine.api.LumeerConst.UserGroup;
import io.lumeer.engine.api.LumeerConst.UserSettings;
import io.lumeer.engine.api.LumeerConst.View;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import java.util.Collections;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class DatabaseInitializer {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   @UserDataStorage
   private DataStorage userDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   public void init(@Observes @Initialized(RequestScoped.class) Object init) {

   }

   @PostConstruct
   public void init() {
      initProjectCollection();
      initOrganizationCollection();
      initOrganizationRolesCollection();
      initUserGroupCollection();
      initUserSettingsCollection();
      initConfigurationCollections();
   }

   private void initConfigurationCollections() {
      if (!dataStorage.hasCollection(ConfigurationFacade.USER_CONFIG_COLLECTION)) {
         dataStorage.createCollection(ConfigurationFacade.USER_CONFIG_COLLECTION);
         dataStorage.createIndex(ConfigurationFacade.USER_CONFIG_COLLECTION, new DataDocument(Configuration.NAMEVALUE, Index.ASCENDING), true);
      }
      if (!dataStorage.hasCollection(ConfigurationFacade.PROJECT_CONFIG_COLLECTION)) {
         dataStorage.createCollection(ConfigurationFacade.PROJECT_CONFIG_COLLECTION);
         dataStorage.createIndex(ConfigurationFacade.PROJECT_CONFIG_COLLECTION, new DataDocument(Configuration.NAMEVALUE, Index.ASCENDING), true);
      }
      if (!dataStorage.hasCollection(ConfigurationFacade.ORGANIZATION_CONFIG_COLLECTION)) {
         dataStorage.createCollection(ConfigurationFacade.ORGANIZATION_CONFIG_COLLECTION);
         dataStorage.createIndex(ConfigurationFacade.ORGANIZATION_CONFIG_COLLECTION, new DataDocument(Configuration.NAMEVALUE, Index.ASCENDING), true);
      }
   }

   private void initUserSettingsCollection() {
      if (!dataStorage.hasCollection(UserSettings.COLLECTION_NAME)) {
         dataStorage.createCollection(UserSettings.COLLECTION_NAME);
         dataStorage.createIndex(UserSettings.COLLECTION_NAME, new DataDocument(UserSettings.ATTR_USER, Index.ASCENDING), true);
      }
   }

   private void initUserGroupCollection() {
      if (!dataStorage.hasCollection(UserGroup.COLLECTION_NAME)) {
         dataStorage.createCollection(UserGroup.COLLECTION_NAME);
         dataStorage.createIndex(UserGroup.COLLECTION_NAME, new DataDocument(UserGroup.ATTR_ORG_ID, Index.ASCENDING), true);
         dataStorage.createIndex(UserGroup.COLLECTION_NAME, new DataDocument(UserGroup.ATTR_ORG_ID, Index.ASCENDING)
               .append(UserGroup.ATTR_USERS, Index.ASCENDING), false);

      }
   }

   private void initOrganizationCollection() {
      if (!dataStorage.hasCollection(Organization.COLLECTION_NAME)) {
         dataStorage.createCollection(Organization.COLLECTION_NAME);
         dataStorage.createIndex(Organization.COLLECTION_NAME, new DataDocument(Organization.ATTR_ORG_CODE, Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection which holds metadata for all views in given project.
    *
    * @param projectId
    *       project id
    */
   private void initViewsMetadata(String projectId) {
      String viewsCollection = viewFacade.metadataCollectionFromId(projectId);
      if (!userDataStorage.hasCollection(viewsCollection)) {
         userDataStorage.createCollection(viewsCollection);
         userDataStorage.createIndex(viewsCollection, new DataDocument(View.ID_KEY, Index.ASCENDING), true);
         userDataStorage.createIndex(viewsCollection, new DataDocument(View.NAME_KEY, Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection which holds metadata for all collections in given project.
    *
    * @param projectId
    *       project id
    */
   private void initCollectionsMetadata(String projectId) {
      String collectionMetadataCollection = collectionMetadataFacade.metadataCollectionFromId(projectId);
      if (!userDataStorage.hasCollection(collectionMetadataCollection)) {
         userDataStorage.createCollection(collectionMetadataCollection);
         userDataStorage.createIndex(collectionMetadataCollection, new DataDocument(Collection.CODE, Index.ASCENDING), true);
         userDataStorage.createIndex(collectionMetadataCollection, new DataDocument(Collection.REAL_NAME, Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection in system data storage which holds information about roles for every organization.
    */
   private void initOrganizationRolesCollection() {
      if (!dataStorage.hasCollection(Security.ORGANIZATION_ROLES_COLLECTION_NAME)) {
         dataStorage.createCollection(Security.ORGANIZATION_ROLES_COLLECTION_NAME);
         dataStorage.createIndex(Security.ORGANIZATION_ROLES_COLLECTION_NAME,
               new DataDocument(Security.ORGANIZATION_ID_KEY, Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection in user data storage which holds information about roles for projects,
    * views and collections of current organization.
    */
   private void initRolesCollection() {
      if (!userDataStorage.hasCollection(Security.ROLES_COLLECTION_NAME)) {
         userDataStorage.createCollection(Security.ROLES_COLLECTION_NAME);

         userDataStorage.createIndex(Security.ROLES_COLLECTION_NAME,
               new DataDocument(Security.PROJECT_ID_KEY, Index.ASCENDING)
                     .append(Security.TYPE_KEY, Index.ASCENDING)
                     .append(Security.TYPE_ID_KEY, Index.ASCENDING), true);
      }
   }

   /**
    * Initializes document with roles for given organization.
    *
    * @param organizationId
    *       organization id
    */
   private void initOrganizationRoles(String organizationId) {
      DataDocument roles = new DataDocument()
            .append(Security.ORGANIZATION_ID_KEY, organizationId)
            .append(Security.PERMISSIONS_KEY, new DataDocument()
                  .append(Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_WRITE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList())));

      dataStorage.createDocument(Security.ORGANIZATION_ROLES_COLLECTION_NAME, roles);
   }

   /**
    * Initializes document with roles for given project.
    *
    * @param projectId
    *       project id
    */
   private void initProjectRoles(String projectId) {
      initTypeRoles(projectId, Security.TYPE_PROJECT, null, Security.RESOURCE_ROLES.get(Security.PROJECT_RESOURCE));
   }

   /**
    * Initializes document with roles for given collection.
    *
    * @param projectCode
    *       project code
    * @param collectionId
    *       collection id
    */
   private void initCollectionRoles(String projectCode, String collectionId) {
      initTypeRoles(projectFacade.getProjectId(projectCode), Security.TYPE_COLLECTION, collectionId, Security.RESOURCE_ROLES.get(Security.COLLECTION_RESOURCE));
   }

   /**
    * Initializes document with roles for given view.
    *
    * @param projectCode
    *       project code
    * @param viewId
    *       view
    */
   private void initViewRoles(String projectCode, int viewId) {
      initTypeRoles(projectFacade.getProjectId(projectCode), Security.TYPE_VIEW, Integer.toString(viewId), Security.RESOURCE_ROLES.get(Security.VIEW_RESOURCE));
   }

   private void initTypeRoles(String projectId, String typeKey, String typeId, Set<String> roles) {
      DataDocument doc = new DataDocument()
            .append(Security.PROJECT_ID_KEY, projectId)
            .append(Security.TYPE_KEY, typeKey)
            .append(Security.TYPE_ID_KEY, typeId);
      DataDocument rolesDoc = new DataDocument();
      for (String r : roles) {
         rolesDoc.append(r, new DataDocument()
               .append(Security.USERS_KEY, Collections.emptyList())
               .append(Security.GROUP_KEY, Collections.emptyList()));
      }
      doc.append(Security.PERMISSIONS_KEY, rolesDoc);
      userDataStorage.createDocument(Security.ROLES_COLLECTION_NAME, doc);
   }

   private void initProjectCollection() {
      if (!dataStorage.hasCollection(Project.COLLECTION_NAME)) {
         dataStorage.createCollection(Project.COLLECTION_NAME);
         dataStorage.createIndex(Project.COLLECTION_NAME, new DataDocument(Project.ATTR_ORGANIZATION_ID, Index.ASCENDING)
               .append(Project.ATTR_PROJECT_CODE, Index.ASCENDING), true);
      }
   }

   public void onOrganizationCreated(final String organizationId) {
      // init userGroup collection
      DataDocument userGroup = new DataDocument(UserGroup.ATTR_ORG_ID, organizationId)
            .append(UserGroup.ATTR_USERS, Collections.emptyList());
      dataStorage.createDocument(UserGroup.COLLECTION_NAME, userGroup);

      // init group collection
      DataDocument group = new DataDocument(Group.ATTR_ORG_ID, organizationId)
            .append(Group.ATTR_GROUPS, Collections.emptyList());
      dataStorage.createDocument(Group.COLLECTION_NAME, group);

      // initialize document with roles
      initOrganizationRoles(organizationId);
      // initializes collection with roles for collections, views and projects inside the organization
      initRolesCollection();
   }

   public void onOrganizationRemoved(final String organizationId) {
      // clean userGroup collection
      dataStorage.dropDocument(UserGroup.COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(UserGroup.ATTR_ORG_ID, organizationId));

      // clean group collection
      dataStorage.dropDocument(Group.COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(Group.ATTR_ORG_ID, organizationId));
   }

   /**
    * Initializes some project collections and documents.
    *
    * @param projectId
    *       project id
    */
   public void onProjectCreated(final String projectId) {
      initProjectRoles(projectId);
      initCollectionsMetadata(projectId);
      initViewsMetadata(projectId);
   }

   /**
    * Initializes some documents for new collection.
    *
    * @param projectCode
    *       project code
    * @param collectionId
    *       collection id
    */
   public void onCollectionCreated(final String projectCode, final String collectionId) {
      initCollectionRoles(projectCode, collectionId);
   }

   /**
    * Initializes some documents for new view.
    *
    * @param projectCode
    *       project code
    * @param viewId
    *       view
    */
   public void onViewCreated(final String projectCode, final int viewId) {
      initViewRoles(projectCode, viewId);
   }

}