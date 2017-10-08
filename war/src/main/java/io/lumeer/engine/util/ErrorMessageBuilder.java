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
package io.lumeer.engine.util;

import java.text.MessageFormat;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
public class ErrorMessageBuilder {

   private ErrorMessageBuilder() {
      // to prevent initialization
   }

   private static final String ERROR_COLLECTION_NOT_FOUND = "The collection \"{0}\" does not exist.";
   private static final String ERROR_ATTRIBUTE_NOT_FOUND_IN_COLLECTION = "The attribute \"{0}\" does not exist in collection \"{1}\".";
   private static final String ERROR_ID_NOT_FOUND = "The id does not exist in document";
   private static final String ERROR_ATTRIBUTE_ALREADY_EXISTS = "The attribute \"{0}\" already exists in collection \"{1}\".";
   private static final String ERROR_DOCUMENT_NOT_FOUND = "Document does not exist.";
   private static final String ERROR_CREATE_UNSUCCESFUL = "The document could not be created.";
   private static final String ERROR_DROP_UNSUCCESFUL = "The document could not be deleted.";
   private static final String ERROR_UPDATE_UNSUCCESFUL = "The document was not successfully updated.";
   private static final String ERROR_INVALID_METADATA_KEY = "The key  \"{0}\" is not metadata attribute";
   private static final String ERROR_INVALID_DOCUMENT_KEY = "The key  \"{0}\" is not valid";
   private static final String ERROR_INVALID_CONSTRAINT_KEY = "Invalid value for attribute: \"{0}\"";
   private static final String ERROR_NULL_KEY = "The key can not be set to 'null'";
   private static final String ERROR_LINK_ALREADY_EXISTS = "LinkInstance between documents already exists";
   private static final String ERROR_PARAM_CANNOT_BE_NULL = "The param  \"{0}\" can not be null.";

   private static final String ERROR_USER_COLLECTION_NOT_FOUND = "The user collection \"{0}\" does not exist.";
   private static final String ERROR_USER_COLLECTION_ALREADY_EXISTS = "The user collection \"{0}\" already exists.";
   private static final String ERROR_COLLECTION_METADATA_NOT_FOUND = "The metadata for collection \"{0}\" does not exist.";

   private static final String ERROR_VIEW_USERNAME_ALREADY_EXISTS = "The view with username \"{0}\" already exists.";

   private static final String ERROR_USER_ALREADY_EXISTS_IN_PROJECT = "User \"{0}\" already exists in project \"{1}\"";
   private static final String ERROR_USER_ALREADY_EXISTS_IN_ORGANIZATION = "User \"{0}\" already exists in organization \"{1}\"";

   private static final String ERROR_ORGANIZATION_DOESNT_EXIST = "Organization \"{0}\" doesn't exist";
   private static final String ERROR_PROJECT_DOESNT_EXIST = "Project \"{0}\" doesn't exist in organization \"{1}\"";

   public static String organizationDoesntExist(String orgCode) {
      return MessageFormat.format(ERROR_ORGANIZATION_DOESNT_EXIST, orgCode);
   }

   public static String projectDoesntExist(String orgCode, String projCode) {
      return MessageFormat.format(ERROR_PROJECT_DOESNT_EXIST, projCode, orgCode);
   }

   public static String collectionNotFoundString(String collection) {
      return MessageFormat.format(ERROR_COLLECTION_NOT_FOUND, collection);
   }

   public static String attributeNotFoundInColString(String attribute, String collection) {
      return MessageFormat.format(ERROR_ATTRIBUTE_NOT_FOUND_IN_COLLECTION, attribute, collection);
   }

   public static String attributeAlreadyExistsString(String attribute, String collection) {
      return MessageFormat.format(ERROR_ATTRIBUTE_ALREADY_EXISTS, attribute, collection);
   }

   public static String documentNotFoundString() {
      return ERROR_DOCUMENT_NOT_FOUND;
   }

   public static String dropDocumentUnsuccesfulString() {
      return ERROR_DROP_UNSUCCESFUL;
   }

   public static String createDocumentUnsuccesfulString() {
      return ERROR_CREATE_UNSUCCESFUL;
   }

   public static String updateDocumentUnsuccesfulString() {
      return ERROR_UPDATE_UNSUCCESFUL;
   }

   public static String invalidMetadataKeyString(String key) {
      return MessageFormat.format(ERROR_INVALID_METADATA_KEY, key);
   }

   public static String invalidDocumentKeyString(String key) {
      return MessageFormat.format(ERROR_INVALID_DOCUMENT_KEY, key);
   }

   public static String invalidConstraintKeyString(String key) {
      return MessageFormat.format(ERROR_INVALID_CONSTRAINT_KEY, key);
   }

   public static String paramCanNotBeNullString(String key) {
      return MessageFormat.format(ERROR_PARAM_CANNOT_BE_NULL, key);
   }

   public static String nullKeyString() {
      return ERROR_NULL_KEY;
   }

   public static String linkAlreadyExistsString() {
      return ERROR_LINK_ALREADY_EXISTS;
   }

   public static String userCollectionNotFoundString(String collection) {
      return MessageFormat.format(ERROR_USER_COLLECTION_NOT_FOUND, collection);
   }

   public static String userCollectionAlreadyExistsString(String collection) {
      return MessageFormat.format(ERROR_USER_COLLECTION_ALREADY_EXISTS, collection);
   }

   public static String collectionMetadataNotFoundString(String collection) {
      return MessageFormat.format(ERROR_COLLECTION_METADATA_NOT_FOUND, collection);
   }

   public static String viewUsernameAlreadyExistsString(String viewName) {
      return MessageFormat.format(ERROR_VIEW_USERNAME_ALREADY_EXISTS, viewName);
   }

   public static String idNotFoundString() {
      return ERROR_ID_NOT_FOUND;
   }

   public static String userAlreadyExistsInProjectString(String userName, String projectId) {
      return MessageFormat.format(ERROR_USER_ALREADY_EXISTS_IN_PROJECT, userName, projectId);
   }

   public static String userAlreadyExistsInOrganizationString(String userName, String organizationId) {
      return MessageFormat.format(ERROR_USER_ALREADY_EXISTS_IN_ORGANIZATION, userName, organizationId);
   }
}
