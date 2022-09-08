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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.ConditionValueType;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
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
import java.util.Map;
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
   private static final String USER1 = "some@user.com";
   private static final String USER2 = "other.user@lmr.com";

   private List<String> collectionIds = new ArrayList<>();
   private String userId;

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
   private LinkDataDao linkDataDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private PermissionsChecker permissionsChecker;

   private ConstraintManager constraintManager;

   @Before
   public void configureCollections() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);

      userId = createUser(USER, null).getId();

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      organizationPermissions.updateUserPermissions(Permission.buildWithRoles(userId, Set.of(new Role(RoleType.Read))));
      organization.setPermissions(organizationPermissions);
      Organization storedOrganization = organizationDao.createOrganization(organization);
      updateOrganizationInUser(userId, USER, organization.getId());
      createUser(USER1, organization.getId());
      createUser(USER2, organization.getId());

      projectDao.setOrganization(storedOrganization);

      Project project = new Project();
      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(userId, Set.of(new Role(RoleType.Read))));
      project.setPermissions(projectPermissions);
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      collectionDao.setProject(storedProject);
      collectionDao.createRepository(storedProject);
      documentDao.setProject(storedProject);
      linkInstanceDao.setProject(project);
      linkTypeDao.setProject(project);

      collectionIds.clear();

      for (String name : COLLECTION_CODES) {
         collectionIds.add(createCollection(name).getId());
      }
      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   private User createUser(String email, String organizationId) {
      User user = new User(null, email, email, organizationId != null ? Collections.singleton(organizationId) : null);
      return userDao.createUser(user);
   }

   private User updateOrganizationInUser(String userId, String email, String organizationId) {
      assert organizationId != null;
      User user = new User(userId, email, email, Collections.singleton(organizationId));
      return userDao.updateUser(userId, user);
   }

   private Collection createCollection(String name, Attribute... attributes) {
      Permissions collectionPermissions = new Permissions();
      collectionPermissions.updateUserPermissions(new Permission(userId, Collection.ROLES));
      Collection collection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
      collection.setAttributes(Arrays.asList(attributes));

      Collection createdCollection = collectionDao.createCollection(collection);
      dataDao.createDataRepository(createdCollection.getId());
      return createdCollection;
   }

   private Collection setCollectionUserRoles(Collection collection, final Set<Role> roles) {
      Permissions permissions = collection.getPermissions();
      permissions.updateUserPermissions(Permission.buildWithRoles(userId, roles));
      collection.setPermissions(permissions);
      return collectionDao.updateCollection(collection.getId(), collection, null);
   }

   private Collection createTaskCollection(String name, CollectionPurpose purpose, Attribute... attributes) {
      Collection collection = createCollection(name, attributes);
      collection.setPurpose(purpose);
      return collectionDao.updateCollection(collection.getId(), collection, null, false);
   }

   @Test
   public void testSearchDocumentsByEmptyQuery() {
      String id1 = createDocument(collectionIds.get(0), "doc1").getId();
      String id2 = createDocument(collectionIds.get(0), "doc2").getId();
      String id3 = createDocument(collectionIds.get(1), "doc3").getId();
      String id4 = createDocument(collectionIds.get(1), "doc4").getId();
      String id5 = createDocument(collectionIds.get(2), "doc5").getId();
      String id6 = createDocument(collectionIds.get(2), "doc6").getId();

      List<Document> documents = searchFacade.searchDocuments(new Query(), true);
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

      List<Document> documents = searchFacade.searchDocuments(query, true);
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

      QueryStem stem1 = new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.singleton(id1), Collections.emptyList(), Collections.emptyList());
      QueryStem stem2 = new QueryStem(null, collectionIds.get(1), Collections.emptyList(), new HashSet<>(Arrays.asList(id3, id4)), Collections.emptyList(), Collections.emptyList());
      QueryStem stem3 = new QueryStem(null, collectionIds.get(2), Collections.emptyList(), Collections.singleton(id6), Collections.emptyList(), Collections.emptyList());
      Query query = new Query(Arrays.asList(stem1, stem2, stem3));

      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3, id4, id6);
   }

   @Test
   public void testSearchDocumentsByFullTexts() {
      String id1 = createDocument(collectionIds.get(0), "word").getId();
      String id2 = createDocument(collectionIds.get(0), "fulltext").getId();
      String id3 = createDocument(collectionIds.get(1), "something fulltext").getId();
      String id4 = createDocument(collectionIds.get(1), "some other word").getId();
      String id5 = createDocument(collectionIds.get(2), "full word").getId();

      Query query = new Query(Collections.emptyList(), new HashSet<>(Collections.singletonList("some")), null, null);
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id3, id4);

      query = new Query(Collections.emptyList(), new HashSet<>(Arrays.asList("full", "word")), null, null);
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5);
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
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3, id4, id6, id7, id8);

      List<CollectionAttributeFilter> filters = Collections.singletonList(CollectionAttributeFilter.createFromValues(collectionIds.get(0), DOCUMENT_KEY, ConditionType.EQUALS, "lmr"));
      QueryStem stem = new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.emptySet(), filters, Collections.emptyList());
      query = new Query(Collections.singletonList(stem), new HashSet<>(Collections.singletonList("mr")), null, null);
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2);

      filters = Arrays.asList(CollectionAttributeFilter.createFromValues(collectionIds.get(0), DOCUMENT_KEY, ConditionType.EQUALS, "lmr"),
            CollectionAttributeFilter.createFromValues(collectionIds.get(1), DOCUMENT_KEY, ConditionType.EQUALS, "other lmr"));
      List<QueryStem> stems = Arrays.asList(new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.emptySet(), filters, Collections.emptyList()),
            new QueryStem(null, collectionIds.get(1), Collections.emptyList(), Collections.emptySet(), filters, Collections.emptyList()));
      query = new Query(stems, new HashSet<>(Collections.singletonList("mr")), null, null);
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id7);
   }

   @Test
   public void testSearchDocumentsByNumberConstraint() {
      Constraint constraint = new Constraint(ConstraintType.Number, new DataDocument());
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("numberCollection", attribute).getId();

      String id1 = createDocument(collectionId, "20.3").getId();
      String id2 = createDocument(collectionId, "40.1").getId();
      String id3 = createDocument(collectionId, "60").getId();
      String id4 = createDocument(collectionId, 80).getId();
      String id5 = createDocument(collectionId, "100.2").getId();
      String id6 = createDocument(collectionId, "-30.123").getId();

      Query query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.EQUALS, 40.1));
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.NOT_EQUALS, "60"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id4, id5, id6);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.LOWER_THAN, "40.1"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id6);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.LOWER_THAN_EQUALS, "40.1"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id6);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.BETWEEN, "20", 41));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.NOT_BETWEEN, "20", 41));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id3, id4, id5, id6);
   }

   @Test
   public void testSearchFulltextDocumentsByNumberConstraint() {
      Constraint constraint = new Constraint(ConstraintType.Number, new DataDocument("currency", "sk-SK"));
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("numberCollection2", attribute).getId();

      String id1 = createDocument(collectionId, "20.3").getId();
      String id2 = createDocument(collectionId, "40.1").getId();
      String id3 = createDocument(collectionId, "60").getId();

      Query query = createSimpleQueryWithFulltext("20");
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1);

      query = createSimpleQueryWithFulltext("€");
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3);

      query = createSimpleQueryWithFulltext("60 €");
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id3);
   }

   private Query createSimpleQueryWithAttributeFilter(String collectionId, CollectionAttributeFilter filter) {
      List<QueryStem> stems = Collections.singletonList(new QueryStem(null, collectionId, Collections.emptyList(), Collections.emptySet(), Collections.singletonList(filter), Collections.emptyList()));
      return new Query(stems, Collections.emptySet(), null, null);
   }

   private Query createSimpleQueryWithFulltext(String fulltext) {
      return new Query(Collections.emptyList(), Collections.singleton(fulltext), null, null);
   }

   @Test
   public void testSearchTasks() {
      Collection taskCollection = createTaskCollectionWithAttributes("taskCollection");
      String taskCollectionId = taskCollection.getId();
      Attribute stateAttribute = taskCollection.getAttributes().stream().filter(a -> a.getId().equals("a1")).findFirst().get();
      String stateAttributeId = stateAttribute.getId();
      Attribute assigneeAttribute = taskCollection.getAttributes().stream().filter(a -> a.getId().equals("a2")).findFirst().get();
      String assigneeAttributeId = assigneeAttribute.getId();

      String otherCollectionId = createCollection("otherCollection", stateAttribute, assigneeAttribute).getId();

      String id1 = createDocument(taskCollectionId, Map.of(stateAttributeId, "a", assigneeAttributeId, USER)).getId();
      createDocument(taskCollectionId, Collections.singletonMap(stateAttributeId, "b"));
      String id3 = createDocument(taskCollectionId, Collections.singletonMap(stateAttributeId, "c")).getId();
      String id4 = createDocument(taskCollectionId, Collections.singletonMap(stateAttributeId, "d")).getId();
      String id5 = createDocument(taskCollectionId, Map.of(stateAttributeId, Arrays.asList("a", "b"), assigneeAttributeId, USER)).getId();
      String id6 = createDocument(taskCollectionId, Collections.singletonMap(stateAttributeId, Arrays.asList("b", "c"))).getId();
      String id7 = createDocument(taskCollectionId, Collections.singletonMap(stateAttributeId, Arrays.asList("c", "d"))).getId();
      String id8 = createDocument(taskCollectionId, Collections.singletonMap(stateAttributeId, Arrays.asList("d", "a"))).getId();
      createDocument(otherCollectionId, Collections.singletonMap(stateAttributeId, "a"));
      createDocument(otherCollectionId, Collections.singletonMap(stateAttributeId, "b"));
      createDocument(otherCollectionId, Collections.singletonMap(stateAttributeId, "c"));
      createDocument(otherCollectionId, Collections.singletonMap(stateAttributeId, "d"));

      Query query = new Query();
      List<Document> documents = searchFacade.searchTasksDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id5);

      query = createSimpleQueryWithAttributeFilter(taskCollectionId, CollectionAttributeFilter.createFromValues(taskCollectionId, stateAttributeId, ConditionType.HAS_SOME, Arrays.asList("a", "c", "d")));
      documents = searchFacade.searchTasksDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3, id4, id5, id6, id7, id8);

      query = createSimpleQueryWithAttributeFilter(otherCollectionId, CollectionAttributeFilter.createFromValues(otherCollectionId, stateAttributeId, ConditionType.HAS_SOME, Arrays.asList("a", "c", "d")));
      documents = searchFacade.searchTasksDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).isEmpty();
   }

   private Collection createTaskCollectionWithAttributes(String name) {
      var options = Arrays.asList(new DataDocument("option", "a"), new DataDocument("option", "b"), new DataDocument("option", "c"), new DataDocument("option", "d"));
      Constraint stateConstraint = new Constraint(ConstraintType.Select, new DataDocument("multi", true).append("options", options));
      Attribute stateAttribute = new Attribute("a1", "a1", null, stateConstraint, null, null, null, 1, null);
      var stateAttributeId = stateAttribute.getId();
      Constraint assigneeConstraint = new Constraint(ConstraintType.User, new DataDocument());
      Attribute assigneeAttribute = new Attribute("a2", "a2", null, assigneeConstraint, null, null, null, 1, null);
      var assigneeAttributeId = assigneeAttribute.getId();
      var purposeMetadata = new DataDocument(CollectionPurpose.META_STATE_ATTRIBUTE_ID, stateAttributeId)
            .append(CollectionPurpose.META_ASSIGNEE_ATTRIBUTE_ID, assigneeAttributeId)
            .append(CollectionPurpose.META_FINAL_STATES_LIST, Arrays.asList("c", "d"));
      CollectionPurpose purpose = new CollectionPurpose(CollectionPurposeType.Tasks, purposeMetadata);
      Constraint numberConstraint = new Constraint(ConstraintType.Number, new DataDocument());
      Attribute otherAttribute = new Attribute("a3", "a3", null, numberConstraint, null, null, null, 3, null);
      return createTaskCollection(name, purpose, stateAttribute, assigneeAttribute, otherAttribute);
   }

   @Test
   public void testSearchContributors() {
      Constraint constraint = new Constraint(ConstraintType.Number, new DataDocument());
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      Collection collection = createCollection("numberCollection", attribute);
      setCollectionUserRoles(collection, Set.of(new Role(RoleType.Read)));

      String id1 = createDocument(collection.getId(), "10").getId();
      String id2 = createDocumentWithOtherUser(collection.getId(), "20").getId();
      String id3 = createDocument(collection.getId(), "30").getId();
      String id4 = createDocumentWithOtherUser(collection.getId(), 40).getId();
      String id5 = createDocument(collection.getId(), "50").getId();
      String id6 = createDocumentWithOtherUser(collection.getId(), "60").getId();
      String id7 = createDocumentWithOtherUser(collection.getId(), 70).getId();
      String id8 = createDocument(collection.getId(), 80).getId();

      Query query = new Query();
      List<Document> documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).isEmpty();

      // contributor
      setCollectionUserRoles(collection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));
      documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3, id5, id8);

      // read all data
      setCollectionUserRoles(collection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataRead)));
      documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5, id6, id7, id8);

      // contributor with filters
      setCollectionUserRoles(collection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));
      query = createSimpleQueryWithAttributeFilter(collection.getId(), CollectionAttributeFilter.createFromValues(collection.getId(), DOCUMENT_KEY, ConditionType.LOWER_THAN, "40"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3);

      query = createSimpleQueryWithAttributeFilter(collection.getId(), CollectionAttributeFilter.createFromValues(collection.getId(), DOCUMENT_KEY, ConditionType.GREATER_THAN, "40"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id5, id8);

      // reader with filters
      setCollectionUserRoles(collection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataRead)));
      query = createSimpleQueryWithAttributeFilter(collection.getId(), CollectionAttributeFilter.createFromValues(collection.getId(), DOCUMENT_KEY, ConditionType.LOWER_THAN, "40"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3);

      query = createSimpleQueryWithAttributeFilter(collection.getId(), CollectionAttributeFilter.createFromValues(collection.getId(), DOCUMENT_KEY, ConditionType.GREATER_THAN, "40"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id5, id6, id7, id8);

   }

   @Test
   public void testSearchTasksAndContributors() {
      Collection taskCollection = createTaskCollectionWithAttributes("taskCollection");
      Attribute stateAttribute = taskCollection.getAttributes().stream().filter(a -> a.getId().equals("a1")).findFirst().get();
      Attribute assigneeAttribute = taskCollection.getAttributes().stream().filter(a -> a.getId().equals("a2")).findFirst().get();

      String id1 = createDocumentWithOtherUser(taskCollection.getId(), Map.of(stateAttribute.getId(), "a", assigneeAttribute.getId(), USER)).getId();
      String id2 = createDocumentWithOtherUser(taskCollection.getId(), Collections.singletonMap(stateAttribute.getId(), "b")).getId();
      String id3 = createDocument(taskCollection.getId(), Collections.singletonMap(stateAttribute.getId(), "c")).getId();
      String id4 = createDocument(taskCollection.getId(), Collections.singletonMap(stateAttribute.getId(), "d")).getId();
      String id5 = createDocumentWithOtherUser(taskCollection.getId(), Map.of(stateAttribute.getId(), Arrays.asList("a", "b"), assigneeAttribute.getId(), USER)).getId();
      String id6 = createDocument(taskCollection.getId(), Collections.singletonMap(stateAttribute.getId(), Arrays.asList("b", "c"))).getId();
      String id7 = createDocumentWithOtherUser(taskCollection.getId(), Collections.singletonMap(stateAttribute.getId(), Arrays.asList("c", "d"))).getId();
      String id8 = createDocument(taskCollection.getId(), Collections.singletonMap(stateAttribute.getId(), Arrays.asList("d", "a"))).getId();

      setCollectionUserRoles(taskCollection, Set.of(new Role(RoleType.Read)));
      Query query = new Query();
      List<Document> documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id5);

      setCollectionUserRoles(taskCollection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));
      documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3, id4, id5, id6, id8);

      setCollectionUserRoles(taskCollection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataRead)));
      documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5, id6, id7, id8);

      setCollectionUserRoles(taskCollection, Set.of(new Role(RoleType.Read), new Role(RoleType.DataContribute)));
      query = createSimpleQueryWithAttributeFilter(taskCollection.getId(), CollectionAttributeFilter.createFromValues(taskCollection.getId(), stateAttribute.getId(), ConditionType.HAS_SOME, Arrays.asList("a", "b")));
      documents = searchFacade.searchDocumentsAndLinks(query, true).getFirst();
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id5, id6, id8);
   }

   @Test
   public void testSearchDocumentsDateConstraint() {
      Constraint constraint = new Constraint(ConstraintType.DateTime, new DataDocument());
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("dateCollection", attribute).getId();

      String id1 = createDocument(collectionId, "2019-04-01T00:00:00.000Z").getId();
      String id2 = createDocument(collectionId, "2019-04-02T00:00:00.000Z").getId();
      String id3 = createDocument(collectionId, "2019-04-03T00:00:00.000Z").getId();
      String id4 = createDocument(collectionId, "2019-04-04T00:00:00.000Z").getId();
      String id5 = createDocument(collectionId, "2019-04-04T00:00:00.000Z").getId();
      String id6 = createDocument(collectionId, "2019-04-05T00:00:00.000Z").getId();

      Query query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.EQUALS, "2019-04-04T00:00:00.000Z"));
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id4, id5);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.NOT_EQUALS, "2019-04-04T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id6);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.GREATER_THAN, "2019-04-04T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id6);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.LOWER_THAN, "2019-04-04T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.LOWER_THAN_EQUALS, "2019-04-04T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.LOWER_THAN_EQUALS, "2019-04-04T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.BETWEEN, "2019-04-02T00:00:00.000Z", "2019-04-08T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3, id4, id5, id6);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.NOT_BETWEEN, "2019-04-02T00:00:00.000Z", "2019-04-08T00:00:00.000Z"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1);
   }

   @Test
   public void testSearchDocumentsCoordinatesConstraint() {
      Constraint constraint = new Constraint(ConstraintType.Coordinates, new DataDocument());
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("coordinatesCollection", attribute).getId();

      String id1 = createDocument(collectionId, "40.123°N 74.123°W").getId();
      String id2 = createDocument(collectionId, "45.123°N 74.123°W").getId();
      String id3 = createDocument(collectionId, "60.123°N 74.123°W").getId();

      Query query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.EQUALS, "40.123, -74.123"));
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1);
   }

   @Test
   public void testSearchMultiSelectConstraint() {
      var options = Arrays.asList(new DataDocument("option", "a"), new DataDocument("option", "b"), new DataDocument("option", "c"), new DataDocument("option", "d"));
      Constraint constraint = new Constraint(ConstraintType.Select, new DataDocument("multi", true).append("options", options));
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("selectCollection", attribute).getId();

      String id1 = createDocument(collectionId, Arrays.asList("a", "b", "c")).getId();
      String id2 = createDocument(collectionId, Arrays.asList("a", "b")).getId();
      String id3 = createDocument(collectionId, Arrays.asList("a", "c")).getId();
      String id4 = createDocument(collectionId, Arrays.asList("c", "x")).getId();
      String id5 = createDocument(collectionId, Collections.singletonList("d")).getId();

      Query query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.IN, Arrays.asList("a", "b")));
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.HAS_SOME, Arrays.asList("a", "c")));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.HAS_ALL, Arrays.asList("a", "b")));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.HAS_NONE_OF, Arrays.asList("a", "b")));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id4, id5);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.NOT_EMPTY));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5);
   }

   @Test
   public void testDurationConstraint() {
      Constraint constraint = new Constraint(ConstraintType.Duration, new DataDocument("type", "Work"));
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("durationCollection", attribute).getId();

      String id1 = createDocument(collectionId, "5w3d").getId();
      String id2 = createDocument(collectionId, "3d4h").getId();
      String id3 = createDocument(collectionId, "2d12h").getId();
      String id4 = createDocument(collectionId, "3d2h5s").getId();
      String id5 = createDocument(collectionId, "ddhmms").getId();

      Query query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.EQUALS, "28h"));
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id2, id3);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.NOT_EQUALS, "dh19h"));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id4, id5);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.EQUALS, "5tddd"));
      searchFacade.setLanguage(Language.CS);
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1);
   }

   @Test
   public void testUserConstraint() {
      Constraint constraint = new Constraint(ConstraintType.User, new DataDocument("multi", true));
      Attribute attribute = new Attribute(DOCUMENT_KEY, DOCUMENT_KEY, null, constraint, null, null, null, 3, null);
      String collectionId = createCollection("userCollection", attribute).getId();

      String id1 = createDocument(collectionId, Collections.singletonList(USER)).getId();
      String id2 = createDocument(collectionId, Arrays.asList(USER1, USER2)).getId();
      String id3 = createDocument(collectionId, Arrays.asList(USER, USER1)).getId();
      String id4 = createDocument(collectionId, Collections.singletonList(USER1)).getId();

      Query query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromTypes(collectionId, DOCUMENT_KEY, ConditionType.HAS_SOME, ConditionValueType.CURRENT_USER.getValue()));
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3);

      query = createSimpleQueryWithAttributeFilter(collectionId, CollectionAttributeFilter.createFromValues(collectionId, DOCUMENT_KEY, ConditionType.HAS_NONE_OF, USER2));
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id3, id4);
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
      String linkType01Id = linkTypeDao.createLinkType(new LinkType("lmr",
            Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList(), null, null, null)).getId();
      String linkType12Id = linkTypeDao.createLinkType(new LinkType("lmrr",
            Arrays.asList(collectionIds.get(1), collectionIds.get(2)), Collections.emptyList(), null, null, null)).getId();
      createLinkInstance(linkType01Id, Arrays.asList(id1, id6));
      createLinkInstance(linkType01Id, Arrays.asList(id1, id7));
      createLinkInstance(linkType01Id, Arrays.asList(id2, id8));
      createLinkInstance(linkType01Id, Arrays.asList(id3, id8));
      createLinkInstance(linkType01Id, Arrays.asList(id4, id6));
      createLinkInstance(linkType12Id, Arrays.asList(id6, id10));
      createLinkInstance(linkType12Id, Arrays.asList(id6, id11));
      createLinkInstance(linkType12Id, Arrays.asList(id7, id12));

      QueryStem stem = new QueryStem(null, collectionIds.get(0), Arrays.asList(linkType01Id, linkType12Id), Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
      Query query = new Query(Collections.singletonList(stem), Collections.emptySet(), null, null);
      List<Document> documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id3, id4, id5, id6, id7, id8, id9, id10, id11, id12);

      stem = new QueryStem(null, collectionIds.get(0), Collections.singletonList(linkType01Id), Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
      query = new Query(Collections.singletonList(stem), Collections.singleton("lol"), null, null);
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id1, id2, id6, id7, id8);

      stem = new QueryStem(null, collectionIds.get(1), Collections.singletonList(linkType12Id), Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
      query = new Query(Collections.singletonList(stem), Collections.singleton("lumr"), null, null);
      documents = searchFacade.searchDocuments(query, true);
      assertThat(documents).extracting(Document::getId).containsOnly(id11, id6);
   }

   private LinkInstance createLinkInstance(String linkTypeId, List<String> documentIds) {
      final LinkInstance linkInstance = linkInstanceDao.createLinkInstance(new LinkInstance(linkTypeId, documentIds));
      linkDataDao.createData(linkTypeId, linkInstance.getId(), new DataDocument());
      return linkInstance;
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

      Query query = new Query(new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.singleton(a0.getId()), Collections.emptyList(), Collections.emptyList()));
      List<Document> documents = searchFacade.searchDocuments(query, true);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(
                      documents.stream().filter(d -> d.getMetaData() == null || d.getMetaData().get(Document.META_PARENT_ID) == null).count())
                .isEqualTo(1);
      assertions.assertThat(documents).hasSize(11);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("a0", "b1", "b2", "c1", "c2", "c3", "d1", "d2", "d3", "e1", "e2");
      assertions.assertAll();

      query = new Query(new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.singleton(b2.getId()), Collections.emptyList(), Collections.emptyList()));
      documents = searchFacade.searchDocuments(query, true);
      assertions = new SoftAssertions();
      assertions.assertThat(documents).hasSize(4);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("b2", "d1", "d2", "d3");
      assertions.assertAll();

      query = new Query(new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.singleton(b1.getId()), Collections.emptyList(), Collections.emptyList()));
      documents = searchFacade.searchDocuments(query, true);
      assertions = new SoftAssertions();
      assertions.assertThat(documents).hasSize(6);
      assertions.assertThat(documents.stream().map(d -> d.getData().getString(DOCUMENT_KEY)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("b1", "c1", "c2", "c3", "e1", "e2");
      assertions.assertAll();
   }

   @Test
   public void testCyclicChildDocuments() {
      final Document a0 = createDocument(collectionIds.get(0), "a0");
      final Document a1 = createDocument(collectionIds.get(0), "a1", a0.getId());
      final Document a2 = createDocument(collectionIds.get(0), "a2", a1.getId());
      a0.setMetaData(new DataDocument(Document.META_PARENT_ID, a2.getId()));
      documentDao.updateDocument(a0.getId(), a0);

      Query query = new Query(new QueryStem(null, collectionIds.get(0), Collections.emptyList(), Collections.singleton(a0.getId()), Collections.emptyList(), Collections.emptyList()));
      List<Document> documents = searchFacade.searchDocuments(query, true);

      assertThat(documents).hasSize(3);
      var parents = documents.stream().map(d -> d.getMetaData().getString(Document.META_PARENT_ID)).collect(Collectors.toList());
      var ids = documents.stream().map(Document::getId).collect(Collectors.toList());

      assertThat(ids).hasSize(3);
      assertThat(parents).hasSize(3);
      assertThat(parents).containsAll(ids);
   }

   private Document createDocument(String collectionId, Object value) {
      return createDocument(collectionId, Collections.singletonMap(DOCUMENT_KEY, value));
   }

   private Document createDocumentWithOtherUser(String collectionId, Object value) {
      return createDocumentWithOtherUser(collectionId, Collections.singletonMap(DOCUMENT_KEY, value));
   }

   private Document createDocumentWithOtherUser(String collectionId, Map<String, Object> values) {
      Document document = createDocument(collectionId, values);

      Document updatingDocument = new Document(document);
      updatingDocument.setCreatedBy(collectionId);

      Document updatedDocument = documentDao.updateDocument(document.getId(), updatingDocument);
      updatedDocument.setData(document.getData());
      return updatedDocument;
   }

   private Document createDocument(String collectionId, Map<String, Object> values) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      values.forEach((attributeId, value) -> {
         if (collection.getAttributes().stream().noneMatch(attr -> attr.getName().equals(attributeId))) {
            collection.createAttribute(new Attribute(attributeId, attributeId, null, null, null, null, null, 1, null));
            collection.setLastAttributeNum(collection.getLastAttributeNum() + 1);
         } else {
            Attribute attr = collection.getAttributes().stream().filter(a -> a.getName().equals(attributeId)).findFirst().get();
            attr.setUsageCount(attr.getUsageCount() + 1);
         }
      });
      collectionDao.updateCollection(collectionId, collection, null);

      Document document = new Document(new DataDocument(values));
      document.setCollectionId(collectionId);
      document.setCreatedBy(userId);
      document.setCreationDate(ZonedDateTime.now());
      Document storedDocument = documentDao.createDocument(document);

      document.setData(constraintManager.encodeDataTypes(collection, document.getData()));

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
      LinkType linkType = new LinkType("nm", Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList(), null, null, null);
      String linkTypeId1 = linkTypeDao.createLinkType(linkType).getId();
      LinkType linkType2 = new LinkType("nm2", Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList(), null, null, null);
      String linkTypeId2 = linkTypeDao.createLinkType(linkType2).getId();

      String id10 = createDocument(collectionIds.get(0), "lumeer").getId();
      String id11 = createDocument(collectionIds.get(0), "lumeer").getId();
      String id12 = createDocument(collectionIds.get(0), "lumeer").getId();
      String id20 = createDocument(collectionIds.get(1), "lumeer").getId();
      String id21 = createDocument(collectionIds.get(1), "lumeer").getId();
      String id22 = createDocument(collectionIds.get(1), "lumeer").getId();

      String id1 = createLinkInstance(linkTypeId1, Arrays.asList(id10, id20)).getId();
      String id2 = createLinkInstance(linkTypeId1, Arrays.asList(id10, id22)).getId();
      String id3 = createLinkInstance(linkTypeId1, Arrays.asList(id11, id21)).getId();
      String id4 = createLinkInstance(linkTypeId2, Arrays.asList(id10, id20)).getId();

      QueryStem stem1 = new QueryStem(null, collectionIds.get(0), Collections.singletonList(linkTypeId1), Collections.singleton(id10), null, null);
      Query query1 = new Query(stem1);
      List<LinkInstance> linkInstances = searchFacade.searchLinkInstances(query1, true);
      assertThat(linkInstances).extracting(LinkInstance::getId).containsOnly(id1, id2);

      QueryStem stem2 = new QueryStem(null, collectionIds.get(1), Collections.singletonList(linkTypeId1), Collections.singleton(id21), null, null);
      Query query = new Query(stem2);
      linkInstances = searchFacade.searchLinkInstances(query, true);
      assertThat(linkInstances).extracting(LinkInstance::getId).containsOnly(id3);

      QueryStem stem3 = new QueryStem(null, collectionIds.get(0), Collections.singletonList(linkTypeId2), null, null, null);
      Query query3 = new Query(stem3);
      linkInstances = searchFacade.searchLinkInstances(query3, true);
      assertThat(linkInstances).extracting(LinkInstance::getId).containsOnly(id4);

      QueryStem stem4 = new QueryStem(null, collectionIds.get(0), Collections.singletonList(linkTypeId1), null, null, null);
      Query query4 = new Query(stem4);
      linkInstances = searchFacade.searchLinkInstances(query4, true);
      assertThat(linkInstances).extracting(LinkInstance::getId).containsOnly(id1, id2, id3);

      QueryStem stem5 = new QueryStem(null, collectionIds.get(0), Collections.singletonList(linkTypeId2), Collections.singleton(id20), null, null);
      Query query5 = new Query(stem5);
      linkInstances = searchFacade.searchLinkInstances(query5, true);
      assertThat(linkInstances).extracting(LinkInstance::getId).containsOnly(id4);
   }
}
