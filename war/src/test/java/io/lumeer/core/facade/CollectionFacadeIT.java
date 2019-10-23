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

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.*;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.ListCollectionsIn10SecondsTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class CollectionFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE = "TCOLL";
   private static final String NAME = "Test collection";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ee00";
   private static final String COLOR2 = "#987EDC";

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;
   private Project project;
   private Organization organization;

   private static final String ATTRIBUTE_ID = "a1";
   private static final String ATTRIBUTE_NAME = "fullname";
   private static final Constraint ATTRIBUTE_CONSTRAINT = new Constraint(ConstraintType.Boolean, null);
   private static final Function ATTRIBUTE_FUNCTION = new Function("", "xml", "error", 123456L, false);
   private static final Integer ATTRIBUTE_COUNT = 0;

   private static final String ATTRIBUTE_NAME2 = "fullname2";
   static final String ATTRIBUTE_STATE = "State";

   private static final String CODE2 = "TCOLL2";
   private static final String CODE3 = "TCOLL3";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private UserNotificationFacade userNotificationFacade;

   @Inject
   private UserNotificationDao userNotificationDao;

   @Inject
   private TaskExecutor taskExecutor;

   @Inject
   private ContextualTaskFactory contextualTaskFactory;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      Group group = new Group(GROUP);
      this.group = groupDao.createGroup(group);

      userPermission = Permission.buildWithRoles(this.user.getId(), Collection.ROLES);
      groupPermission = Permission.buildWithRoles(this.group.getId(), Collections.singleton(Role.READ));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      this.project = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(this.organization.getId(), this.project.getId());

      collectionDao.setProject(project);
   }

   private Collection prepareCollection(String code) {
      return prepareCollection(code, NAME);
   }

   private Collection prepareCollection(String code, String name) {
      return new Collection(code, name, ICON, COLOR, null);
   }

   private Collection createCollection(String code) {
      return createCollection(code, NAME);
   }

   private Collection createCollection(String code, String name) {
      Collection collection = prepareCollection(code, name);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      return collectionDao.createCollection(collection);
   }

   private Collection createCollection(String code, Attribute attribute) {
      return createCollection(code, attribute, Collections.emptyMap());
   }

   private Collection createCollection(String code, Attribute attribute, Map<String, Rule> rules) {
      Collection collection = prepareCollection(code);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      collection.updateAttribute(attribute.getId(), attribute);
      collection.setRules(rules);
      return collectionDao.createCollection(collection);
   }

   @Test
   public void testCreateCollection() {
      Collection collection = prepareCollection(CODE);

      Collection returnedCollection = collectionFacade.createCollection(collection);
      assertThat(returnedCollection).isNotNull();
      assertThat(returnedCollection.getId()).isNotNull();

      Collection storedCollection = collectionDao.getCollectionByCode(CODE);
      assertThat(storedCollection).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedCollection.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateCollection() {
      String collectionId = createCollection(CODE).getId();

      Collection updatedCollection = prepareCollection(CODE2);
      updatedCollection.getPermissions().removeUserPermission(USER);

      collectionFacade.updateCollection(collectionId, updatedCollection);

      Collection storedCollection = collectionDao.getCollectionByCode(CODE2);
      assertThat(storedCollection).isNotNull();
      assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testDeleteCollection() {
      String collectionId = createCollection(CODE).getId();

      collectionFacade.deleteCollection(collectionId);

      assertThatThrownBy(() -> collectionDao.getCollectionByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetCollection() {
      String collectionId = createCollection(CODE).getId();

      Collection storedCollection = collectionFacade.getCollection(collectionId);
      assertThat(storedCollection).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedCollection.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertions.assertThat(storedCollection.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedCollection.getIcon()).isEqualTo(ICON);
      assertions.assertAll();

      assertPermissions(storedCollection.getPermissions().getUserPermissions(), userPermission);
      assertPermissions(storedCollection.getPermissions().getGroupPermissions(), groupPermission);
   }

   @Test
   public void testGetCollections() {
      createCollection(CODE);
      createCollection(CODE2);

      assertThat(collectionFacade.getCollections())
            .extracting(Resource::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testUpdateCollectionAttributeAdd() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).isEmpty();

      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINT, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT);
      final Attribute createdAttribute = collectionFacade.updateCollectionAttribute(collection.getId(), ATTRIBUTE_ID, attribute);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getId()).isEqualTo(createdAttribute.getId());
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(storedAttribute.getConstraint()).isEqualTo(ATTRIBUTE_CONSTRAINT);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testCreateCollectionAttribute() {
      final Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).isEmpty();

      final var attributeIds = new HashSet<String>();

      Runnable r = () -> {
         for (int i = 0; i < 20; i++) {
            Attribute attribute = new Attribute(null, ATTRIBUTE_NAME, null, null, 0);
            final java.util.Collection<Attribute> createdAttributes = collectionFacade.createCollectionAttributes(collection.getId(), List.of(attribute));
            createdAttributes.forEach(a -> attributeIds.add(a.getId()));
         }
      };

      final List<Thread> threads = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
         threads.add(new Thread(r));
      }

      threads.forEach(Thread::run);

      threads.forEach(t -> {
         try {
            t.join();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      });

      var updatedCollection = collectionDao.getCollectionByCode(CODE);
      assertThat(updatedCollection).isNotNull();
      assertThat(updatedCollection.getAttributes()).hasSize(4 * 20);
   }

   @Test
   public void testUpdateCollectionAttributeUpdate() {
      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINT, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      Attribute updatedAttribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME2, ATTRIBUTE_CONSTRAINT, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT);
      collectionFacade.updateCollectionAttribute(collection.getId(), ATTRIBUTE_ID, updatedAttribute);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getId()).isEqualTo(ATTRIBUTE_ID);
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME2);
      assertions.assertThat(storedAttribute.getConstraint()).isEqualTo(ATTRIBUTE_CONSTRAINT);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testDeleteCollectionAttribute() {
      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINT, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      collectionFacade.deleteCollectionAttribute(collection.getId(), ATTRIBUTE_ID);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).isEmpty();
   }

   @Test
   public void testDeleteAutoLinkRules() {
      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, ATTRIBUTE_CONSTRAINT, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT);
      Collection collection = createCollection(CODE, attribute);

      var rules = createRules(collection, attribute);
      collection.setRules(createRules(collection, attribute));
      collection = collectionDao.updateCollection(collection.getId(), collection, null);
      assertThat(collection.getRules().get("A")).isNotNull();

      Collection collection2 = createCollection(CODE2, attribute, rules);
      assertThat(collection2.getRules().get("A")).isNotNull();

      collectionFacade.deleteCollectionAttribute(collection.getId(), attribute.getId());
      collection = collectionFacade.getCollection(collection.getId());
      assertThat(collection.getRules().get("A")).isNull();
      assertThat(collection.getRules().keySet().size()).isEqualTo(2);

      collection2 = collectionFacade.getCollection(collection2.getId());
      assertThat(collection2.getRules().get("A")).isNull();
      assertThat(collection2.getRules().keySet().size()).isEqualTo(2);
   }

   private Map<String, Rule> createRules(Collection collection, Attribute attribute) {
      var map = new HashMap<String, Rule>();
      map.put("A", createRule(collection.getId(), attribute.getId()));
      map.put("B", createRule("something else", attribute.getId()));
      map.put("C", createRule("sosoelse", attribute.getId()));

      return map;
   }

   private Rule createRule(String collectionId, String attributeId) {
      final AutoLinkRule rule = new AutoLinkRule(new Rule(Rule.RuleType.AUTO_LINK, Rule.RuleTiming.ALL, new DataDocument()));
      rule.setCollection1(collectionId);
      rule.setAttribute1(attributeId);
      rule.setCollection2("some collection to test");
      rule.setAttribute2("a1");
      rule.setLinkType("some link type");

      return rule.getRule();
   }

   @Test
   public void testGetCollectionPermissions() {
      String collectionId = createCollection(CODE).getId();

      Permissions permissions = collectionFacade.getCollectionPermissions(collectionId);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      String USER2 = "aaa" + user.getId().substring(3);

      var notifications = userNotificationFacade.getNotifications();
      assertThat(notifications).isEmpty();

      final Collection collection = createCollection(CODE);
      final String collectionId = collection.getId();

      Permission userPermission = Permission.buildWithRoles(user.getId(), Set.of(Role.MANAGE, Role.READ));
      collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission));

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);

      userPermission = Permission.buildWithRoles(USER2, Set.of(Role.MANAGE, Role.READ));
      collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission));

      notifications = userNotificationDao.getRecentNotifications(USER2);
      assertThat(notifications).hasSize(1).allMatch(n ->
            n.getData().getString(UserNotification.CollectionShared.COLLECTION_COLOR).equals(COLOR)
                  && n.getUserId().equals(USER2)
                  && n.getData().getString(UserNotification.CollectionShared.COLLECTION_ID).equals(collectionId));

      collection.setColor(COLOR2);
      collectionFacade.updateCollection(collectionId, collection);

      notifications = userNotificationDao.getRecentNotifications(USER2);
      assertThat(notifications).hasSize(1).allMatch(n ->
            n.getData().getString(UserNotification.CollectionShared.COLLECTION_COLOR).equals(COLOR2)
                  && n.getUserId().equals(USER2)
                  && n.getData().getString(UserNotification.CollectionShared.COLLECTION_ID).equals(collectionId));

      userPermission = Permission.buildWithRoles(USER2, Collections.emptySet());
      collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission));

      notifications = userNotificationDao.getRecentNotifications(USER2);
      assertThat(notifications).isEmpty();
   }

   @Test
   public void testRemoveUserPermission() {
      String collectionId = createCollection(CODE).getId();

      collectionFacade.removeUserPermission(collectionId, user.getId());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      String collectionId = createCollection(CODE).getId();

      Permission groupPermission = Permission.buildWithRoles(group.getId(), Set.of(Role.SHARE, Role.READ));
      collectionFacade.updateGroupPermissions(collectionId, Set.of(groupPermission));

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      String collectionId = createCollection(CODE).getId();

      collectionFacade.removeGroupPermission(collectionId, group.getId());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testTooManyCollections() {
      for (int i = 1; i <= 10; i++) {
         Collection collection = prepareCollection(CODE + i);
         collectionFacade.createCollection(collection);
      }

      Collection collection = prepareCollection(CODE + "11");
      assertThatExceptionOfType(ServiceLimitsExceededException.class).isThrownBy(() -> {
         collectionFacade.createCollection(collection);
      }).as("On Trial plan, it should be possible to create only 10 collections but it was possible to create another one.");
   }

   public void testAddFavoriteCollection() {
      List<String> ids = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         Collection collection = prepareCollection(CODE + i);
         ids.add(collectionFacade.createCollection(collection).getId());
      }

      assertThat(collectionFacade.getFavoriteCollectionsIds()).isEmpty();

      collectionFacade.addFavoriteCollection(ids.get(0));
      collectionFacade.addFavoriteCollection(ids.get(3));
      collectionFacade.addFavoriteCollection(ids.get(5));

      assertThat(collectionFacade.getFavoriteCollectionsIds()).containsOnly(ids.get(0), ids.get(3), ids.get(5));

      for (int i = 0; i < 10; i++) {
         assertThat(collectionFacade.isFavorite(ids.get(i))).isEqualTo(i == 0 || i == 3 || i == 5);
      }
   }

   @Test
   public void testRemoveFavoriteCollection() {
      List<String> ids = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         Collection collection = prepareCollection(CODE + i);
         ids.add(collectionFacade.createCollection(collection).getId());
      }

      collectionFacade.addFavoriteCollection(ids.get(1));
      collectionFacade.addFavoriteCollection(ids.get(2));
      collectionFacade.addFavoriteCollection(ids.get(9));

      assertThat(collectionFacade.getFavoriteCollectionsIds()).containsOnly(ids.get(1), ids.get(2), ids.get(9));

      collectionFacade.removeFavoriteCollection(ids.get(1));
      collectionFacade.removeFavoriteCollection(ids.get(9));

      assertThat(collectionFacade.getFavoriteCollectionsIds()).containsOnly(ids.get(2));
   }

   @Test
   public void testSetDefaultAttribute() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE));
      assertThat(collection.getDefaultAttributeId()).isNull();

      collectionFacade.setDefaultAttribute(collection.getId(), "some not existing id");
      collection = collectionFacade.getCollection(collection.getId());
      assertThat(collection.getDefaultAttributeId()).isNull();

      Attribute attribute = new Attribute("a1");
      Attribute created = new ArrayList<>(collectionFacade.createCollectionAttributes(collection.getId(), Collections.singletonList(attribute))).get(0);

      collectionFacade.setDefaultAttribute(collection.getId(), created.getId());
      collection = collectionFacade.getCollection(collection.getId());
      assertThat(collection.getDefaultAttributeId()).isEqualTo(created.getId());
   }

   @Test
   public void testDocumentCount() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE));

      assertThat(collectionFacade.getDocumentsCountInAllCollections()).isEqualTo(0);

      documentFacade.createDocument(collection.getId(), new Document(new DataDocument("pepa", "zdepa")));
      documentFacade.createDocument(collection.getId(), new Document(new DataDocument("tonda", "fonda")));
      documentFacade.createDocument(collection.getId(), new Document(new DataDocument("franta", "pajta")));

      assertThat(collectionFacade.getDocumentsCountInAllCollections()).isEqualTo(3);
   }

   @Test
   public void testGetAllCollectionsProjectManager() {
      collectionDao.createCollection(prepareCollection("CD1"));
      collectionDao.createCollection(prepareCollection("CD2"));

      assertThat(collectionFacade.getCollections()).hasSize(2);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      project.setPermissions(projectPermissions);
      projectDao.updateProject(project.getId(), project);
      workspaceCache.clear();

      assertThat(collectionFacade.getCollections()).hasSize(2);

      Permissions organizationPermissions = new Permissions();
      organizationPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      organization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.clear();

      assertThat(collectionFacade.getCollections()).isEmpty();
   }

   @Test
   public void testAttributeConversion() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE3));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, 0),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, 0)
            )
      );

      var values = Arrays.asList("New", "In Progress", "To Do", "Done", "Won't fix");
      var rnd = new Random();

      for(int i = 0; i < 1_000; i++) {
         documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-" + (i + 1)).append("a2", values.get(rnd.nextInt(values.size())))));
      }

      var attributes = collectionFacade.getCollection(collection.getId()).getAttributes();
      var attr = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr.setConstraint(new Constraint(ConstraintType.Select, new org.bson.Document("options",
            List.of(
                  new org.bson.Document("value", "New").append("displayValue", ""),
                  new org.bson.Document("value", "In Progress").append("displayValue", ""),
                  new org.bson.Document("value", "To Do").append("displayValue", ""),
                  new org.bson.Document("value", "Done").append("displayValue", ""),
                  new org.bson.Document("value", "Won't fix").append("displayValue", "")
            )
      )));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr.getId(), attr);

      var documents = documentDao.getDocumentsByCollection(collection.getId());

      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());

         assertThat(doc.getData().getString(attr.getId())).isIn(values);
      });

      // now add display value
      var attr2 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr2.setConstraint(new Constraint(ConstraintType.Select, new org.bson.Document("options",
            List.of(
                  new org.bson.Document("value", 0).append("displayValue", "New"),
                  new org.bson.Document("value", 1).append("displayValue", "In Progress"),
                  new org.bson.Document("value", 2).append("displayValue", "To Do"),
                  new org.bson.Document("value", 3).append("displayValue", "Done"),
                  new org.bson.Document("value", 4).append("displayValue", "Won't fix")
            )
      ).append("displayValues", List.of("New", "In Progress", "To Do", "Done", "Won't fix"))));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr2.getId(), attr2);

      documents = documentDao.getDocumentsByCollection(collection.getId());

      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());

         assertThat(doc.getData().getInteger(attr.getId())).isBetween(0, 4);
      });

      // now add display value
      var attr3 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr3.setConstraint(null);
      collectionFacade.updateCollectionAttribute(collection.getId(), attr3.getId(), attr3);

      documents = documentDao.getDocumentsByCollection(collection.getId());

      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());

         assertThat(doc.getData().getString(attr.getId())).isIn(values);
      });
   }

   @Test
   public void testDurationAttributeConversion() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE3));
      collectionFacade.createCollectionAttributes(
              collection.getId(),
              Arrays.asList(
                      new Attribute("a1", "Task", null, null, 0),
                      new Attribute("a2", ATTRIBUTE_STATE, null, null, 0)
              )
      );

      // 100800000, 77700000, 86466000, 1345, 1434534000, 10806000, 14580000
      // 3d4h, 2d5h35m, 3d1m6s, 1s, 9w4d6h28m54s, 3h6s, 4h3m
      var values = Arrays.asList("3d4h", "14h455m", "3d66s", "1345", "1434534s", "3h6s", "0d3m4h0s");
      var rnd = new Random();

      var i = new AtomicInteger(1);
      values.forEach(value -> {
         documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-" + i.getAndIncrement()).append("a2", value)));
      });

      var attributes = collectionFacade.getCollection(collection.getId()).getAttributes();
      var attr = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr.setConstraint(new Constraint(ConstraintType.Duration, new org.bson.Document("type", "Work").append("conversions",
              new org.bson.Document("w", 5).append("d", 8).append("h", 60).append("m", 60).append("s", 1000)
      )));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr.getId(), attr);

      var documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Long> res = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res.put(doc.getData().getString("a1"), doc.getData().getLong("a2"));
      });

      assertThat(res).contains(
              Map.entry("Task-1", 100800000L),
              Map.entry("Task-2", 77700000L),
              Map.entry("Task-3", 86466000L),
              Map.entry("Task-4", 1345L),
              Map.entry("Task-5", 1434534000L),
              Map.entry("Task-6", 10806000L),
              Map.entry("Task-7", 14580000L)
      );

      // now back to no constraint
      var attr2 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr2.setConstraint(new Constraint(ConstraintType.None, null));
      collectionFacade.updateCollectionAttribute(collection.getId(), attr2.getId(), attr2);

      documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, String> res2 = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res2.put(doc.getData().getString("a1"), doc.getData().getString("a2"));
      });

      assertThat(res2).contains(
              Map.entry("Task-1", "3d4h"),
              Map.entry("Task-2", "2d5h35m"),
              Map.entry("Task-3", "3d1m6s"),
              Map.entry("Task-4", "1s"),
              Map.entry("Task-5", "9w4d6h28m54s"),
              Map.entry("Task-6", "3h6s"),
              Map.entry("Task-7", "4h3m")
      );

      // custom unit lengths
      var attr3 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr3.setConstraint(new Constraint(ConstraintType.Duration, new org.bson.Document("type", "Custom").append("conversions",
              new org.bson.Document("w", 5).append("d", 5).append("h", 30).append("m", 60).append("s", 1000)
      )));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr3.getId(), attr3);

      documents = documentDao.getDocumentsByCollection(collection.getId());
      Map<String, Long> res3 = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res3.put(doc.getData().getString("a1"), doc.getData().getLong("a2"));
      });

      assertThat(res3).contains(
              Map.entry("Task-1", 34200000L),
              Map.entry("Task-2", 29100000L),
              Map.entry("Task-3", 27066000L),
              Map.entry("Task-4", 1000L),
              Map.entry("Task-5", 453534000L),
              Map.entry("Task-6", 5406000L),
              Map.entry("Task-7", 7380000L)
      );
   }

   @Test
   public void testDateAttributeConversion() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE3));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, 0),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, 0)
            )
      );

      var values = Arrays.asList("23/11/2019 8:23:10", "28/02/2019 23:34:12", "24/03/1943 6:55:19", "12/04/1529 9:37:01");

      var i = new AtomicInteger(1);
      values.forEach(value -> {
         documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-" + i.getAndIncrement()).append("a2", value)));
      });

      var attributes = collectionFacade.getCollection(collection.getId()).getAttributes();
      var attr = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr.setConstraint(new Constraint(ConstraintType.DateTime, new org.bson.Document("format", "DD/MM/YYYY H:mm:ss")));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr.getId(), attr);

      var documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Long> res = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res.put(doc.getData().getString("a1"), doc.getData().getLong("a2"));
      });

      assertThat(res).contains(
            Map.entry("Task-1", 1574493790000L),
            Map.entry("Task-2", 1551393252000L),
            Map.entry("Task-3", -844970681000L),
            Map.entry("Task-4", -13907863379000L)
      );

      // now back to no constraint
      var attr2 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr2.setConstraint(new Constraint(ConstraintType.None, null));
      collectionFacade.updateCollectionAttribute(collection.getId(), attr2.getId(), attr2);

      documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, String> res2 = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res2.put(doc.getData().getString("a1"), doc.getData().getString("a2"));
      });

      assertThat(res2).contains(
            Map.entry("Task-1", "23/11/2019 8:23:10"),
            Map.entry("Task-2", "28/02/2019 23:34:12"),
            Map.entry("Task-3", "24/03/1943 6:55:19"),
            Map.entry("Task-4", "12/04/1529 9:37:01")
      );
   }

   @Test
   public void testPercentageAttributeConversion() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE3));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, 0),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, 0)
            )
      );

      var values = Arrays.asList("10", "12%", "0.12", "0.12 %");

      var i = new AtomicInteger(1);
      values.forEach(value -> {
         documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-" + i.getAndIncrement()).append("a2", value)));
      });

      var attributes = collectionFacade.getCollection(collection.getId()).getAttributes();
      var attr = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr.setConstraint(new Constraint(ConstraintType.Percentage, null));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr.getId(), attr);

      var documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Object> res = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res.put(doc.getData().getString("a1"), doc.getData().get("a2"));
      });

      assertThat(res).contains(
            Map.entry("Task-1", 10L),
            Map.entry("Task-2", 0.12d),
            Map.entry("Task-3", 0.12d),
            Map.entry("Task-4", 0.0012d)
      );

      // now back to no constraint
      var attr2 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr2.setConstraint(new Constraint(ConstraintType.None, null));
      collectionFacade.updateCollectionAttribute(collection.getId(), attr2.getId(), attr2);

      documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Object> res2 = new HashMap<>();
      documents.forEach(document -> {
         var doc = documentFacade.getDocument(collection.getId(), document.getId());
         res2.put(doc.getData().getString("a1"), doc.getData().get("a2"));
      });

      assertThat(res2).contains(
            Map.entry("Task-1", 10L),
            Map.entry("Task-2", 0.12d),
            Map.entry("Task-3", 0.12d),
            Map.entry("Task-4", 0.0012d)
      );
   }

   public void testTaskExecutor() throws InterruptedException {
      taskExecutor.submitTask(contextualTaskFactory.getInstance(ListCollectionsIn10SecondsTask.class));
      Thread.sleep(15_000);
   }

}
