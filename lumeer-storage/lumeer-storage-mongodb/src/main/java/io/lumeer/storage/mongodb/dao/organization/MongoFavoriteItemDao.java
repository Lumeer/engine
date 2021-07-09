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

import static com.mongodb.client.model.Filters.*;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.AddFavoriteItem;
import io.lumeer.engine.api.event.RemoveFavoriteItem;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoFavoriteItemDao extends MongoOrganizationScopedDao implements FavoriteItemDao {

   private static final String PREFIX_COLLECTIONS = "fav_collection-";
   private static final String PREFIX_DOCUMENTS = "fav_document-";
   private static final String PREFIX_VIEWS = "fav_view-";

   private static final String PROJECT_ID = "projectId";
   private static final String USER_ID = "userId";
   private static final String COLLECTION_ID = "collectionId";
   private static final String VIEW_ID = "viewId";
   private static final String DOCUMENT_ID = "documentId";

   @Inject
   private Event<AddFavoriteItem> addFavoriteItemEvent;

   @Inject
   private Event<RemoveFavoriteItem> removeFavoriteItemEvent;

   @Override
   public void createRepository(final Organization organization) {
      createCollectionsRepository(organization);
      createViewsRepository(organization);
      createDocumentsRepository(organization);
   }

   private void createCollectionsRepository(final Organization organization) {
      database.createCollection(favoriteCollectionsDBName(organization));

      MongoCollection<Document> collection = database.getCollection(favoriteCollectionsDBName(organization));
      collection.createIndex(Indexes.ascending(USER_ID, PROJECT_ID, COLLECTION_ID), new IndexOptions().unique(true));
   }

   private void createViewsRepository(final Organization organization) {
      database.createCollection(favoriteViewsDBName(organization));

      MongoCollection<Document> collection = database.getCollection(favoriteViewsDBName(organization));
      collection.createIndex(Indexes.ascending(USER_ID, PROJECT_ID, VIEW_ID), new IndexOptions().unique(true));
   }

   private void createDocumentsRepository(final Organization organization) {
      database.createCollection(favoriteDocumentsDBName(organization));

      MongoCollection<Document> collection = database.getCollection(favoriteDocumentsDBName(organization));
      collection.createIndex(Indexes.ascending(USER_ID, PROJECT_ID, COLLECTION_ID, DOCUMENT_ID), new IndexOptions().unique(true));
   }

   @Override
   public void deleteRepository(final Organization organization) {
      database.getCollection(favoriteCollectionsDBName(organization)).drop();
      database.getCollection(favoriteViewsDBName(organization)).drop();
      database.getCollection(favoriteDocumentsDBName(organization)).drop();
   }

   @Override
   public void addFavoriteCollection(final String userId, final String projectId, final String collectionId) {
      Document document = new Document()
            .append(USER_ID, userId)
            .append(PROJECT_ID, projectId)
            .append(COLLECTION_ID, collectionId);
      try {
         favoriteCollectionsDBCollection().insertOne(document);
         if (addFavoriteItemEvent != null) {
            addFavoriteItemEvent.fire(new AddFavoriteItem(userId, collectionId, ResourceType.COLLECTION));
         }
      } catch (MongoException ex) {
         throw new StorageException("User : " + userId + " has already " + collectionId + " as favorite collection");
      }
   }

   @Override
   public void removeFavoriteCollection(final String userId, final String collectionId) {
      Bson filter = and(eq(USER_ID, userId), eq(COLLECTION_ID, collectionId));
      Document deleted = favoriteCollectionsDBCollection().findOneAndDelete(filter);
      if (deleted != null && removeFavoriteItemEvent != null) {
         removeFavoriteItemEvent.fire(new RemoveFavoriteItem(userId, collectionId, ResourceType.COLLECTION));
      }
   }

   @Override
   public void removeFavoriteCollectionFromUsers(String projectId, final String collectionId) {
      Bson filter = and(eq(PROJECT_ID, projectId), eq(COLLECTION_ID, collectionId));
      favoriteCollectionsDBCollection().deleteMany(filter);
   }

   @Override
   public void removeFavoriteCollectionsByProjectFromUsers(final String projectId) {
      Bson filter = eq(PROJECT_ID, projectId);
      favoriteCollectionsDBCollection().deleteMany(filter);
   }

   @Override
   public Set<String> getFavoriteCollectionIds(final String userId, final String projectId) {
      Bson filter = and(eq(USER_ID, userId), eq(PROJECT_ID, projectId));
      final ArrayList<Document> favoriteCollections = favoriteCollectionsDBCollection().find(filter).into(new ArrayList<>());
      return favoriteCollections.stream()
                                .map(document -> document.getString(COLLECTION_ID))
                                .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getFavoriteCollectionIds(final String projectId) {
      Bson filter = eq(PROJECT_ID, projectId);
      final ArrayList<Document> favoriteCollections = favoriteCollectionsDBCollection().find(filter).into(new ArrayList<>());
      return favoriteCollections.stream()
                                .map(document -> document.getString(COLLECTION_ID))
                                .collect(Collectors.toSet());
   }

   @Override
   public void addFavoriteView(final String userId, final String projectId, final String viewId) {
      Document document = new Document()
            .append(USER_ID, userId)
            .append(PROJECT_ID, projectId)
            .append(VIEW_ID, viewId);
      try {
         favoriteViewsDBCollection().insertOne(document);
         if (addFavoriteItemEvent != null) {
            addFavoriteItemEvent.fire(new AddFavoriteItem(userId, viewId, ResourceType.VIEW));
         }
      } catch (MongoException ex) {
         throw new StorageException("User : " + userId + " has already " + viewId + " as favorite view");
      }
   }

   @Override
   public void removeFavoriteView(final String userId, final String viewId) {
      Bson filter = and(eq(USER_ID, userId), eq(VIEW_ID, viewId));
      Document deleted = favoriteViewsDBCollection().findOneAndDelete(filter);
      if (deleted != null && removeFavoriteItemEvent != null) {
         removeFavoriteItemEvent.fire(new RemoveFavoriteItem(userId, viewId, ResourceType.VIEW));
      }
   }

   @Override
   public void removeFavoriteViewFromUsers(final String projectId, final String viewId) {
      Bson filter = and(eq(PROJECT_ID, projectId), eq(VIEW_ID, viewId));
      favoriteViewsDBCollection().deleteMany(filter);
   }

   @Override
   public void removeFavoriteViewByProjectFromUsers(final String projectId) {
      Bson filter = eq(PROJECT_ID, projectId);
      favoriteViewsDBCollection().deleteMany(filter);
   }

   @Override
   public Set<String> getFavoriteViewIds(final String userId, final String projectId) {
      Bson filter = and(eq(USER_ID, userId), eq(PROJECT_ID, projectId));
      final ArrayList<Document> favoriteViews = favoriteViewsDBCollection().find(filter).into(new ArrayList<>());
      return favoriteViews.stream()
                                .map(document -> document.getString(VIEW_ID))
                                .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getFavoriteViewIds(final String projectId) {
      Bson filter = eq(PROJECT_ID, projectId);
      final ArrayList<Document> favoriteViews = favoriteViewsDBCollection().find(filter).into(new ArrayList<>());
      return favoriteViews.stream()
                          .map(document -> document.getString(VIEW_ID))
                          .collect(Collectors.toSet());
   }

   @Override
   public void addFavoriteDocument(final String userId, final String projectId, final String collectionId, final String documentId) {
      Document document = new Document()
            .append(USER_ID, userId)
            .append(PROJECT_ID, projectId)
            .append(COLLECTION_ID, collectionId)
            .append(DOCUMENT_ID, documentId);
      try {
         favoriteDocumentsDBCollection().insertOne(document);
         if (addFavoriteItemEvent != null) {
            addFavoriteItemEvent.fire(new AddFavoriteItem(userId, documentId, ResourceType.DOCUMENT));
         }
      } catch (MongoException ex) {
         throw new StorageException("User : " + userId + " has already " + documentId + " as favorite document");
      }
   }

   @Override
   public void removeFavoriteDocument(final String userId, final String documentId) {
      Bson filter = and(eq(USER_ID, userId), eq(DOCUMENT_ID, documentId));
      Document deleted = favoriteDocumentsDBCollection().findOneAndDelete(filter);
      if (deleted != null && removeFavoriteItemEvent != null) {
         removeFavoriteItemEvent.fire(new RemoveFavoriteItem(userId, documentId, ResourceType.DOCUMENT));
      }
   }

   @Override
   public void removeFavoriteDocumentFromUsers(final String projectId, final String collectionId, final String documentId) {
      Bson filter = and(eq(PROJECT_ID, projectId), eq(COLLECTION_ID, collectionId), eq(DOCUMENT_ID, documentId));
      favoriteDocumentsDBCollection().deleteMany(filter);
   }

   @Override
   public void removeFavoriteDocumentsByProjectFromUsers(final String projectId) {
      Bson filter = eq(PROJECT_ID, projectId);
      favoriteDocumentsDBCollection().deleteMany(filter);
   }

   @Override
   public void removeFavoriteDocumentsByCollectionFromUsers(final String projectId, final String collectionId) {
      Bson filter = and(eq(PROJECT_ID, projectId), eq(COLLECTION_ID, collectionId));
      favoriteDocumentsDBCollection().deleteMany(filter);
   }

   @Override
   public Set<String> getFavoriteDocumentIds(final String userId, final String projectId) {
      Bson filter = and(eq(USER_ID, userId), eq(PROJECT_ID, projectId));
      final ArrayList<Document> favoriteDocuments = favoriteDocumentsDBCollection().find(filter).into(new ArrayList<>());
      return favoriteDocuments.stream()
                              .map(document -> document.getString(DOCUMENT_ID))
                              .collect(Collectors.toSet());
   }

   private String favoriteCollectionsDBName() {
      if (getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return favoriteCollectionsDBName(getOrganization().get());
   }

   private String favoriteCollectionsDBName(Organization organization) {
      return PREFIX_COLLECTIONS + organization.getId();
   }

   private MongoCollection<Document> favoriteCollectionsDBCollection() {
      return database.getCollection(favoriteCollectionsDBName());
   }

   private String favoriteDocumentsDBName() {
      if (getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return favoriteDocumentsDBName(getOrganization().get());
   }

   private String favoriteDocumentsDBName(Organization organization) {
      return PREFIX_DOCUMENTS + organization.getId();
   }

   private MongoCollection<Document> favoriteDocumentsDBCollection() {
      return database.getCollection(favoriteDocumentsDBName());
   }

   private String favoriteViewsDBName() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return favoriteViewsDBName(getOrganization().get());
   }

   private String favoriteViewsDBName(Organization organization) {
      return PREFIX_VIEWS + organization.getId();
   }

   private MongoCollection<Document> favoriteViewsDBCollection() {
      return database.getCollection(favoriteViewsDBName());
   }
}
