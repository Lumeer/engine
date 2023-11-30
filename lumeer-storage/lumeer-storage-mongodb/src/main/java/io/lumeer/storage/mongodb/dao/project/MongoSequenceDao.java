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
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.nameFilter;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Sequence;
import io.lumeer.engine.api.event.CreateOrUpdateSequence;
import io.lumeer.engine.api.event.RemoveSequence;
import io.lumeer.storage.api.dao.SequenceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.SequenceCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@RequestScoped
public class MongoSequenceDao extends MongoProjectScopedDao implements SequenceDao {

   private static final String PREFIX = "sequences_p-";

   @Inject
   private Event<CreateOrUpdateSequence> createOrUpdateSequenceEvent;

   @Inject
   private Event<RemoveSequence> removeSequenceEvent;

   @Override
   public void createRepository(final Project project) {
      database.createCollection(getSequenceCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(getSequenceCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(SequenceCodec.NAME), new IndexOptions().unique(true));
   }

   @Override
   public void deleteRepository(final Project project) {
      database.getCollection(getSequenceCollectionName(project)).drop();
   }

   @Override
   public List<Sequence> getAllSequences() {
      return databaseCollection().find().sort(Sorts.ascending(SequenceCodec.NAME)).into(new ArrayList<>());
   }

   @Override
   public Sequence getSequence(final String name) {
      return databaseCollection().find(nameFilter(name)).first();
   }

   @Override
   public Sequence updateSequence(final String id, final Sequence sequence) {
      return updateSequence(sequence, MongoFilters.idFilter(id));
   }

   @Override
   public void deleteSequence(final String id) {
      final Sequence sequence = databaseCollection().findOneAndDelete(idFilter(id));
      if (sequence == null) {
         throw new StorageException("Sequence '" + id + "' has not been deleted.");
      }
      if (removeSequenceEvent != null) {
         removeSequenceEvent.fire(new RemoveSequence(sequence));
      }
   }

   private Sequence updateSequence(final Sequence sequence, final Bson filter) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
      try {
         Bson update = new Document("$set", sequence);
         final Sequence returnedSequence = databaseCollection().findOneAndUpdate(filter, update, options);
         if (returnedSequence == null) {
            throw new StorageException("Sequence '" + sequence.getId() + "' has not been updated.");
         }
         if (createOrUpdateSequenceEvent != null) {
            createOrUpdateSequenceEvent.fire(new CreateOrUpdateSequence(returnedSequence));
         }
         return returnedSequence;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update sequence " + sequence, ex);
      }
   }

   @Override
   public synchronized int getNextSequenceNo(final String indexName) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Sequence seq = databaseCollection().findOneAndUpdate(eq(SequenceCodec.NAME, indexName), inc(SequenceCodec.SEQ, 1),
            options);

      if (seq == null) { // the sequence did not exist
         resetSequence(indexName);
         return 0;
      } else {
         return seq.getSeq();
      }
   }

   @Override
   public synchronized int changeSequenceBy(final String indexName, final int change) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
      options.returnDocument(ReturnDocument.AFTER);

      final Sequence seq = databaseCollection().findOneAndUpdate(eq(SequenceCodec.NAME, indexName), inc(SequenceCodec.SEQ, change),
            options);

      if (seq == null) { // the sequence did not exist
         resetSequence(indexName, change);
         return change;
      } else {
         return seq.getSeq();
      }
   }

   @Override
   public synchronized void resetSequence(final String indexName) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      final Sequence sequence = new Sequence(indexName, 0);
      Bson update = new org.bson.Document("$set", sequence);
      databaseCollection().findOneAndUpdate(eq(SequenceCodec.NAME, indexName), update, options);
   }

   @Override
   public synchronized void resetSequence(final String indexName, final int initValue) {
      final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      final Sequence sequence = new Sequence(indexName, initValue);
      Bson update = new org.bson.Document("$set", sequence);
      databaseCollection().findOneAndUpdate(eq(SequenceCodec.NAME, indexName), update, options);
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

   MongoCollection<Sequence> databaseCollection() {
      return database.getCollection(getDatabaseCollectionName(), Sequence.class);
   }

}
