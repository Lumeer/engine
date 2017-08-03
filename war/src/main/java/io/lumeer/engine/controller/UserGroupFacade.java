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
import io.lumeer.engine.api.LumeerConst.Group;
import io.lumeer.engine.api.LumeerConst.UserGroup;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Stores users and their groups
 */
@SessionScoped
public class UserGroupFacade implements Serializable {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private OrganizationFacade organizationFacade;

   /**
    * Adds user to organization with one or more groups.
    *
    * @param organization
    *       Code of the organization.
    * @param user
    *       User identificator.
    * @param groups
    *       One or more group names.
    * @throws UserAlreadyExistsException
    *       When user already exists in organization.
    */
   public void addUser(final String organization, final String user, final String... groups) throws UserAlreadyExistsException {
      if (readUser(organization, user) != null) {
         throw new UserAlreadyExistsException(ErrorMessageBuilder.userAlreadyExistsInOrganizationString(user, organization));
      }
      DataDocument document = new DataDocument(UserGroup.ATTR_USERS_USER, user)
            .append(UserGroup.ATTR_USERS_GROUPS, Arrays.asList(groups));
      dataStorage.addItemToArray(UserGroup.COLLECTION_NAME, organizationFilter(organization), UserGroup.ATTR_USERS, document);
   }

   /**
    * Adds one or more groups to organization
    *
    * @param organization
    *       Code of the organization.
    * @param groups
    *       The one or more group names.
    */
   public void addGroups(final String organization, final String... groups) {
      if (groups.length == 0) {
         return;
      }
      dataStorage.addItemsToArray(Group.COLLECTION_NAME, organizationFilterGroups(organization), Group.ATTR_GROUPS, Arrays.asList(groups));
   }

   /**
    * Assign one or more group to user
    *
    * @param organization
    *       Code of the organization.
    * @param user
    *       User identificator.
    * @param groups
    *       The one or more group names.
    */
   public void addUserToGroups(final String organization, final String user, final String... groups) {
      if (groups.length == 0) {
         return;
      }
      dataStorage.addItemsToArray(UserGroup.COLLECTION_NAME, userFilter(organization, user), userParam(), Arrays.asList(groups));
   }

   /**
    * Remove user from organization
    *
    * @param organization
    *       Code of the organization.
    * @param user
    *       User identificator.
    */
   public void removeUser(final String organization, final String user) {
      dataStorage.removeItemFromArray(UserGroup.COLLECTION_NAME, organizationFilter(organization),
            UserGroup.ATTR_USERS, new DataDocument(UserGroup.ATTR_USERS_USER, user));
   }

   /**
    * Remove one or more groups from user
    *
    * @param organization
    *       Code of the organization.
    * @param user
    *       User identificator.
    * @param groups
    *       The one or more group names.
    */
   public void removeUserFromGroups(final String organization, final String user, final String... groups) {
      if (groups.length == 0) {
         return;
      }
      dataStorage.removeItemsFromArray(UserGroup.COLLECTION_NAME, userFilter(organization, user), userParam(), Arrays.asList(groups));
   }

   /**
    * Remove one or more groups from organization
    *
    * @param organization
    *       Code of the organization.
    * @param groups
    *       The one or more group names.
    */
   public void removeGroups(final String organization, final String... groups) {
      if (groups.length == 0) {
         return;
      }
      dataStorage.removeItemsFromArray(Group.COLLECTION_NAME, organizationFilterGroups(organization), Group.ATTR_GROUPS, Arrays.asList(groups));

      DataDocument dataDocument = dataStorage.readDocument(UserGroup.COLLECTION_NAME, organizationFilter(organization));
      if (dataDocument != null) {
         List<DataDocument> users = dataDocument.getArrayList(UserGroup.ATTR_USERS, DataDocument.class);
         if (!users.isEmpty()) {
            for (DataDocument user : users) {
               user.getArrayList(UserGroup.ATTR_USERS_GROUPS, String.class).removeAll(Arrays.asList(groups));
            }
            dataStorage.replaceDocument(UserGroup.COLLECTION_NAME, dataDocument, organizationFilter(organization));
         }
      }
   }

