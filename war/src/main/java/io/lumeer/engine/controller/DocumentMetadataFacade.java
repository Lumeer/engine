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

import java.io.Serializable;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class DocumentMetadataFacade implements Serializable {

   public final String DOCUMENT_METADATA_PREFIX = "meta-";
   public final String DOCUMENT_CREATE_DATE_KEY = DOCUMENT_METADATA_PREFIX + "create-date";
   public final String DOCUMENT_UPDATE_DATE_KEY = DOCUMENT_METADATA_PREFIX + "update-date";
   public final String DOCUMENT_CREATE_BY_USER_KEY = DOCUMENT_METADATA_PREFIX + "create-user";
   public final String DOCUMENT_UPDATED_BY_USER_KEY = DOCUMENT_METADATA_PREFIX + "update-user";
   public final String DOCUMENT_RIGHTS_KEY = DOCUMENT_METADATA_PREFIX + "rights";

   // example of document metadata structure:
   // -------------------------------------
   // {
   //	“_meta-create-date” : date,
   //	“_meta-update-date” : date,
   //	“_meta-creator-user” : user_name,
   //	“_meta-update-user” : user_name,
   //	“_meta-rights” : [
   //      user_name1 : 1  //execute permissions
   //      user_name2 : 2  //write permissions
   //      user_name3 : 4  //read permissions
   //      user_name4 : 3  //execute and write permissions = 1 + 2
   //      user_name5 : 5  //read and execute permissions = 1 + 4
   //      user_name6 : 6  //write and read permissions = 2 + 4
   //      user_name7 : 7  //full permissions = 1 + 2 + 4
   //      others     : 0  //others is forced to be there, maybe if not presented, that means 0 permissions
   //    ],
   // “_meta-group-rights” : [
   //      group_name1: 1  //execute permissions
   //      group_name2: 2  //write permissions
   //      group_name3: 4  //read permissions
   //      group_name4: 3  //execute and write permissions = 1 + 2
   //      group_name5: 5  //read and execute permissions = 1 + 4
   //      group_name6: 6  //write and read permissions = 2 + 4
   //      group_name7: 7  //full permissions = 1 + 2 + 4
   //	   ]
   //	… (rest of the document) …
   // }

}
