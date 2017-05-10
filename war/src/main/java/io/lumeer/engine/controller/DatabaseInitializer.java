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