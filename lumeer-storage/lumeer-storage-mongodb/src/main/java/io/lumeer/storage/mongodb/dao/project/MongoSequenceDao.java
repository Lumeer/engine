/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
package io.lumeer.storage.mongodb.dao.project;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.SequenceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoSequenceDao extends ProjectScopedDao implements SequenceDao {

   private static final String PREFIX = "sequences_p-";


   @Override
   public void createSequencesRepository(final Project project) {
      database.createCollection(getSequenceCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(getSequenceCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(INDEX_NAME), new IndexOptions().unique(true));
   }

   @Override
   public void deleteSequencesRepository(final Project project) {
      database.getCollection(getSequenceCollectionName(project)).drop();
   }


   @Override
   public synchronized int getNextSequenceNo(final String indexName) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Document doc = database.getCollection(getDatabaseCollectionName()).findOneAndUpdate(eq(INDEX_NAME, indexName), inc("seq", 1),
            options);

      if (doc == null) { // the sequence did not exist
         resetSequence(indexName);
         return 0;
      } else {
         return doc.getInteger("seq");
      }
   }

   @Override
   public synchronized void resetSequence(final String indexName) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Document doc = database.getCollection(getDatabaseCollectionName()).findOneAndUpdate(eq(INDEX_NAME, indexName), set("seq", 0),
            options);

      if (doc == null) {
         Document newSeq = new Document();
         newSeq.put(INDEX_NAME, indexName);
         newSeq.put("seq", 0);
         database.getCollection(getDatabaseCollectionName()).insertOne(newSeq);
      }
   }

   public String getSequenceCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   String getDatabaseCollectionName() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return getSequenceCollectionName(getProject().get());
   }

   MongoCollection<Collection> databaseCollection() {
      return database.getCollection(getDatabaseCollectionName(), Collection.class);
   }

}
