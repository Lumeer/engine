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
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Manipulates with project related data.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class ProjectFacade {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   private String projectId = "default";

   public String getCurrentProjectId() {
      return projectId;
   }

   public void setCurrentProjectId(final String projectId) {
      this.projectId = projectId;
   }

   public Map<String, String> getProjectsMap(final String organizationName) {
      return null;
   }

   public String getProjectId(final String projectName) {
      return null;
   }

   public void changeProjectId(final String oldProjectId, final String newProjectId) {

   }

   public String getProjectName(final String projectId) {
      return null;
   }

   public void setProjectMetadata(final String projectId, final String metaName, final String value) {

   }

   public String getProjectMetadata(final String projectId, final String metaName) {
      return null;
   }

   private DataDocument getProjectMetadata(final String projectId) {
      // helper method for getProjectMetadata
      return null;
   }

   public void renameProject(final String projectId, final String newProjectName) {

   }

   public void createProject(final String projectId, final String projectName) {

   }

   public void deleteProject(final String projectId) {

   }

   public void setDefaultRolesToProject(final String projectId, final List<String> userRoleIds) {

   }

   public void addUserToProject(final String projectId, final String userId) {

   }

   public void removeUserFromProject(final String projectId, final String userId) {

   }

   public List<String> getProjectUsers(final String projectId) {
      return null;
   }

   public void addUserRole(final String projectId, final String userId, final String userRoleId) {

   }

   public void removeUserRole(final String projectId, final String userId, final String userRoleId) {

   }
}
