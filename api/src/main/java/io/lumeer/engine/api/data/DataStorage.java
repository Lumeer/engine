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
package io.lumeer.engine.api.data;

import java.util.List;
import java.util.Set;

/**
 * Represents a data storage.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 *         <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 *         <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public interface DataStorage {

   List<String> getAllCollections();

   void createCollection(final String collectionName);

   void dropCollection(final String collectionName);

   String createDocument(final String collectionName, final DataDocument document);

   DataDocument readDocument(final String collectionName, final String documentId);

   void updateDocument(final String collectionName, final DataDocument updatedDocument, final String documentId);

   void dropDocument(final String collectionName, final String documentId);

   void renameAttribute(final String collectionName, final String oldName, final String newName);

   Set<String> getAttributeValues(final String collectionName, final String attributeName);

   List<DataDocument> search(final String query);

   List<DataDocument> search(final String collectionName, final String filter, final String sort, final int skip, final int limit);

}
