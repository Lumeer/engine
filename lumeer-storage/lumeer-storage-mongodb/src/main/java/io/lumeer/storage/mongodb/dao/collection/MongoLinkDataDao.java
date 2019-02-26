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
package io.lumeer.storage.mongodb.dao.collection;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.idsFilter;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Set;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoLinkDataDao extends CollectionScopedDao implements LinkDataDao {

   private static final String ID = "_id";
   private static final String PREFIX = "linkData_c-";

   @Override
   public void createDataRepository(final String linkTypeId) {
      database.createCollection(linkDataCollectionName(linkTypeId));
      createFulltextIndexOnAllFields(linkTypeId);
   }

   private void createFulltextIndexOnAllFields(final String linkTypeId) {
      linkDataCollection(linkTypeId).createIndex(Indexes.text("$**"));
   }

   @Override
   public void deleteDataRepository(final String linkTypeId) {
      linkDataCollection(linkTypeId).drop();
   }

   @Override
   public DataDocument createData(final String linkTypeId, final String linkInstanceId, final DataDocument data) {
      Document document = new Document(data).append(ID, new ObjectId(linkInstanceId));
      linkDataCollection(linkTypeId).insertOne(document);
      return data;
   }

   @Override
   public DataDocument updateData(final String linkTypeId, final String linkInstanceId, final DataDocument data) {
      Document document = new Document(data);
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      Document updatedDocument = linkDataCollection(linkTypeId).findOneAndReplace(idFilter(linkInstanceId), document, options);
      if (updatedDocument == null) {
         throw new StorageException("LinkInstance '" + linkInstanceId + "' has not been updated (replaced).");
      }
      return MongoUtils.convertDocument(updatedDocument);
   }

   @Override
   public DataDocument patchData(final String linkTypeId, final String linkInstanceId, final DataDocument data) {
      data.remove(ID);
      Document updateDocument = new Document("$set", new Document(data));
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      Document patchedDocument = linkDataCollection(linkTypeId).findOneAndUpdate(idFilter(linkInstanceId), updateDocument, options);
      if (patchedDocument == null) {
         throw new StorageException("LinkInstance '" + linkInstanceId + "' has not been patched (partially updated).");
      }
      return MongoUtils.convertDocument(patchedDocument);
   }

   @Override
   public void deleteData(final String linkTypeId, final String linkInstanceId) {
      linkDataCollection(linkTypeId).deleteOne(idFilter(linkInstanceId));
   }

   @Override
   public void deleteData(final String linkTypeId, final Set<String> linkInstanceIds) {
      linkDataCollection(linkTypeId).deleteMany(idsFilter(linkInstanceIds));
   }

   @Override
   public long deleteAttribute(final String linkTypeId, final String attributeId) {
      final UpdateResult updateResult = linkDataCollection(linkTypeId).updateMany(new BsonDocument(), Updates.unset(attributeId));
      return updateResult.getModifiedCount();
   }

   @Override
   public DataDocument getData(final String linkTypeId, final String linkInstanceId) {
      MongoCursor<Document> mongoCursor = linkDataCollection(linkTypeId).find(idFilter(linkInstanceId)).iterator();
      if (!mongoCursor.hasNext()) {
         return new DataDocument();
      }
      return MongoUtils.convertDocument(mongoCursor.next());
   }

   @Override
   public List<DataDocument> getData(final String linkTypeId) {
      return MongoUtils.convertIterableToList(linkDataCollection(linkTypeId).find());
   }

   @Override
   public List<DataDocument> getData(final String linkTypeId, final Set<String> linkInstanceIds) {
      return MongoUtils.convertIterableToList(linkDataCollection(linkTypeId).find(MongoFilters.idsFilter(linkInstanceIds)));
   }

   MongoCollection<Document> linkDataCollection(String linkTypeId) {
      return database.getCollection(linkDataCollectionName(linkTypeId));
   }

   String linkDataCollectionName(String linkTypeId) {
      return PREFIX + linkTypeId;
   }

}
