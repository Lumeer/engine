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
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@SessionScoped
public class SearchFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionFacade collectionFacade;

   /**
    * Searches the specified collection for specified documents using filter, sort, skip and limit option.
    *
    * @param collectionName
    *       the name of the collection where the run will be performed
    * @param filter
    *       the query predicate. If unspecified, then all documents in the collection will match the predicate.
    * @param sort
    *       the sort specification for the ordering of the results
    * @param skip
    *       the number of documents to skip
    * @param limit
    *       the maximum number of documents to return
    * @return the list of the found documents
    */
   public List<DataDocument> search(String collectionName, String filter, String sort, int skip, int limit) throws CollectionNotFoundException {
      if (!collectionFacade.isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      return dataStorage.search(collectionName, filter, sort, skip, limit);
   }

   /**
    * Executes a query to find and return documents.
    *
    * @param query
    *       the database find command specified as a JSON string
    * @return the list of the found documents
    * @see <a href="https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find">https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find</a>
    */
   public List<DataDocument> search(String query) {
      return dataStorage.run(query);
   }

   public List<DataDocument> query(final Query query) {
      return null;
   }

}
