/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
package io.lumeer.engine.api.data;

import java.util.List;

/**
 * Represents a data storage.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface DataStorage {

   void createCollection(final String name);

   List<String> getAttributes(final String collectionName);

   List<String> getAttributeValues(final String collectionName, final String attributeName);

   List<String> getCollections(final String searchString);

   List<String> getAllCollections();

   void dropCollection(final String name);

   void renameCollection(final String origName, final String newName);

   void storeElement(final DataElement element);

   void updateElement(final DataElement element);

   void dropElement(final DataElement element);

   List<DataElement> search(final String query, int page, int limit);
}
