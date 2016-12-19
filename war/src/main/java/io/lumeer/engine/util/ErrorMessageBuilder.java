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
   private static final String ERROR_COLLECTION_ALREADY_EXISTS = "The collection \"{0}\" already exists.";
   private static final String ERROR_ATTRIBUTE_NOT_FOUND = "The attribute \"{0}\" does not exist in collection \"{1}\".";
   private static final String ERROR_ATTRIBUTE_ALREADY_EXISTS = "The attribute \"{0}\" already exists in collection \"{1}\".";
   private static final String ERROR_DOCUMENT_NOT_FOUND = "Document does not exist.";
   private static final String ERROR_CREATE_UNSUCCESFUL = "The document could not be created.";
   private static final String ERROR_DROP_UNSUCCESFUL = "The document could not be deleted.";
   private static final String ERROR_UPDATE_UNSUCCESFUL = "The document was not successfully updated.";
   private static final String ERROR_INVALID_METADATA_KEY = "The key  \"{0}\" is not metadata attribute";
   private static final String ERROR_INVALID_DOCUMENT_KEY = "The key  \"{0}\" is not valid";
   private static final String ERROR_INVALID_CONSTRAINT_KEY = "Invalid value for attribute: \"{0}\"";
   private static final String ERROR_NULL_KEY = "The key can not be set to 'null'";
   private static final String ERROR_LINK_ALREADY_EXISTS = "Link between documents already exists";
   private static final String ERROR_PARAM_CANNOT_BE_NULL = "The param  \"{0}\" can not be null.";

   private static final String ERROR_USER_COLLECTION_NOT_FOUND = "The user collection \"{0}\" does not exist.";
   private static final String ERROR_USER_COLLECTION_ALREADY_EXISTS = "The user collection \"{0}\" already exists.";
   private static final String ERROR_ATTRIBUTE_METADATA_DOCUMENT_NOT_FOUND = "The metadata document for attribute \"{0}\" in collection \"{1}\" does not exist.";
   private static final String ERROR_ATTRIBUTE_METADATA_NOT_FOUND = "The metadata \"{0}\" for attribute \"{1}\" in collection \"{2}\" does not exist.";
   private static final String ERROR_COLLECTION_METADATA_NOT_FOUND = "The metadata \"{0}\" for collection \"{1}\" does not exist.";

   private static final String ERROR_VIEW_METADATA_NOT_FOUND = "The metadata for view \"{0}\" does not exist.";
   private static final String ERROR_VIEW_METADATA_VALUE_NOT_FOUND = "The metadata value for key \"{1}\" for view \"{0}\" does not exist.";
   private static final String ERROR_VIEW_USERNAME_ALREADY_EXISTS = "The view with username \"{0}\" already exists.";
   private static final String ERROR_VIEW_META_IMMUTABLE = "The metadata key \"{1}\" for view \"{0}\" cannot be changed.";
   private static final String ERROR_VIEW_META_SPECIAL = "The metadata key \"{1}\" for view \"{0}\" can be changed only with special method.";

   public static String collectionNotFoundString(String collection) {
      return MessageFormat.format(ERROR_COLLECTION_NOT_FOUND, collection);
   }

   public static String collectionAlreadyExistsString(String collection) {
      return MessageFormat.format(ERROR_COLLECTION_ALREADY_EXISTS, collection);
   }

   public static String attributeNotFoundString(String attribute, String collection) {
      return MessageFormat.format(ERROR_ATTRIBUTE_NOT_FOUND, attribute, collection);
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

   public static String invalidMetadataKey(String key) {
      return MessageFormat.format(ERROR_INVALID_METADATA_KEY, key);
   }

   public static String invalidDocumentKey(String key) {
      return MessageFormat.format(ERROR_INVALID_DOCUMENT_KEY, key);
   }

   public static String invalidConstraintKey(String key) {
      return MessageFormat.format(ERROR_INVALID_CONSTRAINT_KEY, key);
   }

   public static String paramCanNotBeNull(String key) {
      return MessageFormat.format(ERROR_PARAM_CANNOT_BE_NULL, key);
   }


   public static String nullKey() {
      return ERROR_NULL_KEY;
   }

   public static String linkAlreadyExists() {
      return ERROR_LINK_ALREADY_EXISTS;
   }

   public static String userCollectionNotFoundString(String collection) {
      return MessageFormat.format(ERROR_USER_COLLECTION_NOT_FOUND, collection);
   }

   public static String userCollectionAlreadyExistsString(String collection) {
      return MessageFormat.format(ERROR_USER_COLLECTION_ALREADY_EXISTS, collection);
   }

   public static String attributeMetadataDocumentNotFoundString(String collection, String attribute) {
      return MessageFormat.format(ERROR_ATTRIBUTE_METADATA_DOCUMENT_NOT_FOUND, attribute, collection);
   }

   public static String attributeMetadataNotFoundString(String collection, String attribute, String metadataType) {
      return MessageFormat.format(ERROR_ATTRIBUTE_METADATA_NOT_FOUND, metadataType, attribute, collection);
   }

   public static String collectionMetadataNotFoundString(String collection, String metadataType) {
      return MessageFormat.format(ERROR_COLLECTION_METADATA_NOT_FOUND, metadataType, collection);
   }

   public static String viewMetadataNotFoundString(int viewId) {
      return MessageFormat.format(ERROR_VIEW_METADATA_NOT_FOUND, viewId);
   }

   public static String viewMetadataValueNotFoundString(int viewId, String valueKey) {
      return MessageFormat.format(ERROR_VIEW_METADATA_VALUE_NOT_FOUND, viewId, valueKey);
   }

   public static String viewUsernameAlreadyExistsString(String viewName) {
      return MessageFormat.format(ERROR_VIEW_USERNAME_ALREADY_EXISTS, viewName);
   }

   public static String viewMetaImmutableString(int viewId, String metaKey) {
      return MessageFormat.format(ERROR_VIEW_META_IMMUTABLE, viewId, metaKey);
   }

   public static String viewMetaSpecialString(int viewId, String metaKey) {
      return MessageFormat.format(ERROR_VIEW_META_SPECIAL, viewId, metaKey);
   }

}
