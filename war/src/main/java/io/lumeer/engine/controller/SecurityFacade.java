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
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.rest.dao.AccessRightsDao;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

   /********* CHECKING ROLES *********/

   public boolean hasOrganizationRole(String organizationId, String role) {
      DataDocument doc = systemDataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationIdentificator(organizationId)),
            Arrays.asList(LumeerConst.Security.ROLES_KEY));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationId);
   }

   public boolean hasProjectRole(String projectId, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectId)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationId());
   }

   public boolean hasCollectionRole(String projectId, String collectionName, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(collectionFilter(projectId, collectionName)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationId());
   }

   public boolean hasViewRole(String projectId, int viewId, String role) {
      DataDocument doc = dataStorage.readDocumentIncludeAttrs(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(viewFilter(projectId, viewId)),
            Arrays.asList(dataStorageDialect.concatFields(LumeerConst.Security.ROLES_KEY, role)));

      return checkRole(doc, userFacade.getUserEmail(), role, organizationFacade.getOrganizationId());
   }

   private boolean checkRole(DataDocument rolesDocument, String user, String role, String organizationCode) {
      List<String> groupsForUser = userGroupFacade.getGroupsOfUser(organizationFacade.getOrganizationIdentificator(organizationCode), user);
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

   public void addOrganizationUserRole(String organizationId, String user, String role) {
      addOrganizationRole(organizationId, user, null, role);
   }

   public void addOrganizationGroupRole(String organizationId, String group, String role) {
      addOrganizationRole(organizationId, null, group, role);
   }

   private void addOrganizationRole(String organizationCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      systemDataStorage.addItemToArray(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationIdentificator(organizationCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void addProjectUserRole(String projectId, String user, String role) {
      addProjectRole(projectId, user, null, role);
   }

   public void addProjectGroupRole(String projectId, String group, String role) {
      addProjectRole(projectId, null, group, role);
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

   public void addCollectionUserRole(String projectId, String collectionName, String user, String role) {
      addCollectionRole(collectionName, projectId, user, null, role);
   }

   public void addCollectionGroupRole(String projectId, String collectionName, String group, String role) {
      addCollectionRole(collectionName, projectId, null, group, role);
   }

   private void addCollectionRole(String collectionName, String projectId, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = collectionFilter(projectId, collectionName);

      dataStorage.addItemToArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void addViewUserRole(String projectId, int viewId, String user, String role) {
      addViewRole(viewId, projectId, user, null, role);
   }

   public void addViewGroupRole(String projectId, int viewId, String group, String role) {
      addViewRole(viewId, projectId, null, group, role);
   }

   private void addViewRole(int viewId, String projectId, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = viewFilter(projectId, viewId);

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

   public void removeOrganizationUserRole(String organizationId, String user, String role) {
      removeOrganizationRole(organizationId, user, null, role);
   }

   public void removeOrganizationGroupRole(String organizationId, String group, String role) {
      removeOrganizationRole(organizationId, null, group, role);
   }

   private void removeOrganizationRole(String organizationCode, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      systemDataStorage.removeItemFromArray(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.fieldValueFilter(LumeerConst.Security.ORGANIZATION_ID_KEY, organizationFacade.getOrganizationIdentificator(organizationCode)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void removeProjectUserRole(String projectId, String user, String role) {
      removeProjectRole(projectId, user, null, role);
   }

   public void removeProjectGroupRole(String projectId, String group, String role) {
      removeProjectRole(projectId, null, group, role);
   }

   private void removeProjectRole(String projectId, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      dataStorage.removeItemFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(projectFilter(projectId)),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void removeCollectionUserRole(String projectId, String collectionName, String user, String role) {
      removeCollectionRole(projectId, collectionName, user, null, role);
   }

   public void removeCollectionGroupRole(String projectId, String collectionName, String group, String role) {
      removeCollectionRole(projectId, collectionName, null, group, role);
   }

   private void removeCollectionRole(String projectId, String collectionName, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = collectionFilter(projectId, collectionName);

      dataStorage.removeItemFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   public void removeViewUserRole(String projectId, int viewId, String user, String role) {
      removeViewRole(projectId, viewId, user, null, role);
   }

   public void removeViewGroupRole(String projectId, int viewId, String group, String role) {
      removeViewRole(projectId, viewId, null, group, role);
   }

   private void removeViewRole(String projectId, int viewId, String user, String group, String role) {
      String userOrGroupKey = userOrGroupKey(user);
      String userOrGroupName = userOrGroupName(user, group);

      Map<String, Object> filter = viewFilter(projectId, viewId);

      dataStorage.removeItemFromArray(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.multipleFieldsValueFilter(filter),
            dataStorageDialect.concatFields(
                  LumeerConst.Security.ROLES_KEY,
                  role,
                  userOrGroupKey),
            userOrGroupName);
   }

   private Map<String, Object> projectFilter(String projectId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectIdentificator(projectId));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_PROJECT);
      return filter;
   }

   private Map<String, Object> collectionFilter(String projectId, String collectionName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectIdentificator(projectId));
      filter.put(LumeerConst.Security.TYPE_KEY, LumeerConst.Security.TYPE_COLLECTION);
      filter.put(LumeerConst.Security.COLLECTION_NAME_KEY, collectionName);
      return filter;
   }

   private Map<String, Object> viewFilter(String projectId, int viewId) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Security.PROJECT_ID_KEY, projectFacade.getProjectIdentificator(projectId));
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

   /********* OLD **********/

   @Inject
   private UserFacade user;

   @Inject
   private DocumentMetadataFacade dmf;

   private final int READ_BP = 2;
   private final int WRITE_BP = 1;
   private final int EXECUTE_BP = 0;
   private final int NO_RIGHTS = -1;
   private final int NOT_FOUND = 0;

   /* change EMPTY_LIST to 0 to specify no rights if no user is presented in array of rights but list exists
      change EMPTY_LIST to -1 to specify all rights for all user if no user is presented in array of rights but list exists
    */
   private final int EMPTY_LIST = 0;
   private final int NULL_LIST = -1;

   // TODO add users group ...
   private DataDocument readGroupRights(DataDocument dataDocument) {
      return null;
   }

   private boolean checkBit(int inBit, int bit) {
      if (inBit == NO_RIGHTS) {
         return true;
      }

      if (inBit == NOT_FOUND) {
         return false;
      }
      if ((inBit >> bit & 1) == 1) {
         return true;
      }
      return false;
   }

   /**
    * Check in datadocument for userName if can read.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can read this document
    */
   public boolean checkForRead(DataDocument dataDocument, String userName) {
      return checkBit(recordValue(dataDocument, userName), READ_BP);
   }

   /**
    * Check in datadocument for userName if can write.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can write to this document
    */
   public boolean checkForWrite(DataDocument dataDocument, String userName) {
      return checkBit(recordValue(dataDocument, userName), WRITE_BP);
   }

   /**
    * Check in datadocument for userName if can execute.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can execute this document
    */
   public boolean checkForExecute(DataDocument dataDocument, String userName) {
      return checkBit(recordValue(dataDocument, userName), EXECUTE_BP);
   }

   /**
    * Check in datadocument for userName if can add rights - same as execute.
    *
    * @param dataDocument
    *       document to check
    * @param userName
    *       user name to check
    * @return return true if user can add rights this document
    */
   public boolean checkForAddRights(DataDocument dataDocument, String userName) {
      if (dataDocument.containsKey(LumeerConst.Document.CREATE_BY_USER_KEY)) {
         if (dataDocument.getString(LumeerConst.Document.CREATE_BY_USER_KEY).equals(userName)) {
            return true;
         }
      }
      return checkForExecute(dataDocument, userName);
   }

   private List<String> buildMetaList() {
      return Arrays.asList(LumeerConst.Document.CREATE_BY_USER_KEY, LumeerConst.Document.USER_RIGHTS);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can read this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can read this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForRead(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, dataStorageDialect.documentIdFilter(documentId), buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return checkForRead(dataDoc, userName);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can write to this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can write to this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForWrite(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, dataStorageDialect.documentIdFilter(documentId), buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return checkForWrite(dataDoc, userName);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can execute this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can execute this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForExecute(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, dataStorageDialect.documentIdFilter(documentId), buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return checkForExecute(dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId)), userName);
   }

   /**
    * Read document from collectionName with specified documentId
    * and check if user can add rights to this document.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param userName
    *       user name for checking
    * @return return true if user can add rights to this document
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public boolean checkForAddRights(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDoc = dataStorage.readDocumentIncludeAttrs(collectionName, dataStorageDialect.documentIdFilter(documentId), buildMetaList());
      if (dataDoc == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (dataDoc.containsKey(LumeerConst.Document.CREATE_BY_USER_KEY)) {
         if (dataDoc.getString(LumeerConst.Document.CREATE_BY_USER_KEY).equals(userName)) {
            return true;
         }
      }
      return checkForExecute(collectionName, documentId, userName);
   }

   /**
    * Set read rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument setRightsRead(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (!checkForRead(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, LumeerConst.Security.READ, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean setRightsRead(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(documentId);
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentIdFilter);
      setRightsRead(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentIdFilter);
      return checkForRead(collectionName, documentId, userName);
   }

   /**
    * Set write rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument setRightsWrite(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (!checkForWrite(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, LumeerConst.Security.WRITE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean setRightsWrite(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(documentId);
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentIdFilter);
      setRightsWrite(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentIdFilter);
      return checkForWrite(collectionName, documentId, userName);
   }

   /**
    * Set execute rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument setRightsExecute(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (!checkForExecute(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, LumeerConst.Security.EXECUTE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Set rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean setRightsExecute(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(documentId);
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentIdFilter);
      setRightsExecute(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, documentIdFilter);
      return checkForExecute(collectionName, documentId, userName);
   }

   /**
    * Remove execute rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument removeRightsExecute(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (checkForExecute(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, (-1) * LumeerConst.Security.EXECUTE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove rights for execute in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean removeRightsExecute(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      removeRightsExecute(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, dataStorageDialect.documentIdFilter(documentId));
      return !checkForExecute(collectionName, documentId, userName);
   }

   /**
    * Remove write rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument removeRightsWrite(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (checkForWrite(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, (-1) * LumeerConst.Security.WRITE, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove rights for write in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean removeRightsWrite(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      removeRightsWrite(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, dataStorageDialect.documentIdFilter(documentId));
      return !checkForWrite(collectionName, documentId, userName);
   }

   /**
    * Remove read rights to dataDocument for userName.
    *
    * @param dataDocument
    *       dataDocument where rights are set
    * @param userName
    *       user name to set rights
    * @return return dataDocument with rights speficied
    */
   public DataDocument removeRightsRead(DataDocument dataDocument, String userName) {
      int value = recordValue(dataDocument, userName);
      if (checkForAddRights(dataDocument, user.getUserEmail())) {
         if (checkForRead(dataDocument, userName) || (value == NO_RIGHTS)) {
            setRights(dataDocument, (-1) * LumeerConst.Security.READ, userName, value);
         }
      }
      return dataDocument;
   }

   /**
    * Remove rights for read in database.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @param userName
    *       username of user rights
    * @return true if update successful
    * @throws DocumentNotFoundException
    *       throws if document not found in database
    */
   public boolean removeRightsRead(String collectionName, String documentId, String userName) throws DocumentNotFoundException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      removeRightsRead(dataDocument, userName);
      dataStorage.updateDocument(collectionName, dataDocument, dataStorageDialect.documentIdFilter(documentId));
      return !checkForRead(collectionName, documentId, userName);
   }

   private boolean checkMetadata(DataDocument dataDocument) {
      return dataDocument.containsKey(LumeerConst.Document.USER_RIGHTS);
   }

   public void addMetaData(DataDocument dataDocument) {
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, Collections.emptyList());
   }

   private List<DataDocument> readList(DataDocument dataDocument) {
      if (!(dataDocument.containsKey(LumeerConst.Document.USER_RIGHTS))) {
         return null;
      }
      return dataDocument.getArrayList(LumeerConst.Document.USER_RIGHTS, DataDocument.class);
   }

   private int recordValue(DataDocument dataDocument, String email) {
      List<DataDocument> arrayList = readList(dataDocument);
      if (arrayList == null) {
         return NULL_LIST;
      }
      if (arrayList.size() == 0) {
         return EMPTY_LIST;
      }
      for (DataDocument dataDoc : arrayList) {
         if (dataDoc.containsValue(email)) {
            return dataDoc.getInteger(LumeerConst.Security.RULE);
         }
      }
      return 0;
   }

   private void replaceInList(DataDocument dataDocument, String email, int newInteger) {
      List<DataDocument> arrayList = readList(dataDocument);
      if (arrayList == null) {
         return;
      }
      for (DataDocument datadoc : arrayList) {
         if (datadoc.containsValue(email)) {
            datadoc.replace(LumeerConst.Security.RULE, newInteger);
         }
      }
      dataDocument.replace(LumeerConst.Document.USER_RIGHTS, arrayList);
   }

   private void putToList(DataDocument dataDocument, String email, int rights) {
      List<DataDocument> arrayList = readList(dataDocument);
      if (arrayList == null) {
         return;
      }
      DataDocument newRule = new DataDocument(LumeerConst.Security.USER_ID, email)
            .append(LumeerConst.Security.RULE, rights);
      arrayList.add(newRule);
      dataDocument.replace(LumeerConst.Document.USER_RIGHTS, arrayList);
   }

   private void setRights(DataDocument dataDocument, int addRights, String userName, int rightsInteger) {
      if (!checkMetadata(dataDocument)) {
         addMetaData(dataDocument);
         rightsInteger = NOT_FOUND;
      }
      if (rightsInteger != NOT_FOUND) {
         int newRights = rightsInteger + addRights;
         if ((newRights <= 7) && (newRights >= 0)) {
            replaceInList(dataDocument, userName, newRights);
            //dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).replace(userName, newRights);
         }
      } else {
         putToList(dataDocument, userName, addRights);
         //dataDocument.getDataDocument(LumeerConst.Document.USER_RIGHTS).put(userName, addRights);
      }
   }

   /**
    * Sets full rights to document internally
    *
    * @param dataDocument
    *       document to set rigthts
    * @param userEmail
    *       user email which identify one user
    */
   public void putFullRightsInternally(DataDocument dataDocument, String userEmail) {
      dataDocument.put(LumeerConst.Document.USER_RIGHTS, Collections.singletonList(new DataDocument(LumeerConst.Security.USER_ID, userEmail).append(LumeerConst.Security.RULE, LumeerConst.Security.WRITE + LumeerConst.Security.EXECUTE + LumeerConst.Security.READ)));
   }

   /**
    * Read one integer as access rule of document
    *
    * @param dataDocument
    *       document where this integer is stored
    * @param email
    *       user email which identify one user
    * @return integer as rule of access (linux system rule)
    */
   public int readRightInteger(DataDocument dataDocument, String email) {
      return recordValue(dataDocument, email);
   }

   /**
    * Read all rules of access list and return in as hashmap
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @return return hashmap of all rules
    */
   public Map<String, Integer> readRightsMap(String collectionName, String documentId) {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      return readRightsMap(dataDocument);
   }

   public Map<String, Integer> readRightsMap(DataDocument dataDocument) {
      List<DataDocument> arrayList = readList(dataDocument);
      if (arrayList == null) {
         return Collections.emptyMap();
      }
      HashMap<String, Integer> map = new HashMap<>();
      for (DataDocument dataDoc : arrayList) {
         map.put(dataDoc.getString(LumeerConst.Security.USER_ID), dataDoc.getInteger(LumeerConst.Security.RULE));
      }
      return map;
   }

   /**
    * Return data access object of access rights.
    *
    * @param dataDocument
    *       where access rights are stored
    * @param email
    *       user email (identificator)
    * @return data access object
    */
   public AccessRightsDao getDao(DataDocument dataDocument, String email) {
      return new AccessRightsDao(checkForRead(dataDocument, email), checkForWrite(dataDocument, email), checkForExecute(dataDocument, email), email);
   }

   /**
    * Return Data access object of Access Rights type from collection.
    *
    * @param collectionName
    *       Collection name where datadocument is stored
    * @param documentId
    *       Document id
    * @return data access object.
    */
   public List<AccessRightsDao> getDaoList(String collectionName, String documentId) {
      DataDocument dataDoc = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      return getDaoList(dataDoc);
   }

   /**
    * Return Data access object of Access Rights type from datadocument.
    *
    * @param dataDocument
    *       dataDocument, from which dao wanted
    * @return data access object of access rights type
    */
   public List<AccessRightsDao> getDaoList(DataDocument dataDocument) {
      Map<String, Integer> hashMap = readRightsMap(dataDocument);
      List<AccessRightsDao> list = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
         boolean read = checkForRead(dataDocument, entry.getKey());
         boolean write = checkForWrite(dataDocument, entry.getKey());
         boolean execute = checkForExecute(dataDocument, entry.getKey());
         AccessRightsDao ard = new AccessRightsDao(read, write, execute, entry.getKey());
         list.add(ard);
      }
      return list;
   }

   /**
    * Return data access object of access rights.
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param email
    *       user email
    * @return data access object
    */
   public AccessRightsDao getDao(String collectionName, String documentId, String email) {
      return getDao(dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId)), email);
   }

   /**
    * This is not tested and maybe not stable
    *
    * @param collectionName
    *       collection name where document is stored
    * @param documentId
    *       document id
    * @param email
    *       user email
    * @return data access object
    * @throws CollectionNotFoundException
    *       if collection not found
    * @throws DocumentNotFoundException
    *       if document not found
    */
   public AccessRightsDao getDaoCached(String collectionName, String documentId, String email) throws CollectionNotFoundException, DocumentNotFoundException {
      return getDao((DataDocument) dmf.readDocumentMetadata(collectionName, documentId), email);
   }

   /**
    * Set rights to be same as data access object
    *
    * @param dataDocument
    *       data document where rights to be set
    * @param accessRightsDao
    *       how data should see
    * @return data document with set rights
    */
   public DataDocument setDao(DataDocument dataDocument, AccessRightsDao accessRightsDao) {
      if (accessRightsDao.isExecute()) {
         setRightsExecute(dataDocument, accessRightsDao.getUserName());
      } else {
         removeRightsExecute(dataDocument, accessRightsDao.getUserName());
      }
      if (accessRightsDao.isRead()) {
         setRightsRead(dataDocument, accessRightsDao.getUserName());
      } else {
         removeRightsRead(dataDocument, accessRightsDao.getUserName());
      }
      if (accessRightsDao.isWrite()) {
         setRightsWrite(dataDocument, accessRightsDao.getUserName());
      } else {
         removeRightsWrite(dataDocument, accessRightsDao.getUserName());
      }
      return dataDocument;
   }

   /**
    * Set rights to be same as data access object.
    * This is not atomic!
    *
    * @param collectionName
    *       collection name where data document is
    * @param documentId
    *       data document id
    * @param accessRightsDao
    *       data access object
    */
   public void setDao(String collectionName, String documentId, AccessRightsDao accessRightsDao) {
      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(documentId);
      dataStorage.updateDocument(collectionName, setDao(dataStorage.readDocument(collectionName, documentIdFilter), accessRightsDao), documentIdFilter);
   }

   /**
    * Set rights to be same as data access object with
    * check. Can be slow. This is not atomic!
    *
    * @param collectionName
    *       collection name where data document is
    * @param documentId
    *       data document id
    * @param accessRightsDao
    *       data access object
    * @return true if all data was updated successful
    */
   public boolean setDaoCheck(String collectionName, String documentId, AccessRightsDao accessRightsDao) {
      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(documentId);
      dataStorage.updateDocument(collectionName, setDao(dataStorage.readDocument(collectionName, documentIdFilter), accessRightsDao), documentIdFilter);
      DataDocument dataDoc = dataStorage.readDocument(collectionName, documentIdFilter);
      return (accessRightsDao.isWrite() == checkForWrite(dataDoc, accessRightsDao.getUserName()))
            & (accessRightsDao.isRead() == checkForRead(dataDoc, accessRightsDao.getUserName()))
            & (accessRightsDao.isExecute() == checkForExecute(dataDoc, accessRightsDao.getUserName()));
   }

   /**
    * Return query string for search and limit user
    * without rights.
    * Example: {"_meta-rights" : { $elemMatch: { "user_email" : "test@gmail.com", "rule" : {$gte : 4}} } }.
    *
    * @param email
    *       email of username
    * @return return string as in example
    */
   public String readQueryString(String email) {
      // GENERATE: ....find(  {"_meta-rights" : { $elemMatch: { "user_email" : "test@gmail.com", "rule" : {$gte : 4}} } })
      return "{\"" + LumeerConst.Document.USER_RIGHTS + "\" : { $elemMatch: { \"" + LumeerConst.Document.CREATE_BY_USER_KEY + "\" : \"" + email + "\", \"rule\" : {$gte : 4}}} }";
   }

   /**
    * Returns a filter part for a query that limits search results only for the documents where the current user has
    * read rights.
    *
    * @return The query filter to limit returned documents only to those where the current user has read rights.
    */
   public DataDocument getReadRightsQueryFilter() {
      return new DataDocument("_meta-rights", new DataDocument("$elemMatch", new DataDocument("user_email", user.getUserEmail()).append("rule", new DataDocument("$gte", 4))));
   }

   public String writeQueryString(String email) {
      // GENERATE: ....find(  {"_meta-rights" : { $elemMatch: { "user_email" : "test@gmail.com", "rule" : {$in : [2,3,6,7]}} } })
      return "{\"" + LumeerConst.Document.USER_RIGHTS + "\" : { $elemMatch: { \"" + LumeerConst.Document.CREATE_BY_USER_KEY + "\" : \"" + email + "\", \"rule\" : {$in : [6,7]}}} }";
   }
}
