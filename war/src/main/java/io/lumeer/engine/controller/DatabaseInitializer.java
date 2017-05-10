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

   public void init(@Observes @Initialized(RequestScoped.class) Object init) {

   }

   @PostConstruct
   public void init() {
      initProjectCollection();
      initOrganizationCollection();
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

   public void initializeRolesCollections() {
      if (!dataStorage.hasCollection(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME)) {
         dataStorage.createCollection(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME);
         dataStorage.createIndex(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
               new DataDocument(LumeerConst.Security.ORGANIZATION_ID_KEY, LumeerConst.Index.ASCENDING), true);
      }

      if (!userDataStorage.hasCollection(LumeerConst.Security.ROLES_COLLECTION_NAME)) {
         userDataStorage.createCollection(LumeerConst.Security.ROLES_COLLECTION_NAME);

         // SecurityFacade#projectFilter
         userDataStorage.createIndex(LumeerConst.Security.ROLES_COLLECTION_NAME,
               new DataDocument(LumeerConst.Security.PROJECT_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(LumeerConst.Security.TYPE_KEY, LumeerConst.Index.ASCENDING), true);

         // SecurityFacade#collectionFilter
         userDataStorage.createIndex(LumeerConst.Security.ROLES_COLLECTION_NAME,
               new DataDocument(LumeerConst.Security.PROJECT_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(LumeerConst.Security.COLLECTION_NAME_KEY, LumeerConst.Index.ASCENDING)
                     .append(LumeerConst.Security.TYPE_KEY, LumeerConst.Index.ASCENDING), true);

         // SecurityFacade#viewFilter
         userDataStorage.createIndex(LumeerConst.Security.ROLES_COLLECTION_NAME,
               new DataDocument(LumeerConst.Security.PROJECT_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(LumeerConst.Security.VIEW_ID_KEY, LumeerConst.Index.ASCENDING)
                     .append(LumeerConst.Security.TYPE_KEY, LumeerConst.Index.ASCENDING), true);
      }
   }

   public void initializeOrganizationRoles(String organizationId) {
      DataDocument roles = new DataDocument()
            .append(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationId)
            .append(LumeerConst.Security.ROLES_KEY, new DataDocument()
                  .append(LumeerConst.Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_WRITE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList())));

      dataStorage.createDocument(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME, roles);
   }

   public void initializeProjectRoles(String projectId) {
      DataDocument roles = new DataDocument()
            .append(LumeerConst.Security.PROJECT_ID_KEY, projectId)
            .append(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_PROJECT)
            .append(LumeerConst.Security.ROLES_KEY, new DataDocument()
                  .append(LumeerConst.Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_WRITE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList())));

      userDataStorage.createDocument(LumeerConst.Security.ROLES_COLLECTION_NAME, roles);
   }

   public void initializeCollectionRoles(String projectId, String collectionName) {
      DataDocument roles = new DataDocument()
            .append(LumeerConst.Security.PROJECT_ID_KEY, projectId)
            .append(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_COLLECTION)
            .append(LumeerConst.Security.COLLECTION_NAME_KEY, collectionName)
            .append(LumeerConst.Security.ROLES_KEY, new DataDocument()
                  .append(LumeerConst.Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_READ,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_SHARE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_WRITE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList())));

      userDataStorage.createDocument(LumeerConst.Security.ROLES_COLLECTION_NAME, roles);
   }

   public void initializeViewRoles(String projectId, int viewId) {
      DataDocument roles = new DataDocument()
            .append(LumeerConst.Security.PROJECT_ID_KEY, projectId)
            .append(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_VIEW)
            .append(LumeerConst.Security.VIEW_ID_KEY, viewId)
            .append(LumeerConst.Security.ROLES_KEY, new DataDocument()
                  .append(LumeerConst.Security.ROLE_MANAGE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_READ,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList()))
                  .append(LumeerConst.Security.ROLE_CLONE,
                        new DataDocument()
                              .append(LumeerConst.Security.USERS_KEY, Collections.emptyList())
                              .append(LumeerConst.Security.GROUP_KEY, Collections.emptyList())));

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
   }

   public void onOrganizationRemoved(final String organization) {
      // clean userGroup collection
      dataStorage.dropDocument(UserGroup.COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(UserGroup.ATTR_ORG_ID, organization));

      // clean group collection
      dataStorage.dropDocument(Group.COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(Group.ATTR_ORG_ID, organization));
   }
}