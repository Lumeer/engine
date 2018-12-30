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
import io.lumeer.api.model.AttributeFilter;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
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
import java.util.Set;
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
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureCollections() {
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(storedProject);
      documentDao.setProject(storedProject);
      linkInstanceDao.setProject(project);
      linkTypeDao.setProject(project);

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
   public void testSearchDocumentsByCollectionIds() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      createDocument(collectionIds.get(1), "doc3");
      createDocument(collectionIds.get(1), "doc4");
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      QueryStem stem1 = new QueryStem(collectionIds.get(0));
      QueryStem stem2 = new QueryStem(collectionIds.get(2));
      Query query = new Query(Arrays.asList(stem1, stem2));

      List<Document> documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id5, id6);
   }

   @Test
   public void testSearchDocumentsByIds() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      createDocument(collectionIds.get(0), "doc2");
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      createDocument(collectionIds.get(2), "doc5");
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      QueryStem stem1 = new QueryStem(collectionIds.get(0), Collections.emptyList(), Collections.singleton(id1), Collections.emptySet());
      QueryStem stem2 = new QueryStem(collectionIds.get(1), Collections.emptyList(), new HashSet<>(Arrays.asList(id3, id4)), Collections.emptySet());
      QueryStem stem3 = new QueryStem(collectionIds.get(2), Collections.emptyList(), Collections.singleton(id6), Collections.emptySet());
      Query query = new Query(Arrays.asList(stem1, stem2, stem3));

      List<Document> documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3, id4, id6);
   }

   @Test
   public void testSearchDocumentsByFullTexts() {
      createDocument(collectionIds.get(0), "word");
      createDocument(collectionIds.get(0), "fulltext");
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      String id4 = createDocument(collectionIds.get(1), "some other word").getId();
      String id5 = createDocument(collectionIds.get(2), "full word").getId();

      Query query = new Query(Collections.emptyList(), new HashSet<>(Collections.singletonList("some")), null, null);
      List<Document> documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id3, id4);

      query = new Query(Collections.emptyList(), new HashSet<>(Arrays.asList("full", "word")), null, null);
      documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id5);
   }

   @Test
   public void testSearchDocumentsByFiltersAndFulltexts() {
      createDocument(collectionIds.get(0), "word");
      String id2 = createDocument(collectionIds.get(0), "lmr").getId();
      String id3 = createDocument(collectionIds.get(0), "wlmrd").getId();
      String id4 = createDocument(collectionIds.get(0), "lalamr").getId();
      createDocument(collectionIds.get(1), "something fulltext");
      String id6 = createDocument(collectionIds.get(1), "lmr").getId();
      String id7 = createDocument(collectionIds.get(1), "other lmr").getId();
      String id8 = createDocument(collectionIds.get(2), "full wordmr").getId();

      Query query = new Query(Collections.emptyList(), new HashSet<>(Collections.singletonList("mr")), null, null);
      List<Document> documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3, id4, id6, id7, id8);

      Set<AttributeFilter> filters = Collections.singleton(new AttributeFilter(collectionIds.get(0), DOCUMENT_KEY, "=", "lmr"));
      QueryStem stem = new QueryStem(collectionIds.get(0), Collections.emptyList(), Collections.emptySet(), filters);
      query = new Query(Collections.singletonList(stem), new HashSet<>(Collections.singletonList("mr")), null, null);
      documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id2);

      filters = new HashSet<>(Arrays.asList(new AttributeFilter(collectionIds.get(0), DOCUMENT_KEY, "=", "lmr"),
            new AttributeFilter(collectionIds.get(1), DOCUMENT_KEY, "=", "other lmr")));
      List<QueryStem> stems = Arrays.asList(new QueryStem(collectionIds.get(0), Collections.emptyList(), Collections.emptySet(), filters),
            new QueryStem(collectionIds.get(1), Collections.emptyList(), Collections.emptySet(), filters));
      query = new Query(stems, new HashSet<>(Collections.singletonList("mr")), null, null);
      documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id7);
   }

   @Test
   public void testSearchDocumentsWithLinks() {
      String id1 = createDocument(collectionIds.get(0), "lumeer lol").getId();
      String id2 = createDocument(collectionIds.get(0), "lol").getId();
      String id3 = createDocument(collectionIds.get(0), "lmr").getId();
      String id4 = createDocument(collectionIds.get(0), "lalamr").getId();
      String id5 = createDocument(collectionIds.get(0), "something fulltext").getId();
      String id6 = createDocument(collectionIds.get(1), "lmr").getId();
      String id7 = createDocument(collectionIds.get(1), "other lol").getId();
      String id8 = createDocument(collectionIds.get(1), "o lmr").getId();
      String id9 = createDocument(collectionIds.get(1), "lumeer").getId();
      String id10 = createDocument(collectionIds.get(2), "mr").getId();
      String id11 = createDocument(collectionIds.get(2), "mr lumr").getId();
      String id12 = createDocument(collectionIds.get(2), "lol").getId();
      String linkType01Id = linkTypeDao.createLinkType(new LinkType(null, "lmr",
            Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList())).getId();
      String linkType12Id = linkTypeDao.createLinkType(new LinkType(null, "lmrr",
            Arrays.asList(collectionIds.get(1), collectionIds.get(2)), Collections.emptyList())).getId();
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType01Id, Arrays.asList(id1, id6), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType01Id, Arrays.asList(id1, id7), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType01Id, Arrays.asList(id2, id8), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType01Id, Arrays.asList(id3, id8), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType01Id, Arrays.asList(id4, id6), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType12Id, Arrays.asList(id6, id10), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType12Id, Arrays.asList(id6, id11), new DataDocument()));
      linkInstanceDao.createLinkInstance(new LinkInstance(null, linkType12Id, Arrays.asList(id7, id12), new DataDocument()));

      QueryStem stem = new QueryStem(collectionIds.get(0), Arrays.asList(linkType01Id, linkType12Id), Collections.emptySet(), Collections.emptySet());
      Query query = new Query(Collections.singletonList(stem), Collections.emptySet(), null, null);
      List<Document> documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5, id6, id7, id8, id10, id11, id12);

      stem = new QueryStem(collectionIds.get(0), Arrays.asList(linkType01Id, linkType12Id), new HashSet<>(Arrays.asList(id1, id2, id3, id4, id5, id6, id7, id8, id11, id12)), Collections.emptySet());
      query = new Query(Collections.singletonList(stem), Collections.singleton("lol"), null, null);
      documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id7, id12);

      stem = new QueryStem(collectionIds.get(0), Arrays.asList(linkType01Id, linkType12Id), new HashSet<>(Arrays.asList(id1, id2, id9)), Collections.emptySet());
      query = new Query(Collections.singletonList(stem), Collections.singleton("lol"), null, null);
      documents = searchFacade.searchDocuments(query);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2);
   }

   @Test
   public void testChildDocuments() {
      final Document a0 = createDocument(collectionIds.get(0), "a0");
      final Document b1 = createDocument(collectionIds.get(0), "b1", a0.getId());
      final Document b2 = createDocument(collectionIds.get(0), "b2", a0.getId());
      createDocument(collectionIds.get(0), "c1", b1.getId());
      final Document c2 = createDocument(collectionIds.get(0), "c2", b1.getId());
      createDocument(collectionIds.get(0), "c3", b1.getId());
      createDocument(collectionIds.get(0), "d1", b2.getId());
      createDocument(collectionIds.get(0), "d2", b2.getId());
      createDocument(collectionIds.get(0), "d3", b2.getId());
      createDocument(collectionIds.get(0), "e1", c2.getId());
      createDocument(collectionIds.get(0), "e2", c2.getId());

      Query query = new Query(new QueryStem(collectionIds.get(0), Collections.emptyList(), Collections.singleton(a0.getId()), Collections.emptySet()));
      List<Document> documents = searchFacade.searchDocuments(query);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(
            documents.stream().filter(d -> d.getMetaData() == null || d.getMetaData().get(Document.META_PARENT_ID) == null).count())
                .isEqualTo(1);
      assertions.assertThat(documents).hasSize(11);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("a0", "b1", "b2", "c1", "c2", "c3", "d1", "d2", "d3", "e1", "e2");
      assertions.assertAll();

      query = new Query(new QueryStem(collectionIds.get(0), Collections.emptyList(), Collections.singleton(b2.getId()), Collections.emptySet()));
      documents = searchFacade.searchDocuments(query);
      assertions = new SoftAssertions();
      assertions.assertThat(documents).hasSize(4);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("b2", "d1", "d2", "d3");
      assertions.assertAll();

      query = new Query(new QueryStem(collectionIds.get(0), Collections.emptyList(), Collections.singleton(b1.getId()), Collections.emptySet()));
      documents = searchFacade.searchDocuments(query);
      assertions = new SoftAssertions();
      assertions.assertThat(documents).hasSize(6);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("b1", "c1", "c2", "c3", "e1", "e2");
      assertions.assertAll();
   }

   private Document createDocument(String collectionId, Object value) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      final String id = DOCUMENT_KEY; // use the same document id for simplicity in tests
      if (!collection.getAttributes().stream().anyMatch(attr -> attr.getName().equals(DOCUMENT_KEY))) {
         collection.createAttribute(new Attribute(id, DOCUMENT_KEY, null, 1));
         collection.setLastAttributeNum(collection.getLastAttributeNum() + 1);
         collectionDao.updateCollection(collectionId, collection, null);
      } else {
         Attribute attr = collection.getAttributes().stream().filter(a -> a.getName().equals(DOCUMENT_KEY)).findFirst().get();
         attr.setUsageCount(attr.getUsageCount() + 1);
         collectionDao.updateCollection(collectionId, collection, null);
      }

      Document document = new Document(new DataDocument(id, value));
      document.setCollectionId(collectionId);
      document.setCreatedBy(USER);
      document.setCreationDate(ZonedDateTime.now());
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

   @Test
   public void testSearchLinkInstances() {
      LinkType linkType = new LinkType(null, "nm", Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList());
      String linkTypeId1 = linkTypeDao.createLinkType(linkType).getId();
      LinkType linkType2 = new LinkType(null, "nm2", Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList());
      String linkTypeId2 = linkTypeDao.createLinkType(linkType2).getId();

      String id10 = createDocument(collectionIds.get(0), "lumeer").getId();
      String id11 = createDocument(collectionIds.get(0), "lumeer").getId();
      String id12 = createDocument(collectionIds.get(0), "lumeer").getId();
      String id20 = createDocument(collectionIds.get(1), "lumeer").getId();
      String id21 = createDocument(collectionIds.get(1), "lumeer").getId();
      String id22 = createDocument(collectionIds.get(1), "lumeer").getId();

      String id1 = linkInstanceDao.createLinkInstance(new LinkInstance(null, linkTypeId1, Arrays.asList(id10, id20), new DataDocument())).getId();

      LinkInstance linkInstance2 = new LinkInstance(null, linkTypeId1, Arrays.asList(id10, id22), new DataDocument());
      String id2 = linkInstanceDao.createLinkInstance(linkInstance2).getId();

      LinkInstance linkInstance3 = new LinkInstance(null, linkTypeId1, Arrays.asList(id11, id21), new DataDocument());
      linkInstance3.setLinkTypeId(linkTypeId1);
      linkInstance3.setDocumentIds(Arrays.asList(id11, id21));
      String id3 = linkInstanceDao.createLinkInstance(linkInstance3).getId();

      LinkInstance linkInstance4 = new LinkInstance(null, linkTypeId2, Arrays.asList(id10, id20), new DataDocument());
      linkInstance4.setLinkTypeId(linkTypeId2);
      linkInstance4.setDocumentIds(Arrays.asList(id10, id20));
      String id4 = linkInstanceDao.createLinkInstance(linkInstance4).getId();

      QueryStem stem1 = new QueryStem(collectionIds.get(0), null, Collections.singleton(id10), null);
      Query query1 = new Query(stem1);
      List<LinkInstance> linkInstances = searchFacade.getLinkInstances(query1);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id2, id4));

      QueryStem stem2 = new QueryStem(collectionIds.get(1), null, Collections.singleton(id21), null);
      Query query = new Query(stem2);
      linkInstances = searchFacade.getLinkInstances(query);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Collections.singletonList(id3));

      QueryStem stem3 = new QueryStem(collectionIds.get(0), Arrays.asList(linkTypeId1, linkTypeId2), null, null);
      Query query3 = new Query(stem3);
      linkInstances = searchFacade.getLinkInstances(query3);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id2, id3, id4));

      QueryStem stem4 = new QueryStem(collectionIds.get(0), Collections.singletonList(linkTypeId1), null, null);
      Query query4 = new Query(stem4);
      linkInstances = searchFacade.getLinkInstances(query4);
      assertThat(linkInstances).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id2, id3));
   }
}
