package io.lumeer.storage.mongodb.dao.organization;

import static com.mongodb.client.model.Filters.*;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
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

@RequestScoped
public class MongoFavoriteItemDao extends OrganizationScopedDao implements FavoriteItemDao {

   private static final String PREFIX_COLLECTIONS = "fav_collection-";
   private static final String PREFIX_DOCUMENTS = "fav_document-";

   private static final String PROJECT_ID = "projectId";
   private static final String USER_ID = "userId";
   private static final String COLLECTION_ID = "collectionId";
   private static final String DOCUMENT_ID = "documentId";

   @Override
   public void createRepositories(final Organization organization) {
      createCollectionsRepository(organization);
      createDocumentsRepository(organization);
   }

   private void createCollectionsRepository(final Organization organization) {
      database.createCollection(favoriteCollectionsDBName(organization));

      MongoCollection<Document> collection = database.getCollection(favoriteCollectionsDBName(organization));
      collection.createIndex(Indexes.ascending(USER_ID, PROJECT_ID, COLLECTION_ID), new IndexOptions().unique(true));
   }

   private void createDocumentsRepository(final Organization organization) {
      database.createCollection(favoriteDocumentsDBName(organization));

      MongoCollection<Document> collection = database.getCollection(favoriteDocumentsDBName(organization));
      collection.createIndex(Indexes.ascending(USER_ID, PROJECT_ID, COLLECTION_ID, DOCUMENT_ID), new IndexOptions().unique(true));
   }

   @Override
   public void deleteRepositories(final Organization organization) {
      database.getCollection(favoriteCollectionsDBName(organization)).drop();
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
      } catch (MongoException ex) {
         throw new StorageException("User : " + userId + " has already " + collectionId + " as favorite collection");
      }
   }

   @Override
   public void removeFavoriteCollection(final String userId, final String collectionId) {
      Bson filter = and(eq(USER_ID, userId), eq(COLLECTION_ID, collectionId));
      favoriteCollectionsDBCollection().findOneAndDelete(filter);
   }

   @Override
   public void removeFavoriteCollectionFromUsers(String projectId,final String collectionId) {
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
   public void addFavoriteDocument(final String userId, final String projectId, final String collectionId, final String documentId) {
      Document document = new Document()
            .append(USER_ID, userId)
            .append(PROJECT_ID, projectId)
            .append(COLLECTION_ID, collectionId)
            .append(DOCUMENT_ID, documentId);
      try {
         favoriteDocumentsDBCollection().insertOne(document);
      } catch (MongoException ex) {
         throw new StorageException("User : " + userId + " has already " + documentId + " as favorite document");
      }
   }

   @Override
   public void removeFavoriteDocument(final String userId, final String documentId) {
      Bson filter = and(eq(USER_ID, userId), eq(DOCUMENT_ID, documentId));
      favoriteDocumentsDBCollection().findOneAndDelete(filter);
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

   String favoriteCollectionsDBName() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return favoriteCollectionsDBName(getOrganization().get());
   }

   private String favoriteCollectionsDBName(Organization organization) {
      return PREFIX_COLLECTIONS + organization.getId();
   }

   MongoCollection<Document> favoriteCollectionsDBCollection() {
      return database.getCollection(favoriteCollectionsDBName());
   }

   String favoriteDocumentsDBName() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return favoriteDocumentsDBName(getOrganization().get());
   }

   private String favoriteDocumentsDBName(Organization organization) {
      return PREFIX_DOCUMENTS + organization.getId();
   }

   MongoCollection<Document> favoriteDocumentsDBCollection() {
      return database.getCollection(favoriteDocumentsDBName());
   }
}
