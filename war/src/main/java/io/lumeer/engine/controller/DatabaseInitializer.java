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
      if (!dataStorage.hasCollection(LumeerConst.Project.COLLECTION_NAME)) {
         dataStorage.createCollection(LumeerConst.Project.COLLECTION_NAME);
         dataStorage.createIndex(LumeerConst.Project.COLLECTION_NAME, new DataDocument(LumeerConst.Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.ATTR_PROJECT_ID, LumeerConst.Index.ASCENDING), true);
         dataStorage.createIndex(LumeerConst.Project.COLLECTION_NAME, new DataDocument(LumeerConst.Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.ATTR_PROJECT_ID, LumeerConst.Index.ASCENDING)
               .append(dataStorageDialect.concatFields(LumeerConst.Project.ATTR_USERS, LumeerConst.Project.ATTR_USERS_USERNAME), LumeerConst.Index.ASCENDING), false);
      }

      if (!dataStorage.hasCollection(LumeerConst.Organization.COLLECTION_NAME)) {
         dataStorage.createCollection(LumeerConst.Organization.COLLECTION_NAME);
         dataStorage.createIndex(LumeerConst.Organization.COLLECTION_NAME, new DataDocument(LumeerConst.Organization.ATTR_ORG_ID, LumeerConst.Index.ASCENDING), true);
         dataStorage.createIndex(LumeerConst.Organization.COLLECTION_NAME, new DataDocument(LumeerConst.Organization.ATTR_ORG_ID, LumeerConst.Index.ASCENDING)
               .append(dataStorageDialect.concatFields(LumeerConst.Organization.ATTR_USERS, LumeerConst.Organization.ATTR_USERS_USERNAME), LumeerConst.Index.ASCENDING), false);
      }

      // TODO: we have to create the collection for every project
      if (!userDataStorage.hasCollection(viewFacade.metadataCollection())) {
         userDataStorage.createCollection(viewFacade.metadataCollection());
         userDataStorage.createIndex(viewFacade.metadataCollection(), new DataDocument(LumeerConst.View.ID_KEY, LumeerConst.Index.ASCENDING), true);
         userDataStorage.createIndex(viewFacade.metadataCollection(), new DataDocument(LumeerConst.View.NAME_KEY, LumeerConst.Index.ASCENDING), true);
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
}