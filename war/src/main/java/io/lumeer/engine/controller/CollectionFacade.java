/*
 * -----------------------------------------------------------------------\
 * Lummer
 *  
 * Copyright (C) 2016 the original author or authors.
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

import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.CollectionEvent;

import java.io.Serializable;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Named
@SessionScoped
public class CollectionFacade implements Serializable {

   private static final long serialVersionUID = 8967474543742743308L;

   @Inject
   private DataStorage dataStorage;

   private List<String> collections;

   @Produces
   @Named("userCollections")
   public List<String> getAllCollections() {
      if (collections == null) {
         collections = dataStorage.getAllCollections(); // TODO: filter out metadata collections
      }

      return collections;
   }

   public void createCollection(final String collectionName) {
      dataStorage.createCollection(collectionName);
      // TODO: create metadata collection

      collections = null;
   }

   public void dropCollection(final String collectionName) {

   }

   /*public List<CollectionMetadataElement> readCollectionMetadata(final String collectionName) {
      return null;
   }

   public void updateCollectionMetadata(final String collectionName, final CollectionMetadataElement element) {

   }*/

   public void dropCollectionMetadata(final String collectionName) {

   }

   public List<String> getAttributeValues(final String collectionName, final String attributeName) {
      return null;
   }

   public void addColumn(final String collectionName, final String columnName) {

   }

   public void renameColumn(final String collectionName, final String origName, final String newName) {

   }

   public void dropColumn(final String collectionName, final String columnName) {

   }

   public void onCollectionEvent(@Observes(notifyObserver = Reception.IF_EXISTS) final CollectionEvent event) {
      // we do not care which collection got changed, we just invalidate our cache
      collections = null;
   }

}
