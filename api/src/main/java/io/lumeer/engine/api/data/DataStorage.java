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
package io.lumeer.engine.api.data;

import io.lumeer.engine.api.exception.UnsuccessfulOperationException;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a data storage.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public interface DataStorage extends Serializable {

   void connect(final List<StorageConnection> connections, final String database, final Boolean useSsl);

   default void connect(final StorageConnection connection, final String database, final Boolean useSsl) {
      connect(Collections.singletonList(connection), database, useSsl);
   }

   void disconnect();

   /**
    * Returns a List object of all collection names in the database.
    *
    * @return the list of all collection names
    */
   List<String> getAllCollections();

   /**
    * Creates a new collection with the specified name.
    *
    * @param collectionName
    *       the name of the collection to create
    */
   void createCollection(final String collectionName);

   /**
    * Drops the collection with the specified name.
    *
    * @param collectionName
    *       the name of the collection to drop
    */
   void dropCollection(final String collectionName);

   /**
    * Renames collection
    *
    * @param oldCollectionName
    *       the name of the collection to rename
    * @param newCollectionName
    *       new name of collection
    */
   void renameCollection(final String oldCollectionName, final String newCollectionName);

   /**
    * Checks whether the given collection already exists.
    *
    * @param collectionName
    *       The name of the collection to check for.
    * @return True if and only if the collection exists.
    */
   boolean hasCollection(final String collectionName);

   /**
    * Count number of documents in collection
    *
    * @param collectionName
    *       The name of the collection to check for.
    * @return number of documents in collection
    */
   long documentCount(final String collectionName);

   /**
    * Checks whether the document exists in given collection.
    *
    * @param collectionName
    *       The name of the collection to find document.
    * @param filter
    *       the filter to identify document
    * @return True if and only if the document exists.
    */
   boolean collectionHasDocument(final String collectionName, final DataFilter filter);

   /**
    * Creates and inserts a new document to specified collection.
    *
    * @param collectionName
    *       the name of the collection where the document will be created
    * @param document
    *       the DataDocument object representing a document to be created
    * @return the id of the newly created document
    */
   String createDocument(final String collectionName, final DataDocument document);

   /**
    * Creates and inserts new documents to specified collection. Skip if already exists.
    *
    * @param collectionName
    *       The name of the collection where the documents will be created.
    * @param dataDocuments
    *       The DataDocument objects representing a documents to be created.
    * @return The list of ids of the newly created documents.
    */
   List<String> createDocuments(final String collectionName, final List<DataDocument> dataDocuments);

   /**
    * Creates and inserts an old document to specified collection.
    *
    * @param collectionName
    *       the name of the collection where the document will be created
    * @param document
    *       the DataDocument object representing a document to be created
    * @param documentId
    *       the id of the document
    * @param version
    *       the version of document
    * @throws UnsuccessfulOperationException
    *       When somebody already updated the document.
    */
   void createOldDocument(final String collectionName, final DataDocument document, String documentId, int version) throws UnsuccessfulOperationException;

   /**
    * Reads the specified document in given collection by filter.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param filter
    *       the filter to identify document
    * @param attributes
    *       list of attribute names
    * @return the DataDocument object representing the read document containg only specified attributes
    */
   DataDocument readDocumentIncludeAttrs(final String collectionName, final DataFilter filter, final List<String> attributes);

   /**
    * Reads the specified document in given collection by filter.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param filter
    *       the filter to identify document
    * @return the DataDocument object representing the read document
    */
   DataDocument readDocument(final String collectionName, final DataFilter filter);

   /**
    * Modifies an existing document in given collection by filter. If updated document contains non-existing columns, they will be added into database.
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param updatedDocument
    *       the DataDocument object representing a document with changes to update
    * @param filter
    *       the filter to identify document
    */
   void updateDocument(final String collectionName, final DataDocument updatedDocument, final DataFilter filter);

   /**
    * Replace an existing document in given collection by filter.
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param replaceDocument
    *       the DataDocument object representing a document
    * @param filter
    *       the filter to identify document
    */
   void replaceDocument(final String collectionName, final DataDocument replaceDocument, final DataFilter filter);

   /**
    * Drops an existing document in given collection by filter.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param filter
    *       the filter to identify document
    */
   void dropDocument(final String collectionName, final DataFilter filter);

   /**
    * Drops many documents based on filter.
    *
    * @param collectionName
    *       the name of the collection
    * @param filter
    *       string representation of filter
    */
   void dropManyDocuments(final String collectionName, final DataFilter filter);

   /**
    * Updates the name of an attribute which is found in all documents of given collection.
    *
    * @param collectionName
    *       the name of the collection where the given attribute should be renamed
    * @param oldName
    *       the old name of an attribute
    * @param newName
    *       the new name of an attribute
    */
   void renameAttribute(final String collectionName, final String oldName, final String newName);

   /**
    * Removes given attribute from existing document specified by filter.
    *
    * @param collectionName
    *       the name of the collection where the given attribute should be removed
    * @param filter
    *       the filter to identify document
    * @param attributeName
    *       the name of an attribute to remove
    */
   void dropAttribute(final String collectionName, final DataFilter filter, final String attributeName);

   /**
    * Add item to array
    *
    * @param collectionName
    *       the name of the collection
    * @param filter
    *       the filter to identify document
    * @param attributeName
    *       the name of an attribute to add item
    * @param item
    *       the item to add to array
    */
   <T> void addItemToArray(final String collectionName, final DataFilter filter, final String attributeName, final T item);

   /**
    * Add items to array
    *
    * @param collectionName
    *       the name of the collection
    * @param filter
    *       the filter to identify document
    * @param attributeName
    *       the name of an attribute to add item
    * @param items
    *       the items to add to array
    */
   <T> void addItemsToArray(final String collectionName, final DataFilter filter, final String attributeName, final List<T> items);

   /**
    * remove specified item from array
    *
    * @param collectionName
    *       the name of the collection
    * @param filter
    *       the filter to identify document
    * @param attributeName
    *       the name of an attribute to add item
    * @param item
    *       the item that will be deleted from the array
    */
   <T> void removeItemFromArray(final String collectionName, final DataFilter filter, final String attributeName, final T item);

   /**
    * remove items from array
    *
    * @param collectionName
    *       the name of the collection
    * @param filter
    *       the filter to identify document
    * @param attributeName
    *       the name of an attribute to remove item
    * @param items
    *       the items that will be deleted from the array
    */
   <T> void removeItemsFromArray(final String collectionName, final DataFilter filter, final String attributeName, final List<T> items);

   /**
    * Gets the first 100 distinct values of the given attribute in the given collection.
    *
    * @param collectionName
    *       the name of the collection where documents contain the given attribute
    * @param attributeName
    *       the name of the attribute
    * @return the distinct set of values of the given attribute
    */
   Set<String> getAttributeValues(final String collectionName, final String attributeName);

   /**
    * Executes a command to find and return documents.
    *
    * @param command
    *       the database find command specified as a JSON string
    * @return the list of the found documents
    * @see <a href="https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find">https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find</a>
    */
   List<DataDocument> run(final String command);

   /**
    * Executes a command to find and return documents.
    *
    * @param command
    *       the database find command specified as a DataDocument
    * @return the list of the found documents
    * @see <a href="https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find">https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find</a>
    */
   List<DataDocument> run(final DataDocument command);

   /**
    * Searches the specified collection for specified documents using filter and projection to specific attributes.
    *
    * @param collectionName
    *       the name of the collection where the run will be performed
    * @param filter
    *       the query predicate. If unspecified, then all documents in the collection will match the predicate.
    * @param attributes
    *       list of attribute names
    * @return the list of the found documents containing only specified attributes
    */
   List<DataDocument> search(final String collectionName, final DataFilter filter, final List<String> attributes);

   /**
    * Searches the specified collection for specified documents using filter, sort, skip and limit option.
    *
    * @param collectionName
    *       the name of the collection where the run will be performed
    * @param filter
    *       the query predicate. If unspecified, then all documents in the collection will match the predicate.
    * @param sort
    *       the sort specification for the ordering of the results. If unspecified, then a sort is equivalent to setting no sort.
    * @param skip
    *       the number of documents to skip. A skip of 0 is equivalent to setting no skip.
    * @param limit
    *       the maximum number of documents to return. A limit of 0 is equivalent to setting no limit.
    * @return the list of the found documents
    */
   List<DataDocument> search(final String collectionName, final DataFilter filter, final DataSort sort, final int skip, final int limit);

   /**
    * Searches the specified collection for specified documents using filter and projection to specific attributes.
    *
    * @param collectionName
    *       the name of the collection where the run will be performed
    * @param filter
    *       the query predicate. If unspecified, then all documents in the collection will match the predicate.
    * @param sort
    *       the sort specification for the ordering of the results. If unspecified, then a sort is equivalent to setting no sort.
    * @param attributes
    *       list of attribute names
    * @param skip
    *       the number of documents to skip. A skip of 0 is equivalent to setting no skip.
    * @param limit
    *       the maximum number of documents to return. A limit of 0 is equivalent to setting no limit.
    * @return the list of the found documents containing only specified attributes
    */
   List<DataDocument> search(final String collectionName, final DataFilter filter, final DataSort sort, final List<String> attributes, final int skip, final int limit);

   /**
    * Counts the number of document in the collection optionally meeting the filter criteria.
    *
    * @param collectionName
    *       The name of the collection.
    * @param filter
    *       The filter on documents.
    * @return Number of documents in the collection meeting the criteria.
    */
   long count(final String collectionName, final DataFilter filter);

   /**
    * Executes the provided query and returns its results. The query needs to have real database collection names filled in.
    *
    * @param query
    *       Query to execute.
    * @return Results of the query.
    */
   List<DataDocument> query(final Query query);

   /**
    * Executes series of database operations.
    *
    * @param collectionName
    *       Collection on which to execute the operations.
    * @param stages
    *       Operation stages to execute one by one.
    * @return Resulting document.
    */
   List<DataDocument> aggregate(final String collectionName, final DataDocument... stages);

   /**
    * Increment attribute value of document by specified amount. If the field does not exist, it creates the field and sets the field to the specified value.
    *
    * @param collectionName
    *       the name of the collection where the given document is located
    * @param filter
    *       the filter to obtain a document
    * @param attributeName
    *       the name of attribute which value is increment
    * @param incBy
    *       the value by which attribute is increment
    */
   void incrementAttributeValueBy(final String collectionName, final DataFilter filter, final String attributeName, final int incBy);

   /**
    * Gets the next value of sequence.
    *
    * @param collectionName
    *       Name of the collection with sequences.
    * @param indexAttribute
    *       Name of the attribute that identifies the sequence document.
    * @param index
    *       Value of the index attribute to identify the sequence.
    * @return The next value in the sequence.
    */
   int getNextSequenceNo(final String collectionName, final String indexAttribute, final String index);

   /**
    * Resets a sequence to zero.
    *
    * @param collectionName
    *       Name of the collection with sequences.
    * @param indexAttribute
    *       Name of the attribute that identifies the sequence document.
    * @param index
    *       Value of the index attribute to identify the sequence.
    */
   void resetSequence(final String collectionName, final String indexAttribute, final String index);

   /**
    * Creates defined indexes on a collection.
    *
    * @param collectionName
    *       Name of the collection.
    * @param indexAttributes
    *       Names of atributes and their index types to create index on.
    * @param unique
    *       indicated if index should be unique
    */
   void createIndex(final String collectionName, final DataDocument indexAttributes, boolean unique);

   /**
    * Lists all indexes on the given collection.
    *
    * @param collectionName
    *       The name of the collection to get indexes of.
    * @return The list of all indexes on the given collection.
    */
   List<DataDocument> listIndexes(final String collectionName);

   /**
    * Drops the given index on a collection.
    *
    * @param collectionName
    *       The collection name.
    * @param indexName
    *       The name of an index to drop.
    */
   void dropIndex(final String collectionName, final String indexName);

   /**
    * Gets the statistics about database usage.
    *
    * @return Statistics about database usage.
    */
   DataStorageStats getDbStats();

   /**
    * Gets the statistics about collection usage.
    *
    * @param collectionName
    *       Name of the collection to obtain statistics for.
    * @return Statistics about collection usage.
    */
   DataStorageStats getCollectionStats(final String collectionName);

   Object getDatabase();

   Object getDataStore();
}
