package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Organization;

import java.util.Set;

public interface FavoriteItemDao {

   void createRepositories(Organization organization);

   void deleteRepositories(Organization organization);

   void addFavoriteCollection(String userId, String projectId, String collectionId);

   void removeFavoriteCollection(String userId, String collectionId);

   void removeFavoriteCollectionFromUsers(String projectId, String collectionId);

   void removeFavoriteCollectionsByProjectFromUsers(String projectId);

   Set<String> getFavoriteCollectionIds(String userId, String projectId);

   void addFavoriteDocument(String userId, String projectId, String collectionId, String documentId);

   void removeFavoriteDocument(String userId, String documentId);

   void removeFavoriteDocumentFromUsers(String projectId, String collectionId, String documentId);

   void removeFavoriteDocumentsByProjectFromUsers(String projectId);

   void removeFavoriteDocumentsByCollectionFromUsers(String projectId, String collectionId);

   Set<String> getFavoriteDocumentIds(String userId, String projectId);
}
