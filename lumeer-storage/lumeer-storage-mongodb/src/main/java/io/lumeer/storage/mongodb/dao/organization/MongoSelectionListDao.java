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
package io.lumeer.storage.mongodb.dao.organization;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.SelectionList;
import io.lumeer.engine.api.event.CreateSelectionList;
import io.lumeer.engine.api.event.ReloadSelectionLists;
import io.lumeer.engine.api.event.RemoveSelectionList;
import io.lumeer.engine.api.event.UpdateSelectionList;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.SelectionListCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoSelectionListDao extends MongoOrganizationScopedDao implements SelectionListDao {

   private static final String PREFIX = "selectionLists_o-";

   @Inject
   private Event<CreateSelectionList> createEvent;

   @Inject
   private Event<ReloadSelectionLists> reloadEvent;

   @Inject
   private Event<UpdateSelectionList> updateEvent;

   @Inject
   private Event<RemoveSelectionList> removeEvent;

   @Override
   public void createRepository(Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      ensureIndexes(organization);
   }

   @Override
   public void deleteRepository(Organization organization) {
      database.getCollection(databaseCollectionName(organization)).drop();
   }

   @Override
   public void ensureIndexes(final Organization organization) {
      MongoCollection<Document> collection = database.getCollection(databaseCollectionName(organization));
      collection.createIndex(Indexes.ascending(SelectionList.PROJECT_ID, SelectionList.NAME), new IndexOptions().unique(true));
   }

   @Override
   public SelectionList createList(final SelectionList selection) {
      try {
         databaseCollection().insertOne(selection);

         if (createEvent != null) {
            mapList(selection);
            createEvent.fire(new CreateSelectionList(getOrganization().get().getId(), selection));
         }
         return selection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create selection list " + selection, ex);
      }
   }

   @Override
   public void createLists(final List<SelectionList> lists, final String projectId) {
      try {
         databaseCollection().insertMany(lists);

         if (reloadEvent != null) {
            reloadEvent.fire(new ReloadSelectionLists(getOrganization().get().getId(), projectId));
         }
      } catch (MongoException ex) {
         throw new StorageException("Cannot create selection lists " + lists, ex);
      }
   }

   @Override
   public SelectionList updateList(final String id, final SelectionList selection) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         SelectionList returnedList = databaseCollection().findOneAndReplace(idFilter(id), selection, options);
         if (returnedList == null) {
            throw new StorageException("Selection list '" + id + "' has not been updated.");
         }
         if (updateEvent != null) {
            mapList(returnedList);
            updateEvent.fire(new UpdateSelectionList(getOrganization().get().getId(), returnedList));
         }
         return returnedList;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update selection list " + selection, ex);
      }
   }

   @Override
   public void deleteList(final SelectionList list) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(list.getId()));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Selection list '" + list.getId() + "' has not been deleted.");
      }
      if (removeEvent != null) {
         mapList(list);
         removeEvent.fire(new RemoveSelectionList(getOrganization().get().getId(), list));
      }
   }

   @Override
   public List<SelectionList> getAllLists() {
      return databaseCollection().find().into(new ArrayList<>())
                                 .stream().map(this::mapList)
                                 .collect(Collectors.toList());
   }

   @Override
   public List<SelectionList> getAllLists(final List<String> projectIds) {
      Bson filter = Filters.in(SelectionListCodec.PROJECT_ID, projectIds);
      return databaseCollection().find(filter).into(new ArrayList<>())
                                 .stream().map(this::mapList)
                                 .collect(Collectors.toList());
   }

   @Override
   public SelectionList getList(final String id) {
      MongoCursor<SelectionList> mongoCursor = databaseCollection().find(MongoFilters.idFilter(id)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new StorageException("Selection list '" + id + "' could not be found.");
      }
      return mapList(mongoCursor.next());
   }

   private SelectionList mapList(SelectionList list) {
      if (list != null) {
         list.setOrganizationId(getOrganization().get().getId());
      }
      return list;
   }

   MongoCollection<SelectionList> databaseCollection() {
      return database.getCollection(databaseCollectionName(), SelectionList.class);
   }

   String databaseCollectionName() {
      if (getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollectionName(getOrganization().get());
   }

   private String databaseCollectionName(Organization organization) {
      return databaseCollectionName(organization.getId());
   }

   private String databaseCollectionName(String organizationId) {
      return PREFIX + organizationId;
   }
}
