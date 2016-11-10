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

   private static final String ERROR_COLLECTION_NOT_FOUND = "The collection \"{0}\" does not exists.";
   private static final String ERROR_COLLECTION_ALREADY_EXISTS = "The collection \"{0}\" already exists.";
   private static final String ERROR_ATTRIBUTE_NOT_FOUND = "The attribute \"{0}\" does not exists in collection \"{1}\".";
   private static final String ERROR_ATTRIBUTE_ALREADY_EXISTS = "The attribute \"{0}\" already exists in collection \"{1}\".";
   private static final String ERROR_DOCUMENT_NOT_FOUND = "Document does not exists.";
   private static final String ERROR_CREATE_UNSUCCESFUL = "The document could not be created.";
   private static final String ERROR_DROP_UNSUCCESFUL = "The document could not be deleted.";
   private static final String ERROR_INVALID_METADATA_KEY = "The key  \"{0}\" is not metadata attribute";

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

   public static String invalidMetadataKey(String key) {
      return MessageFormat.format(ERROR_INVALID_METADATA_KEY, key);
   }

}
