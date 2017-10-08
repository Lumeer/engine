/*
* Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   /********* CHECKING ROLES *********/

   public boolean hasOrganizationRole(String organizationCode, String role) {
      DataDocument doc = systemDataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationId(organizationCode)),
            Collections.singletonList(LumeerConst.Security.PERMISSIONS_KEY));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationCode);
   }

   public boolean hasProjectRole(String projectCode, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            Collections.singletonList(dataStorageDialect.concatFields(LumeerConst.Security.PERMISSIONS_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationCode());
   }

   public boolean hasViewRole(String projectCode, int viewId, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(viewFilter(projectCode, viewId)),
            Collections.singletonList(dataStorageDialect.concatFields(LumeerConst.Security.PERMISSIONS_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationCode());
   }

   private boolean checkRole(DataDocument rolesDocument, String user, String role, String organizationCode) {
      List<String> groupsForUser = userGroupFacade.getGroupsOfUser(organizationCode, user);
      if (groupsForUser == null) {
         groupsForUser = Collections.emptyList();
      }

      DataDocument roleDoc = rolesDocument.getDataDocument(LumeerConst.Security.PERMISSIONS_KEY).getDataDocument(role);

      if (roleDoc.getArrayList(LumeerConst.Security.USERS_KEY, String.class).contains(user)) {
         return true;
      }

      List<String> groups = roleDoc.getArrayList(LumeerConst.Security.GROUP_KEY, String.class);
      groups.retainAll(groupsForUser);

      return !groups.isEmpty();
   }

   /********* ADDING ROLES *********/

   public void addOrganizationUsersRole(String organizationCode, List<String> users, String role) {
      addOrganizationRole(organizationCode, LumeerConst.Security.USERS_KEY, users, role);
   }

   public void addOrganizationGroupsRole(String organizationCode, List<String> groups, String role) {
      addOrganizationRole(organizationCode, LumeerConst.Security.GROUP_KEY, groups, role);
   }

   private void addOrganizationRole(String organizationCode, String key, List<String> values, String role) {
      systemDataStorage.addItemsToArray(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationId(organizationCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.PERMISSIONS_KEY,
                  role,
                  key),
            values);
   }

   public void addProjectUsersRole(String projectCode, List<String> users, String role) {
      addProjectRole(projectCode, LumeerConst.Security.USERS_KEY, users, role);
   }

   public void addProjectGroupsRole(String projectCode, List<String> groups, String role) {
      addProjectRole(projectCode, LumeerConst.Security.GROUP_KEY, groups, role);
   }

   private void addProjectRole(String projectCode, String key, List<String> values, String role) {
      dataStorage.addItemsToArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.PERMISSIONS_KEY,
                  role,
                  key),
            values);
   }

   public void addViewUsersRole(String projectCode, int viewId, List<String> users, String role) {
      addViewRole(viewId, projectCode, LumeerConst.Security.USERS_KEY, users, role);
   }

   public void addViewGroupsRole(String projectCode, int viewId, List<String> groups, String role) {
      addViewRole(viewId, projectCode, LumeerConst.Security.GROUP_KEY, groups, role);
   }

   private void addViewRole(int viewId, String projectCode, String key, List<String> values, String role) {

      Map<String, Object> filter = viewFilter(projectCode, viewId);

      dataStorage.addItemsToArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.PERMISSIONS_KEY,
                  role,
                  key),
            values);
   }

   /********* REMOVING ROLES *********/

   public void removeOrganizationUsersRole(String organizationCode, List<String> users, String role) {
      removeOrganizationsRole(organizationCode, LumeerConst.Security.USERS_KEY, users, role);
   }

   public void removeOrganizationGroupsRole(String organizationCode, List<String> groups, String role) {
      removeOrganizationsRole(organizationCode, LumeerConst.Security.GROUP_KEY, groups, role);
   }

   private void removeOrganizationsRole(String organizationCode, String key, List<String> values, String role) {
      systemDataStorage.removeItemsFromArray(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationId(organizationCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.PERMISSIONS_KEY,
                  role,
                  key),
            values);
   }

   public void removeProjectUsersRole(String projectCode, List<String> users, String role) {
      removeProjectRole(projectCode, LumeerConst.Security.USERS_KEY, users, role);
   }

   public void removeProjectGroupsRole(String projectCode, List<String> groups, String role) {
      removeProjectRole(projectCode, LumeerConst.Security.GROUP_KEY, groups, role);
   }

   private void removeProjectRole(String projectCode, String key, List<String> values, String role) {
      dataStorage.removeItemsFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.PERMISSIONS_KEY,
                  role,
                  key),
            values);
   }

   public void removeViewUsersRole(String projectCode, int viewId, List<String> users, String role) {
      removeViewRole(projectCode, viewId, LumeerConst.Security.USERS_KEY, users, role);
   }

   public void removeViewGroupsRole(String projectCode, int viewId, List<String> groups, String role) {
      removeViewRole(projectCode, viewId, LumeerConst.Security.GROUP_KEY, groups, role);
   }

   private void removeViewRole(String projectCode, int viewId, String key, List<String> values, String role) {
      Map<String, Object> filter = viewFilter(projectCode, viewId);

      dataStorage.removeItemsFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.PERMISSIONS_KEY,
                  role,
                  key),
            values);
   }

   private Map<String, Object> projectFilter(String projectCode) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectId(projectCode));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_PROJECT);
      return filter;
   }

   private Map<String, Object> viewFilter(String projectCode, int viewId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectId(projectCode));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_VIEW);
      filter.put(LumeerConst.Security.TYPE_ID_KEY, Integer.toString(viewId));
      return filter;
   }

   /********* GETTING ROLES *********/
   public List<Role> getOrganizationRoles(String organizationCode) {
      DataDocument doc = systemDataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY,
                  organizationFacade.getOrganizationId(organizationCode)),
            Collections.singletonList(LumeerConst.Security.PERMISSIONS_KEY));

      return getRolesList(doc);
   }

   public List<Role> getProjectRoles(String projectCode) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectCode)),
            Collections.singletonList(LumeerConst.Security.PERMISSIONS_KEY));

      return getRolesList(doc);
   }

   public List<Role> getViewRoles(String projectCode, int viewId) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(viewFilter(projectCode, viewId)),
            Collections.singletonList(LumeerConst.Security.PERMISSIONS_KEY));

      return getRolesList(doc);
   }

   private List<Role> getRolesList(DataDocument doc) {
      List<Role> output = new ArrayList<>();
      doc.getDataDocument(LumeerConst.Security.PERMISSIONS_KEY).forEach((k, v) -> {
         output.add(new Role(k, (DataDocument) v));
      });

      return output;
   }
}
