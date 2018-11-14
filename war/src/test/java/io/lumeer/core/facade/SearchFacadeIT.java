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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class SearchFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "LMR";
   private static final String PROJECT_CODE = "PROJ";

   private static final List<String> COLLECTION_CODES = Arrays.asList("Collection1", "Collection2", "Collection3");
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String DOCUMENT_KEY = "attribute";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private List<String> collectionIds = new ArrayList<>();

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private DataDao dataDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureCollections() {
      Organization organization = new Organization ();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      Project  project = new Project ();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(storedProject);
      documentDao.setProject(storedProject);

      collectionIds.clear();

      for (String name : COLLECTION_CODES) {
         Permissions collectionPermissions = new Permissions();
         collectionPermissions.updateUserPermissions(new Permission(createdUser.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
         Collection jsonCollection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         jsonCollection.setDocumentsCount(0);
         String collectionId = collectionDao.createCollection(jsonCollection).getId();
         collectionIds.add(collectionId);
         dataDao.createDataRepository(collectionId);
      }
   }

   @Test
   public void testSearchCollectionsByEmptyQuery() {
      List<Collection> collections = searchFacade.searchCollections(new Query());
      assertThat(collections).extracting(Collection::getId).containsOnlyElementsOf(collectionIds);
   }

   @Test
   public void testSearchCollectionsByDocumentsFullText() {
      createDocument(collectionIds.get(0), "word");
      createDocument(collectionIds.get(0), "fulltext");
      createDocument(collectionIds.get(1), "something fulltext");
      createDocument(collectionIds.get(1), "some other word");
      createDocument(collectionIds.get(2), "full word");

      List<Collection> collections = searchFacade.searchCollections(new Query("fulltext"));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));

      collections = searchFacade.searchCollections(new Query("word"));
      assertThat(collections).extracting(Collection::getId).containsOnlyElementsOf(collectionIds);
   }

   @Test
   public void testSearchCollectionsByDocumentsIds() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Collection> collections = searchFacade.searchCollections(new Query(null, null, new HashSet<>(Arrays.asList(id1, id2, id3))));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));

      collections = searchFacade.searchCollections(new Query(null, null, new HashSet<>(Arrays.asList(id1, id4, id5))));
      assertThat(collections).extracting(Collection::getId).containsOnlyElementsOf(collectionIds);

      collections = searchFacade.searchCollections(new Query(null, null, new HashSet<>(Arrays.asList(id5, id6))));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(2));
   }

   @Test
   public void testSearchCollectionsByCollectionIds() {
      List<Collection> collections = searchFacade.searchCollections(new Query(new HashSet<>(Arrays.asList(collectionIds.get(0), collectionIds.get(2))), null, null));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(2));

      collections = searchFacade.searchCollections(new Query(Collections.singleton(collectionIds.get(1)), null, null));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(1));
   }

   @Test
   public void testSearchCollectionsCombination() {
      createDocument(collectionIds.get(0), "word").getId();
      String id2 = createDocument(collectionIds.get(0), "fulltext").getId();
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      createDocument(collectionIds.get(1), "some other word").getId();
      String id5 = createDocument(collectionIds.get(2), "full word").getId();

      List<Collection> collections = searchFacade.searchCollections(new Query(Collections.singleton(collectionIds.get(0)), null, Collections.singleton(id3)));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));
   }

   @Test
   public void testSearchDocumentsByEmptyQuery() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Document> documents = searchFacade.searchDocuments(new Query());
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5, id6);
   }

   @Test
   public void testSearchDocumentsByIds() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Document> documents = searchFacade.searchDocuments(new Query(null, null, new HashSet<>(Arrays.asList(id1, id4, id6))));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id4, id6);

      documents = searchFacade.searchDocuments(new Query(null, null, new HashSet<>(Arrays.asList(id2, id3, id4, id5))));
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3, id4, id5);
   }

   @Test
   public void testSearchDocumentsByFullText() {
      String id1 = createDocument(collectionIds.get(0), "word").getId();
      String id2 = createDocument(collectionIds.get(0), "fulltext").getId();
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      String id4 = createDocument(collectionIds.get(1), "some other word").getId();
      String id5 = createDocument(collectionIds.get(2), "full word").getId();

      List<Document> documents = searchFacade.searchDocuments(new Query("fulltext"));
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3);

      documents = searchFacade.searchDocuments(new Query("word"));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id4, id5);
   }

   @Test
   public void testSearchDocumentsByCollectionIds() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      createDocument(collectionIds.get(1), "doc3");
      createDocument(collectionIds.get(1), "doc4");
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Document> documents = searchFacade.searchDocuments(new Query(new HashSet<>(Arrays.asList(collectionIds.get(0), collectionIds.get(2))), null, null));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id5, id6);
   }

   @Test
   public void testSearchDocumentsByCombination() {
      createDocument(collectionIds.get(0), "word");
      createDocument(collectionIds.get(0), "fulltext");
      createDocument(collectionIds.get(1), "something fulltext");
      createDocument(collectionIds.get(1), "some other word anything");
      createDocument(collectionIds.get(2), "full word");
      createDocument(collectionIds.get(2), "anything");

      List<Document> documents = searchFacade.searchDocuments(new Query(null, Collections.singleton(collectionIds.get(0)), null, null, "anything", null, null));
      assertThat(documents).extracting(Document::getId).isEmpty();
   }

   @Test
   public void testSearchDocumentsAttributeStringEquals() {
      String id1 = createDocument(collectionIds.get(0), "something").getId();
      String id2 = createDocument(collectionIds.get(0), "something else").getId();
      String id3 = createDocument(collectionIds.get(0), "Lumiik").getId();
      createDocument(collectionIds.get(1), "something");

      List<Document> documents = searchFacade.searchDocuments(attributeFilterQuery(collectionIds.get(0), "=", "something"));
      assertThat(documents).extracting(Document::getId).containsOnly(id1);

      documents = searchFacade.searchDocuments(attributeFilterQuery(collectionIds.get(0), "!=", "something"));
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3);
   }

   @Test
   public void testSearchCollectionsAttributeStringEquals() {
      createDocument(collectionIds.get(0), "something");
      createDocument(collectionIds.get(0), "something else");
      createDocument(collectionIds.get(0), "Lumiik");
      createDocument(collectionIds.get(1), "something");

      List<Collection> collections = searchFacade.searchCollections(attributeFilterQuery(collectionIds.get(0), "=", "something"));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0));

      collections = searchFacade.searchCollections(attributeFilterQuery(collectionIds.get(1), "!=", "something"));
      assertThat(collections).extracting(Collection::getId).isEmpty();
   }

   @Test
   public void testChildDocuments() {
      final Document a0 = createDocument(collectionIds.get(0), "a0");
      final Document b1 = createDocument(collectionIds.get(0), "b1", a0.getId());
      final Document b2 = createDocument(collectionIds.get(0), "b2", a0.getId());
      final Document c1 = createDocument(collectionIds.get(0), "c1", b1.getId());
      final Document c2 = createDocument(collectionIds.get(0), "c2", b1.getId());
      final Document c3 = createDocument(collectionIds.get(0), "c3", b1.getId());
      final Document d1 = createDocument(collectionIds.get(0), "d1", b2.getId());
      final Document d2 = createDocument(collectionIds.get(0), "d2", b2.getId());
      final Document d3 = createDocument(collectionIds.get(0), "d3", b2.getId());
      final Document e1 = createDocument(collectionIds.get(0), "e1", c2.getId());
      final Document e2 = createDocument(collectionIds.get(0), "e2", c2.getId());

      List<Document> documents = searchFacade.searchDocuments(new Query(Collections.emptySet(), Collections.emptySet(), Collections.singleton(a0.getId())));
      documents.forEach(System.out::println);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(
            documents.stream().filter(d -> d.getMetaData() == null || d.getMetaData().get(Document.META_PARENT_ID) == null).count())
            .isEqualTo(1);
      assertions.assertThat(documents).hasSize(11);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("a0", "b1", "b2", "c1", "c2", "c3", "d1", "d2", "d3", "e1", "e2");
      assertions.assertAll();

      documents = searchFacade.searchDocuments(new Query(Collections.emptySet(), Collections.emptySet(), Collections.singleton(b2.getId())));
      assertions = new SoftAssertions();
      assertions.assertThat(documents).hasSize(4);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("b2", "d1", "d2", "d3");
      assertions.assertAll();

      documents = searchFacade.searchDocuments(new Query(Collections.emptySet(), Collections.emptySet(), Collections.singleton(b1.getId())));
      assertions = new SoftAssertions();
      assertions.assertThat(documents).hasSize(6);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("b1", "c1", "c2", "c3", "e1", "e2");
      assertions.assertAll();
   }

   private Query attributeFilterQuery(String collectionId, String operator, String value) {
      String filter = collectionId + ":" + DOCUMENT_KEY + ":" + operator + " " + value;
      return new Query(Collections.singleton(filter), null, null, null, null, null, null);
   }

   private Document createDocument(String collectionId, Object value) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      final String id = DOCUMENT_KEY; // use the same document id for simplicity in tests
      if (!collection.getAttributes().stream().anyMatch(attr -> attr.getName().equals(DOCUMENT_KEY))) {
         collection.createAttribute(new Attribute(id, DOCUMENT_KEY, Collections.emptySet(), 1));
         collection.setLastAttributeNum(collection.getLastAttributeNum() + 1);
         collectionDao.updateCollection(collectionId, collection);
      } else {
         Attribute attr = collection.getAttributes().stream().filter(a -> a.getName().equals(DOCUMENT_KEY)).findFirst().get();
         attr.setUsageCount(attr.getUsageCount() + 1);
         collectionDao.updateCollection(collectionId, collection);
      }

      Document document = new Document(new DataDocument(id, value));
      document.setCollectionId(collectionId);
      document.setCreatedBy(USER);
      document.setCreationDate(ZonedDateTime.now());
      document.setDataVersion(DocumentFacade.INITIAL_VERSION);
      Document storedDocument = documentDao.createDocument(document);

      DataDocument storedData = dataDao.createData(collectionId, storedDocument.getId(), document.getData());

      storedDocument.setData(storedData);
      return storedDocument;
   }

   private Document createDocument(final String collectionId, final String value, final String parentId) {
      final Document doc = createDocument(collectionId, value);
      doc.setMetaData(new DataDocument(Document.META_PARENT_ID, parentId));
      return documentDao.updateDocument(doc.getId(), doc);
   }
}
