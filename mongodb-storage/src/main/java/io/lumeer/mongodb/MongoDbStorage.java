/*
 * -----------------------------------------------------------------------\
 * Lumeer
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
package io.lumeer.mongodb;

import io.lumeer.engine.api.data.DataElement;
import io.lumeer.engine.api.data.DataStorage;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Model
public class MongoDbStorage implements DataStorage {

   @PostConstruct
   public void connect() {
      // connect to the database
   }

   @Override
   public void createCollection(final String name) {

   }

   @Override
   public List<String> getAttributes(final String collectionName) {
      return null;
   }

   @Override
   public List<String> getAttributeValues(final String collectionName, final String attributeName) {
      return null;
   }

   @Override
   public List<String> getCollections(final String searchString) {
      return null;
   }

   @Override
   public List<String> getAllCollections() {
      return null;
   }

   @Override
   public void dropCollection(final String name) {

   }

   @Override
   public void renameCollection(final String origName, final String newName) {

   }

   @Override
   public void storeElement(final DataElement element) {

   }

   @Override
   public void updateElement(final DataElement element) {

   }

   @Override
   public void dropElement(final DataElement element) {

   }

   @Override
   public List<DataElement> search(final String query, final int page, final int limit) {
      return null;
   }
}
