package io.lumeer.storage.mongodb.dao.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoDbTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MongoFavoriteItemDaoTest extends MongoDbTestBase {

   private static final String ORGANIZATION_ID = "596e3b86d412bc5a3caaa22a";

   private static final String PROJECT_ID1 = "prj1";
   private static final String PROJECT_ID2 = "prj2";

   private static final String COLLECTION_ID1 = "coll1";
   private static final String COLLECTION_ID2 = "coll2";

   private static final String VIEW_ID1 = "view1";
   private static final String VIEW_ID2 = "view2";

   private static final String DOCUMENT_ID1 = "doc1";
   private static final String DOCUMENT_ID2 = "doc2";

   private static final String USER = "testUser";
   private static final String USER2 = "testUser2";

   private MongoFavoriteItemDao dao;

   @BeforeEach
   public void initProjectDao() {
      Organization organization = Mockito.mock(Organization.class);
      Mockito.when(organization.getId()).thenReturn(ORGANIZATION_ID);

      dao = new MongoFavoriteItemDao();
      dao.setDatabase(database);

      dao.setOrganization(organization);
      dao.createRepository(organization);
   }

   @Test
   public void testAddFavoriteCollection() {
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1);
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID2);

      dao.addFavoriteCollection(USER2, PROJECT_ID2, COLLECTION_ID2);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).containsOnly(COLLECTION_ID1, COLLECTION_ID2);
      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID2)).isEmpty();
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID2)).containsOnly(COLLECTION_ID2);
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID1)).isEmpty();
   }

   @Test
   public void testAddFavoriteView() {
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1);
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID2);

      dao.addFavoriteView(USER2, PROJECT_ID2, VIEW_ID2);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).containsOnly(VIEW_ID1, VIEW_ID2);
      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID2)).isEmpty();
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID2)).containsOnly(VIEW_ID2);
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID1)).isEmpty();
   }

   @Test
   public void testAddFavoriteDocument() {
      dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID1);
      dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID2);

      dao.addFavoriteDocument(USER2, PROJECT_ID2, COLLECTION_ID2, DOCUMENT_ID1);

      assertThat(dao.getFavoriteDocumentIds(USER, PROJECT_ID1)).containsOnly(DOCUMENT_ID1, DOCUMENT_ID2);
      assertThat(dao.getFavoriteDocumentIds(USER, PROJECT_ID2)).isEmpty();
      assertThat(dao.getFavoriteDocumentIds(USER2, PROJECT_ID2)).containsOnly(DOCUMENT_ID1);
      assertThat(dao.getFavoriteDocumentIds(USER2, PROJECT_ID1)).isEmpty();
   }

   @Test
   public void testAddFavoriteCollectionDuplicate() {
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1);
      assertThatThrownBy(() -> dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testAddFavoriteViewDuplicate() {
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1);
      assertThatThrownBy(() -> dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testAddFavoriteDocumentDuplicate() {
      dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID1);
      assertThatThrownBy(() -> dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID1))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testRemoveFavoriteCollection() {
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1);
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID2);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).containsOnly(COLLECTION_ID1, COLLECTION_ID2);

      dao.removeFavoriteCollection(USER, COLLECTION_ID1);
      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).containsOnly(COLLECTION_ID2);
   }

   @Test
   public void testRemoveFavoriteView() {
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1);
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID2);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).containsOnly(VIEW_ID1, VIEW_ID2);

      dao.removeFavoriteView(USER, VIEW_ID1);
      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).containsOnly(VIEW_ID2);
   }

   @Test
   public void testRemoveFavoriteDocument() {
      dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID1);
      dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID2);

      assertThat(dao.getFavoriteDocumentIds(USER, PROJECT_ID1)).containsOnly(DOCUMENT_ID1, DOCUMENT_ID2);

      dao.removeFavoriteDocument(USER, DOCUMENT_ID1);
      assertThat(dao.getFavoriteDocumentIds(USER, PROJECT_ID1)).containsOnly(DOCUMENT_ID2);
   }

   @Test
   public void testRemoveFavoriteCollectionAll() {
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1);
      dao.addFavoriteCollection(USER2, PROJECT_ID1, COLLECTION_ID1);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).containsOnly(COLLECTION_ID1);
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID1)).containsOnly(COLLECTION_ID1);

      dao.removeFavoriteCollectionFromUsers(PROJECT_ID1, COLLECTION_ID1);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).isEmpty();
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID1)).isEmpty();

   }

   @Test
   public void testRemoveFavoriteCollectionByProject() {
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1);
      dao.addFavoriteCollection(USER, PROJECT_ID2, COLLECTION_ID1);
      dao.addFavoriteCollection(USER2, PROJECT_ID1, COLLECTION_ID2);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).containsOnly(COLLECTION_ID1);
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID1)).containsOnly(COLLECTION_ID2);

      dao.removeFavoriteCollectionsByProjectFromUsers(PROJECT_ID1);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).isEmpty();
      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID2)).containsOnly(COLLECTION_ID1);
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID1)).isEmpty();
   }

   @Test
   public void testGetFavoriteCollectionsIds() {
      dao.addFavoriteCollection(USER, PROJECT_ID1, COLLECTION_ID1);
      dao.addFavoriteCollection(USER, PROJECT_ID2, COLLECTION_ID1);
      dao.addFavoriteCollection(USER2, PROJECT_ID1, COLLECTION_ID2);

      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID1)).containsOnly(COLLECTION_ID1);
      assertThat(dao.getFavoriteCollectionIds(USER, PROJECT_ID2)).containsOnly(COLLECTION_ID1);
      assertThat(dao.getFavoriteCollectionIds(USER2, PROJECT_ID1)).containsOnly(COLLECTION_ID2);
   }

   @Test
   public void testRemoveFavoriteViewAll() {
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1);
      dao.addFavoriteView(USER2, PROJECT_ID1, VIEW_ID1);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).containsOnly(VIEW_ID1);
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID1)).containsOnly(VIEW_ID1);

      dao.removeFavoriteViewFromUsers(PROJECT_ID1, VIEW_ID1);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).isEmpty();
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID1)).isEmpty();

   }

   @Test
   public void testRemoveFavoriteViewByProject() {
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1);
      dao.addFavoriteView(USER, PROJECT_ID2, VIEW_ID1);
      dao.addFavoriteView(USER2, PROJECT_ID1, VIEW_ID2);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).containsOnly(VIEW_ID1);
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID1)).containsOnly(VIEW_ID2);

      dao.removeFavoriteViewByProjectFromUsers(PROJECT_ID1);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).isEmpty();
      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID2)).containsOnly(VIEW_ID1);
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID1)).isEmpty();
   }

   @Test
   public void testGetFavoriteViewsIds() {
      dao.addFavoriteView(USER, PROJECT_ID1, VIEW_ID1);
      dao.addFavoriteView(USER, PROJECT_ID2, VIEW_ID1);
      dao.addFavoriteView(USER2, PROJECT_ID1, VIEW_ID2);

      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID1)).containsOnly(VIEW_ID1);
      assertThat(dao.getFavoriteViewIds(USER, PROJECT_ID2)).containsOnly(VIEW_ID1);
      assertThat(dao.getFavoriteViewIds(USER2, PROJECT_ID1)).containsOnly(VIEW_ID2);
   }

   @Test
   public void testGetFavoriteDocumentsIds() {
      dao.addFavoriteDocument(USER, PROJECT_ID1, COLLECTION_ID1, DOCUMENT_ID1);
      dao.addFavoriteDocument(USER, PROJECT_ID2, COLLECTION_ID1, DOCUMENT_ID2);
      dao.addFavoriteDocument(USER2, PROJECT_ID1, COLLECTION_ID2, DOCUMENT_ID1);

      assertThat(dao.getFavoriteDocumentIds(USER, PROJECT_ID1)).containsOnly(DOCUMENT_ID1);
      assertThat(dao.getFavoriteDocumentIds(USER, PROJECT_ID2)).containsOnly(DOCUMENT_ID2);
      assertThat(dao.getFavoriteDocumentIds(USER2, PROJECT_ID1)).containsOnly(DOCUMENT_ID1);
   }

}
