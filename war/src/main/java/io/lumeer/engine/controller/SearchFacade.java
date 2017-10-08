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

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataSort;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.InvalidQueryException;

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
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private SecurityFacade securityFacade;

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
   public List<DataDocument> search(final String collectionName, final DataFilter filter, final DataSort sort, int skip, int limit) throws CollectionNotFoundException {
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
   public List<DataDocument> search(final String query) {
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

      collections.addAll(query.getCollections());
      if (collections.size() == 0) {
         collections.addAll(collectionMetadataFacade.getCollectionsCodeName().values());
      }

      internalQuery.setProjections(query.getProjections());
      internalQuery.setSorting(query.getSorting());

      if (query.getLimit() == null) {
         internalQuery.setLimit(configurationFacade.getConfigurationInteger(LumeerConst.DEFAULT_LIMIT_PROPERTY).orElse(100));
      } else {
         internalQuery.setLimit(query.getLimit());
      }

      if (query.getSkip() == null) {
         internalQuery.setSkip(0);
      } else {
         internalQuery.setSkip(query.getSkip());
      }

      for (final String collection : collections) {
         internalQuery.setCollections(Collections.singleton(collection));
         dataStorage.query(internalQuery).stream().forEach(d -> {
            d.put(LumeerConst.Document.COLLECTION_NAME, collection);
            result.add(d);
         });
      }

      return result;
   }

}
