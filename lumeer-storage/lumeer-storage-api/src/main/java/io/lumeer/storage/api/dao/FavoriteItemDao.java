package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Organization;

import java.util.Set;

public interface FavoriteItemDao {

   void createRepositories(Organization organization);

   void deleteRepositories(Organization organization);

   void addFavoriteCollection(String userId, String projectId, String collectionId);

   void removeFavoriteCollection(String userId, String collectionId);

   void removeFavoriteCollection(String collectionId);

   void removeFavoriteCollectionsByProject(String projectId);

   Set<String> getFavoriteCollectionIds(String userId, String projectId);

   void addFavoriteDocument(String userId, String projectId, String collectionId, String documentId);

   void removeFavoriteDocument(String userId, String documentId);

   void removeFavoriteDocument(String documentId);

   void removeFavoriteDocumentsByProject(String projectId);

   void removeFavoriteDocumentsByCollection(String collectionId);

   Set<String> getFavoriteDocumentIds(String userId, String projectId);
}
