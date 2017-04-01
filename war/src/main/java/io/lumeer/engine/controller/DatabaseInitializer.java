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

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class DatabaseInitializer{

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;


   public void init(@Observes @Initialized(RequestScoped.class) Object init) {

   }

   @PostConstruct
   public void init() {
      if (!dataStorage.hasCollection(LumeerConst.Project.COLLECTION_NAME)) {
         dataStorage.createCollection(LumeerConst.Project.COLLECTION_NAME);
         dataStorage.createIndex(LumeerConst.Project.COLLECTION_NAME, new DataDocument(LumeerConst.Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.ATTR_PROJECT_NAME, LumeerConst.Index.ASCENDING), true);
         dataStorage.createIndex(LumeerConst.Project.COLLECTION_NAME, new DataDocument(LumeerConst.Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.ATTR_PROJECT_ID, LumeerConst.Index.ASCENDING), true);
         dataStorage.createIndex(LumeerConst.Project.COLLECTION_NAME, new DataDocument(LumeerConst.Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.ATTR_PROJECT_ID, LumeerConst.Index.ASCENDING)
               .append(dataStorageDialect.concatFields(LumeerConst.Project.ATTR_USERS, LumeerConst.Project.ATTR_USERS_USERNAME), LumeerConst.Index.ASCENDING), false);
      }

      if (!dataStorage.hasCollection(LumeerConst.Project.UserRoles.COLLECTION_NAME)) {
         dataStorage.createCollection(LumeerConst.Project.UserRoles.COLLECTION_NAME);
         dataStorage.createIndex(LumeerConst.Project.UserRoles.COLLECTION_NAME, new DataDocument(LumeerConst.Project.UserRoles.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.UserRoles.ATTR_PROJECT_ID, LumeerConst.Index.ASCENDING)
               .append(LumeerConst.Project.UserRoles.ATTR_USER_ROLE, LumeerConst.Index.ASCENDING), true);
      }
   }

}