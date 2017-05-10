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
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.LumeerConst.Group;
import io.lumeer.engine.api.LumeerConst.Organization;
import io.lumeer.engine.api.LumeerConst.Project;
import io.lumeer.engine.api.LumeerConst.Security;
import io.lumeer.engine.api.LumeerConst.UserGroup;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import java.util.Collections;
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
   }

   private void initUserGroupCollection() {
      if (!dataStorage.hasCollection(UserGroup.COLLECTION_NAME)) {
         dataStorage.createCollection(UserGroup.COLLECTION_NAME);
         dataStorage.createIndex(UserGroup.COLLECTION_NAME, new DataDocument(UserGroup.ATTR_ORG_ID, LumeerConst.Index.ASCENDING), true);
         dataStorage.createIndex(UserGroup.COLLECTION_NAME, new DataDocument(UserGroup.ATTR_ORG_ID, LumeerConst.Index.ASCENDING)
               .append(UserGroup.ATTR_USERS, LumeerConst.Index.ASCENDING), false);

      }
   }

   private void initOrganizationCollection() {
      if (!dataStorage.hasCollection(Organization.COLLECTION_NAME)) {
         dataStorage.createCollection(Organization.COLLECTION_NAME);
         dataStorage.createIndex(Organization.COLLECTION_NAME, new DataDocument(Organization.ATTR_ORG_ID, LumeerConst.Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection which holds metadata for all views in given project.
    *
    * @param projectId
    *       project id
    */
   private void initViewsMetadata(String projectId) {
      String viewsCollection = viewFacade.metadataCollection(projectId);
      if (!userDataStorage.hasCollection(viewsCollection)) {
         userDataStorage.createCollection(viewsCollection);
         userDataStorage.createIndex(viewsCollection, new DataDocument(LumeerConst.View.ID_KEY, LumeerConst.Index.ASCENDING), true);
         userDataStorage.createIndex(viewsCollection, new DataDocument(LumeerConst.View.NAME_KEY, LumeerConst.Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection which holds metadata for all collections in given project.
    *
    * @param projectId
    *       project id
    */
   private void initCollectionsMetadata(String projectId) {
      String collectionMetadataCollection = collectionMetadataFacade.metadataCollection(projectId);
      if (!userDataStorage.hasCollection(collectionMetadataCollection)) {
         userDataStorage.createCollection(collectionMetadataCollection);
         userDataStorage.createIndex(collectionMetadataCollection, new DataDocument(LumeerConst.Collection.INTERNAL_NAME_KEY, LumeerConst.Index.ASCENDING), true);
         userDataStorage.createIndex(collectionMetadataCollection, new DataDocument(LumeerConst.Collection.REAL_NAME_KEY, LumeerConst.Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection in system data storage which holds information about roles for every organization.
    */
   private void initOrganizationRolesCollection() {
      if (!dataStorage.hasCollection(Security.ORGANIZATION_ROLES_COLLECTION_NAME)) {
         dataStorage.createCollection(Security.ORGANIZATION_ROLES_COLLECTION_NAME);
         dataStorage.createIndex(Security.ORGANIZATION_ROLES_COLLECTION_NAME,
               new DataDocument(Security.ORGANIZATION_ID_KEY, LumeerConst.Index.ASCENDING), true);
      }
   }

   /**
    * Initializes collection in user data storage which holds information about roles for projects,
    * views and collections of current organization.
    */
   private void initRolesCollection() {
      if (!userDataStorage.hasCollection(Security.ROLES_COLLECTION_NAME)) {
         userDataStorage.createCollection(Security.ROLES_COLLECTION_NAME);

         // SecurityFacade#projectFilter
         userDataStorage.createIndex(Security.ROLES_COLLECTION_NAME,
               new DataDocument(Security.PROJECT_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(Security.TYPE_KEY, LumeerConst.Index.ASCENDING), true);

         // SecurityFacade#collectionFilter
         userDataStorage.createIndex(Security.ROLES_COLLECTION_NAME,
               new DataDocument(Security.PROJECT_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(Security.COLLECTION_NAME_KEY, LumeerConst.Index.ASCENDING)
                     .append(Security.TYPE_KEY, LumeerConst.Index.ASCENDING), true);

         // SecurityFacade#viewFilter
         userDataStorage.createIndex(Security.ROLES_COLLECTION_NAME,
               new DataDocument(Security.PROJECT_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(Security.VIEW_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(Security.TYPE_KEY, LumeerConst.Index.ASCENDING), true);
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
            .append(Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationIdentificator(organizationId))
            .append(Security.ROLES_KEY, new DataDocument()
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
      DataDocument roles = new DataDocument()
            .append(Security.PROJECT_ID_KEY, projectFacade.getProjectIdentificator(projectId))
            .append(Security.TYPE_KEY, Security.TYPE_PROJECT)
            .append(Security.ROLES_KEY, new DataDocument()
                  .append(Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_WRITE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList())));

      userDataStorage.createDocument(LumeerConst.Security.ROLES_COLLECTION_NAME, roles);
   }

   /**
    * Initializes document with roles for given collection.
    *
    * @param projectId
    *       project id
    * @param collectionName
    *       collection
    */
   private void initCollectionRoles(String projectId, String collectionName) {
      DataDocument roles = new DataDocument()
            .append(Security.PROJECT_ID_KEY, projectFacade.getProjectIdentificator(projectId))
            .append(Security.TYPE_KEY, Security.TYPE_COLLECTION)
            .append(Security.COLLECTION_NAME_KEY, collectionName)
            .append(Security.ROLES_KEY, new DataDocument()
                  .append(Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_READ,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_SHARE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_WRITE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList())));

      userDataStorage.createDocument(Security.ROLES_COLLECTION_NAME, roles);
   }

   /**
    * Initializes document with roles for given view.
    * @param projectId project id
    * @param viewId view
    */
   private void initViewRoles(String projectId, int viewId) {
      DataDocument roles = new DataDocument()
            .append(Security.PROJECT_ID_KEY, projectFacade.getProjectIdentificator(projectId))
            .append(Security.TYPE_KEY, Security.TYPE_VIEW)
            .append(Security.VIEW_ID_KEY, viewId)
            .append(Security.ROLES_KEY, new DataDocument()
                  .append(Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_READ,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList()))
                  .append(Security.ROLE_CLONE,
                        new DataDocument()
                              .append(Security.USERS_KEY, Collections.emptyList())
                              .append(Security.GROUP_KEY, Collections.emptyList())));

      userDataStorage.createDocument(LumeerConst.Security.ROLES_COLLECTION_NAME, roles);
   }

   private void initProjectCollection() {
      if (!dataStorage.hasCollection(Project.COLLECTION_NAME)) {
         dataStorage.createCollection(Project.COLLECTION_NAME);
         dataStorage.createIndex(Project.COLLECTION_NAME, new DataDocument(Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(Project.ATTR_PROJECT_ID, LumeerConst.Index.ASCENDING), true);
      }
   }

   public void onOrganizationCreated(final String organization) {
      // init userGroup collection
      DataDocument userGroup = new DataDocument(UserGroup.ATTR_ORG_ID, organization)
            .append(UserGroup.ATTR_USERS, Collections.emptyList());
      dataStorage.createDocument(UserGroup.COLLECTION_NAME, userGroup);

      // init group collection
      DataDocument group = new DataDocument(Group.ATTR_ORG_ID, organization)
            .append(Group.ATTR_GROUPS, Collections.emptyList());
      dataStorage.createDocument(Group.COLLECTION_NAME, group);

      // initialize document with roles
      initOrganizationRoles(organization);
      // initializes collection with roles for collections, views and projects inside the organization
      initRolesCollection();
   }

   public void onOrganizationRemoved(final String organization) {
      // clean userGroup collection
      dataStorage.dropDocument(UserGroup.COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(UserGroup.ATTR_ORG_ID, organization));

      // clean group collection
      dataStorage.dropDocument(Group.COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(Group.ATTR_ORG_ID, organization));
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
    * @param projectId
    *       project id
    * @param collection
    *       collection name
    */
   public void onCollectionCreated(final String projectId, final String collection) {
      initCollectionRoles(projectId, collection);
   }

   /**
    * Initializes some documents for new view.
    *
    * @param projectId
    *       project id
    * @param viewId
    *       view
    */
   public void onViewCreated(final String projectId, final int viewId) {
      initViewRoles(projectId, viewId);
   }

}