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
import io.lumeer.engine.api.dto.Role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@SessionScoped
public class SecurityFacade implements Serializable {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private UserFacade userFacade;

   @Inject
   private UserGroupFacade userGroupFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   /********* CHECKING ROLES *********/

   public boolean hasOrganizationRole(String organizationCode, String role) {
      DataDocument doc = systemDataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationId(organizationCode)),
            Arrays.asList(LumeerConst.Security.ROLES_KEY));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationCode);
   }

   public boolean hasProjectRole(String projectId, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectId)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationCode());
   }

   public boolean hasCollectionRole(String projectId, String collectionName, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(collectionFilter(projectId, collectionName)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationCode());
   }

   public boolean hasViewRole(String projectId, int viewId, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(viewFilter(projectId, viewId)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationCode());
   }

   private boolean checkRole(DataDocument rolesDocument, String user, String role, String organizationCode) {
      List<String> groupsForUser = userGroupFacade.getGroupsOfUser(organizationCode, user);
      if (groupsForUser == null) {
         groupsForUser = Collections.emptyList();
      }

      DataDocument roleDoc = rolesDocument.getDataDocument(LumeerConst.Security.ROLES_KEY).getDataDocument(role);

      if (roleDoc.getArrayList(LumeerConst.Security.USERS_KEY, String.class).contains(user)) {
         return true;
      }

      List<String> groups = roleDoc.getArrayList(LumeerConst.Security.GROUP_KEY, String.class);
      groups.retainAll(groupsForUser);

      return !groups.isEmpty();
   }

   /********* ADDING ROLES *********/

   public void addOrganizationUserRole(String organizationCode, String user, String role) {
      addOrganizationRole(organizationCode, user, null, role);
   }

   public void addOrganizationGroupRole(String organizationCode, String group, String role) {
      addOrganizationRole(organizationCode, null, group, role);
   }

   private void addOrganizationRole(String organizationCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      systemDataStorage.addItemToArray(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationId(organizationCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void addProjectUserRole(String projectCode, String user, String role) {
      addProjectRole(projectCode, user, null, role);
   }

   public void addProjectGroupRole(String projectCode, String group, String role) {
      addProjectRole(projectCode, null, group, role);
   }

   private void addProjectRole(String projectCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      dataStorage.addItemToArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void addCollectionUserRole(String projectCode, String collectionName, String user, String role) {
      addCollectionRole(collectionName, projectCode, user, null, role);
   }

   public void addCollectionGroupRole(String projectCode, String collectionName, String group, String role) {
      addCollectionRole(collectionName, projectCode, null, group, role);
   }

   private void addCollectionRole(String collectionName, String projectCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = collectionFilter(projectCode, collectionName);

      dataStorage.addItemToArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void addViewUserRole(String projectCode, int viewId, String user, String role) {
      addViewRole(viewId, projectCode, user, null, role);
   }

   public void addViewGroupRole(String projectCode, int viewId, String group, String role) {
      addViewRole(viewId, projectCode, null, group, role);
   }

   private void addViewRole(int viewId, String projectCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = viewFilter(projectCode, viewId);

      dataStorage.addItemToArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   /********* REMOVING ROLES *********/

   public void removeOrganizationUserRole(String organizationCode, String user, String role) {
      removeOrganizationRole(organizationCode, user, null, role);
   }

   public void removeOrganizationGroupRole(String organizationCode, String group, String role) {
      removeOrganizationRole(organizationCode, null, group, role);
   }

   private void removeOrganizationRole(String organizationCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      systemDataStorage.removeItemFromArray(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationId(organizationCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void removeProjectUserRole(String projectCode, String user, String role) {
      removeProjectRole(projectCode, user, null, role);
   }

   public void removeProjectGroupRole(String projectCode, String group, String role) {
      removeProjectRole(projectCode, null, group, role);
   }

   private void removeProjectRole(String projectCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      dataStorage.removeItemFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void removeCollectionUserRole(String projectCode, String collectionName, String user, String role) {
      removeCollectionRole(projectCode, collectionName, user, null, role);
   }

   public void removeCollectionGroupRole(String projectCode, String collectionName, String group, String role) {
      removeCollectionRole(projectCode, collectionName, null, group, role);
   }

   private void removeCollectionRole(String projectCode, String collectionName, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = collectionFilter(projectCode, collectionName);

      dataStorage.removeItemFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void removeViewUserRole(String projectCode, int viewId, String user, String role) {
      removeViewRole(projectCode, viewId, user, null, role);
   }

   public void removeViewGroupRole(String projectCode, int viewId, String group, String role) {
      removeViewRole(projectCode, viewId, null, group, role);
   }

   private void removeViewRole(String projectCode, int viewId, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = viewFilter(projectCode, viewId);

      dataStorage.removeItemFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void dropCollectionSecurity(String projectCode, String collection) {
      dataStorage.dropDocument(LumeerConst.Security.ROLES_COLLECTION_NAME, dataStorageDialect.multipleFieldsValueFilter(collectionFilter(projectCode, collection)));
   }

   private Map<String, Object> projectFilter(String projectCode) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectId(projectCode));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_PROJECT);
      return filter;
   }

   private Map<String, Object> collectionFilter(String projectCode, String collectionName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectId(projectCode));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_COLLECTION);
      filter.put(LumeerConst.Security.COLLECTION_NAME_KEY, collectionName);
      return filter;
   }

   private Map<String, Object> viewFilter(String projectCode, int viewId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectId(projectCode));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_VIEW);
      filter.put(LumeerConst.Security.VIEW_ID_KEY, viewId);
      return filter;
   }

   private String userOrGroupKey(String user) {
      return user != null ? LumeerConst.Security.USERS_KEY : LumeerConst.Security.GROUP_KEY;
   }

   private String userOrGroupName(String user, String group) {
      return user != null ? user : group;
   }
   /********* GETTING ROLES *********/
   public List<Role> getOrganizationRoles(String organizationCode) {
      DataDocument doc = systemDataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY,
                  organizationFacade.getOrganizationId(organizationCode)),
            Arrays.asList(LumeerConst.Security.ROLES_KEY));

      return getRolesList(doc);
   }

   public List<Role> getProjectRoles(String projectCode) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY)));

      return getRolesList(doc);
   }

   public List<Role> getCollectionRoles(String projectCode, String collectionName) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(collectionFilter(projectCode, collectionName)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY)));

      return getRolesList(doc);
   }

   public List<Role> getViewRoles(String projectCode, int viewId) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(viewFilter(projectCode, viewId)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY)));

      return getRolesList(doc);
   }

   private List<Role> getRolesList(DataDocument doc) {
      List<Role> output = new ArrayList<>();
      doc.getDataDocument(LumeerConst.Security.ROLES_KEY).forEach((k, v)-> {
         output.add(new Role(k, (DataDocument) v));
      });

      return output;
   }
}