   /**
    * Read users in organization.
    *
    * @param organization
    *       Code of the organization.
    * @return The list of user names.
    */
   public List<String> getUsers(final String organization) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(UserGroup.COLLECTION_NAME, organizationFilter(organization), Collections.singletonList(UserGroup.ATTR_USERS));
      List<DataDocument> users = document.getArrayList(UserGroup.ATTR_USERS, DataDocument.class);
      return users.stream().map(u -> u.getString(UserGroup.ATTR_USERS_USER)).collect(Collectors.toList());
   }

   /**
    * Read users and their groups in organization.
    *
    * @param organization
    *       Code of the organization.
    * @return The map of user names and groups.
    */
   public Map<String, List<String>> getUsersAndGroups(final String organization) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(UserGroup.COLLECTION_NAME, organizationFilter(organization), Collections.singletonList(UserGroup.ATTR_USERS));
      if (document == null) {
         return Collections.emptyMap();
      }
      List<DataDocument> users = document.getArrayList(UserGroup.ATTR_USERS, DataDocument.class);
      return users.stream().collect(Collectors.toMap(u -> u.getString(UserGroup.ATTR_USERS_USER), u -> u.getArrayList(UserGroup.ATTR_USERS_GROUPS, String.class)));
   }

   /**
    * Read groups of user in organization.
    *
    * @param organization
    *       Code of the organization.
    * @param user
    *       User identificator.
    * @return The list of group names.
    */
   public List<String> getGroupsOfUser(final String organization, final String user) {
      DataDocument userDoc = readUser(organization, user);
      return userDoc != null ? userDoc.getArrayList(UserGroup.ATTR_USERS_GROUPS, String.class) : Collections.emptyList();
   }

   /**
    * Read users for specific group.
    *
    * @param organization
    *       Id of the organization.
    * @param group
    *       Group name.
    * @return The list of user names.
    */
   public List<String> getUsersInGroup(final String organization, final String group) {
      List<DataDocument> users = dataStorage.aggregate(UserGroup.COLLECTION_NAME, dataStorageDialect.usersOfGroupAggregate(organizationFacade.getOrganizationId(organization), group));
      return users.stream().map(u -> u.getString(UserGroup.ATTR_USERS_USER)).collect(Collectors.toList());
   }

   /**
    * Read groups in organization.
    *
    * @param organization
    *       Code of the organization.
    * @return The list of group names.
    */
   public List<String> getGroups(final String organization) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(Group.COLLECTION_NAME, organizationFilterGroups(organization), Collections.singletonList(Group.ATTR_GROUPS));
      return document != null ? document.getArrayList(Group.ATTR_GROUPS, String.class) : Collections.emptyList();
   }

   private DataDocument readUser(final String organization, final String user) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(UserGroup.COLLECTION_NAME, userFilter(organization, user), Collections.singletonList(dataStorageDialect.concatFields(UserGroup.ATTR_USERS, "$")));
      // we got only one subdocument otherwise there was null
      return document != null ? document.getArrayList(UserGroup.ATTR_USERS, DataDocument.class).get(0) : null;
   }

   private DataFilter organizationFilter(final String organization) {
      return dataStorageDialect.fieldValueFilter(UserGroup.ATTR_ORG_ID, organizationFacade.getOrganizationId(organization));
   }

   private DataFilter organizationFilterGroups(final String organization) {
      return dataStorageDialect.fieldValueFilter(Group.ATTR_ORG_ID, organizationFacade.getOrganizationId(organization));
   }

   private DataFilter userFilter(final String organization, final String user) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(UserGroup.ATTR_ORG_ID, organizationFacade.getOrganizationId(organization));
      filter.put(dataStorageDialect.concatFields(UserGroup.ATTR_USERS, UserGroup.ATTR_USERS_USER), user);
      return dataStorageDialect.multipleFieldsValueFilter(filter);
   }

   private String userParam() {
      return dataStorageDialect.concatFields(UserGroup.ATTR_USERS, "$", UserGroup.ATTR_USERS_GROUPS);
   }

}
