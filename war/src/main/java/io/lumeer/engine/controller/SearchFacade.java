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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.InvalidQueryException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private ConfigurationFacade configurationFacade;

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
    * @throws CollectionNotFoundException
    *       When the collection in which we want to search does not exist. TODO Think about simply returning an empty result.
    */
   public List<DataDocument> search(String collectionName, String filter, String sort, int skip, int limit) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
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

   /**
    * Queries the data storage in a flexible way. Allows for none or multiple collection names to be specified,
    * automatically sets limit to default values.
    *
    * @param query
    *       Query to execute.
    * @return The query result.
    * @throws InvalidQueryException
    *       When it was not possible to execute the query.
    */
   public List<DataDocument> query(final Query query) throws InvalidQueryException {
      final List<String> collections = new ArrayList<>();
      final List<DataDocument> result = new ArrayList<>();
      final Query internalQuery = new Query();

      try {
         for (final String collectionName : query.getCollections()) {
            collections.add(collectionMetadataFacade.getInternalCollectionName(collectionName));
         }

         if (collections.size() == 0) {
            collections.addAll(collectionFacade.getAllCollections().keySet());
         }
      } catch (CollectionNotFoundException e) {
         throw new InvalidQueryException("Search asks for collections that are not available: ", e);
      }

      internalQuery.setFilters(query.getFilters());
      internalQuery.setProjections(query.getProjections());
      internalQuery.setSorting(query.getSorting());

      if (query.getLimit() == null) {
         internalQuery.setLimit(configurationFacade.getConfigurationInteger(LumeerConst.DEFAULT_LIMIT_PROPERTY).orElse(100) / collections.size());
      }

      if (query.getSkip() == null) {
         internalQuery.setSkip(0);
      }

      for (final String collection : collections) {
         internalQuery.setCollections(Collections.singleton(collection));
         result.addAll(dataStorage.query(internalQuery));
      }

      return result;
   }

}
