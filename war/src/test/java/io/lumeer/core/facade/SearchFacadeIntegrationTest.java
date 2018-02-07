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

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class SearchFacadeIntegrationTest extends IntegrationTestBase {

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
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization =organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      userDao.createUser(storedOrganization.getId(), user);

      JsonProject project = new JsonProject();
      project.setPermissions(new JsonPermissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
      collectionDao.createCollectionsRepository(storedProject);
      documentDao.setProject(storedProject);

      collectionIds.clear();

      for (String name : COLLECTION_CODES) {
         JsonPermissions collectionPermissions = new JsonPermissions();
         collectionPermissions.updateUserPermissions(new JsonPermission(USER, Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
         JsonCollection jsonCollection = new JsonCollection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         jsonCollection.setDocumentsCount(0);
         String collectionId = collectionDao.createCollection(jsonCollection).getId();
         collectionIds.add(collectionId);
         dataDao.createDataRepository(collectionId);
      }
   }

   @Test
   public void testSearchCollectionsByEmptyQuery() {
      List<Collection> collections = searchFacade.searchCollections(new JsonQuery());
      assertThat(collections).extracting(Collection::getId).containsOnlyElementsOf(collectionIds);
   }

   @Test
   public void testSearchCollectionsByDocumentsFullText() {
      createDocument(collectionIds.get(0), "word");
      createDocument(collectionIds.get(0), "fulltext");
      createDocument(collectionIds.get(1), "something fulltext");
      createDocument(collectionIds.get(1), "some other word");
      createDocument(collectionIds.get(2), "full word");

      List<Collection> collections = searchFacade.searchCollections(new JsonQuery("fulltext"));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));

      collections = searchFacade.searchCollections(new JsonQuery("word"));
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

      List<Collection> collections = searchFacade.searchCollections(new JsonQuery(null, null, new HashSet<>(Arrays.asList(id1, id2, id3))));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));

      collections = searchFacade.searchCollections(new JsonQuery(null, null, new HashSet<>(Arrays.asList(id1, id4, id5))));
      assertThat(collections).extracting(Collection::getId).containsOnlyElementsOf(collectionIds);

      collections = searchFacade.searchCollections(new JsonQuery(null, null, new HashSet<>(Arrays.asList(id5, id6))));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(2));
   }

   @Test
   public void testSearchCollectionsByCollectionCodes() {
      List<Collection> collections = searchFacade.searchCollections(new JsonQuery(new HashSet<>(Arrays.asList(COLLECTION_CODES.get(0), COLLECTION_CODES.get(1))), null,null, null, null, null, null,null ));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));

      collections = searchFacade.searchCollections(new JsonQuery(Collections.singleton(COLLECTION_CODES.get(2)), null,null, null, null, null, null,null ));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(2));
   }

   @Test
   public void testSearchCollectionsByCollectionIds() {
      List<Collection> collections = searchFacade.searchCollections(new JsonQuery(new HashSet<>(Arrays.asList(collectionIds.get(0), collectionIds.get(2))), null, null));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(2));

      collections = searchFacade.searchCollections(new JsonQuery(Collections.singleton(collectionIds.get(1)), null, null));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(1));
   }

   @Test
   public void testSearchCollectionsCombination() {
      createDocument(collectionIds.get(0), "word").getId();
      String id2 = createDocument(collectionIds.get(0), "fulltext").getId();
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      createDocument(collectionIds.get(1), "some other word").getId();
      String id5 = createDocument(collectionIds.get(2), "full word").getId();

      List<Collection> collections = searchFacade.searchCollections(new JsonQuery(Collections.singleton(collectionIds.get(0)), null, Collections.singleton(id3)));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(1));

      collections = searchFacade.searchCollections(new JsonQuery(null, null, null, null, new HashSet<>(Arrays.asList(id2, id5)), "fulltext", null, null));
      assertThat(collections).extracting(Collection::getId).containsOnly(collectionIds.get(0), collectionIds.get(2));
   }

   @Test
   public void testSearchDocumentsByEmptyQuery() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Document> documents = searchFacade.searchDocuments(new JsonQuery());
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

      List<Document> documents = searchFacade.searchDocuments(new JsonQuery(null, null, new HashSet<>(Arrays.asList(id1, id4, id6))));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id4, id6);

      documents = searchFacade.searchDocuments(new JsonQuery(null, null, new HashSet<>(Arrays.asList(id2, id3, id4, id5))));
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3, id4, id5);
   }

   @Test
   public void testSearchDocumentsByFullText() {
      String id1 = createDocument(collectionIds.get(0), "word").getId();
      String id2 = createDocument(collectionIds.get(0), "fulltext").getId();
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      String id4 = createDocument(collectionIds.get(1), "some other word").getId();
      String id5 = createDocument(collectionIds.get(2), "full word").getId();

      List<Document> documents = searchFacade.searchDocuments(new JsonQuery("fulltext"));
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3);

      documents = searchFacade.searchDocuments(new JsonQuery("word"));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id4, id5);
   }

   @Test
   public void testSearchDocumentsByCollections() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Document> documents = searchFacade.searchDocuments(new JsonQuery(new HashSet<>(Arrays.asList(collectionIds.get(0), collectionIds.get(2))), null, null));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id5, id6);

      documents = searchFacade.searchDocuments(new JsonQuery(new HashSet<>(Arrays.asList(COLLECTION_CODES.get(0), COLLECTION_CODES.get(1))), null,null, null, null, null, null,null ));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4);

      documents = searchFacade.searchDocuments(new JsonQuery(Collections.singleton(COLLECTION_CODES.get(1)), null,new HashSet<>(Arrays.asList(collectionIds.get(0), collectionIds.get(1))), null, null, null, null,null ));
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4);
   }

   @Test
   public void testSearchDocumentsByCombination() {
      createDocument(collectionIds.get(0), "word").getId();
      String id2 = createDocument(collectionIds.get(0), "fulltext").getId();
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      String id4 = createDocument(collectionIds.get(1), "some other word anything").getId();
      createDocument(collectionIds.get(2), "full word").getId();
      createDocument(collectionIds.get(2), "anything").getId();

      List<Document> documents = searchFacade.searchDocuments(new JsonQuery(null, null, Collections.singleton(collectionIds.get(0)), null, null, "anything", null, null));
      assertThat(documents).extracting(Document::getId).isEmpty();

      documents = searchFacade.searchDocuments(new JsonQuery(Collections.singleton(COLLECTION_CODES.get(1)), null, null, null, null, "fulltext", null, null));
      assertThat(documents).extracting(Document::getId).containsOnly(id3);

   }


   private Document createDocument(String collectionId, String value) {
      Document document = new JsonDocument(new DataDocument(DOCUMENT_KEY, value));
      document.setCollectionId(collectionId);
      document.setCreatedBy(USER);
      document.setCreationDate(LocalDateTime.now());
      document.setDataVersion(DocumentFacade.INITIAL_VERSION);
      Document storedDocument = documentDao.createDocument(document);

      DataDocument storedData = dataDao.createData(collectionId, storedDocument.getId(), document.getData());

      storedDocument.setData(storedData);
      return storedDocument;
   }
}
