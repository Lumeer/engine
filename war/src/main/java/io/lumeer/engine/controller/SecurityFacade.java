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

import io.lumeer.engine.api.data.DataDocument;

import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class SecurityFacade {

   public boolean checkForRead(DataDocument dataDocument, String userName) {
      return true;
   }

   public boolean checkForWrite(DataDocument dataDocument, String userName) {
      return true;
   }

   public boolean checkForExecute(DataDocument dataDocument, String userName) {
      return true;
   }

   public boolean checkForAddRights(DataDocument dataDocument, String userName) {
      return true;
   }

   public boolean checkForRead(String collectionName, String documentId, String userName) {
      return true;
   }

   public boolean checkForWrite(String collectionName, String documentId, String userName) {
      return true;
   }

   public boolean checkForExecute(String collectionName, String documentId, String userName) {
      return true;
   }

   public boolean checkForAddRights(String collectionName, String documentId, String userName) {
      return true;
   }
}
