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
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.ListCollectionsIn10SecondsTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.ZapierRuleTaskExecutor;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
   private static final String CODE4 = "TCOLL4";
   private static final String CODE5 = "TCOLL5";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

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
   private DataDao dataDao;

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
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private TaskExecutor taskExecutor;

   @Inject
   private ContextualTaskFactory contextualTaskFactory;

   @Inject
   private ZapierFacade zapierFacade;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Before
   public void configureProject() {
      user = userDao.createUser(new User(USER));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      favoriteItemDao.setOrganization(this.organization);
      group = groupDao.createGroup(new Group(GROUP, Collections.singletonList(user.getId())));
      user.setOrganizations(Collections.singleton(this.organization.getId()));
      user = userDao.updateUser(user.getId(), user);

      userPermission = Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)));
      groupPermission = Permission.buildWithRoles(this.group.getId(), Collections.singleton(new Role(RoleType.Read)));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.CollectionContribute), new Role(RoleType.UserConfig))));
      project.setPermissions(projectPermissions);
      this.project = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(this.organization.getId(), this.project.getId());

      collectionDao.setProject(project);

      PermissionCheckerUtil.allowGroups();
      permissionsChecker.getPermissionAdapter().invalidateUserCache();
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

      setProjectUserRoles(Set.of(new Role(RoleType.Read)));

      assertThatThrownBy(() -> collectionFacade.createCollection(collection))
            .isInstanceOf(NoResourcePermissionException.class);

      setProjectUserRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.CollectionContribute)));

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

   private void setProjectUserRoles(final Set<Role> roles) {
      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      project.setPermissions(projectPermissions);
      projectDao.updateProject(project.getId(), project);
      workspaceCache.clear();
   }

   @Test
   public void testUpdateCollection() {
      String collectionId = createCollection(CODE).getId();

      Collection updatedCollection = prepareCollection(CODE2);
      updatedCollection.getPermissions().removeUserPermission(USER);

      setCollectionGroupRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.Manage)));

      collectionFacade.updateCollection(collectionId, updatedCollection);

      Collection storedCollection = collectionDao.getCollectionByCode(CODE2);
      assertThat(storedCollection).isNotNull();
      assertThat(storedCollection.getName()).isEqualTo(NAME);
      assertThat(storedCollection.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testDeleteCollection() {
      String collectionId = createCollection(CODE).getId();

      setCollectionGroupRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.Manage)));

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
   public void testUpdateCollectionAttribute() {
      Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).isEmpty();

      setCollectionGroupRoles(collection, Set.of(new Role(RoleType.AttributeEdit)));

      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, "", ATTRIBUTE_CONSTRAINT, null, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);
      collectionFacade.createCollectionAttributes(collection.getId(), Set.of(attribute));

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).hasSize(1);

      attribute.setName("Updated name");
      Attribute updatedAttribute = collectionFacade.updateCollectionAttribute(collection.getId(), ATTRIBUTE_ID, attribute);

      Attribute storedAttribute = collection.getAttributes().iterator().next();
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedAttribute.getId()).isEqualTo(updatedAttribute.getId());
      assertions.assertThat(storedAttribute.getName()).isEqualTo(ATTRIBUTE_NAME);
      assertions.assertThat(storedAttribute.getConstraint()).isEqualTo(ATTRIBUTE_CONSTRAINT);
      assertions.assertThat(storedAttribute.getUsageCount()).isEqualTo(ATTRIBUTE_COUNT);
      assertions.assertAll();
   }

   @Test
   public void testCreateCollectionAttribute() {
      final Collection collection = createCollection(CODE);
      assertThat(collection.getAttributes()).isEmpty();

      setCollectionGroupRoles(collection, Set.of(new Role(RoleType.AttributeEdit)));

      final var attributeIds = new HashSet<String>();

      Runnable r = () -> {
         for (int i = 0; i < 20; i++) {
            Attribute attribute = new Attribute(null, ATTRIBUTE_NAME, null, null, null, null, null, 0, null);
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
      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, null, ATTRIBUTE_CONSTRAINT, null, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      setCollectionGroupRoles(collection, Set.of(new Role(RoleType.AttributeEdit)));

      Attribute updatedAttribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME2, null, ATTRIBUTE_CONSTRAINT, null, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);
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
      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, null, ATTRIBUTE_CONSTRAINT, null, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);
      Collection collection = createCollection(CODE, attribute);
      assertThat(collection.getAttributes()).isNotEmpty();

      final String collectionId = collection.getId();
      assertThatThrownBy(() -> collectionFacade.deleteCollectionAttribute(collectionId, ATTRIBUTE_ID))
            .isInstanceOf(NoResourcePermissionException.class);

      setCollectionGroupRoles(collection, Set.of(new Role(RoleType.Read), new Role(RoleType.AttributeEdit)));
      collectionFacade.deleteCollectionAttribute(collection.getId(), ATTRIBUTE_ID);

      collection = collectionDao.getCollectionByCode(CODE);
      assertThat(collection).isNotNull();
      assertThat(collection.getAttributes()).isEmpty();
   }

   private Collection setCollectionUserRoles(Collection collection, final Set<Role> roles) {
      Permissions permissions = collection.getPermissions();
      permissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      collection.setPermissions(permissions);
      return collectionDao.updateCollection(collection.getId(), collection, null);
   }

   private Collection setCollectionGroupRoles(Collection collection, final Set<Role> roles) {
      Permissions permissions = collection.getPermissions();
      permissions.updateGroupPermissions(Permission.buildWithRoles(this.group.getId(), roles));
      collection.setPermissions(permissions);
      return collectionDao.updateCollection(collection.getId(), collection, null);
   }

   @Test
   public void testDeleteAutoLinkRules() {
      Attribute attribute = new Attribute(ATTRIBUTE_ID, ATTRIBUTE_NAME, null, ATTRIBUTE_CONSTRAINT, null, null, ATTRIBUTE_FUNCTION, ATTRIBUTE_COUNT, null);
      Collection collection = createCollection(CODE, attribute);

      var rules = createRules(collection, attribute);
      collection.setRules(createRules(collection, attribute));
      collection = collectionDao.updateCollection(collection.getId(), collection, null);
      assertThat(collection.getRules().get("A")).isNotNull();

      Collection collection2 = createCollection(CODE2, attribute, rules);
      assertThat(collection2.getRules().get("A")).isNotNull();

      setCollectionGroupRoles(collection, Set.of(new Role(RoleType.Read), new Role(RoleType.AttributeEdit)));

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
      final AutoLinkRule rule = new AutoLinkRule(new Rule("rule1", Rule.RuleType.AUTO_LINK, Rule.RuleTiming.ALL, new DataDocument()));
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

      assertThatThrownBy(() -> collectionFacade.getCollectionPermissions(collectionId))
            .isInstanceOf(NoResourcePermissionException.class);

      setCollectionUserRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.UserConfig)));

      Permissions permissions = collectionFacade.getCollectionPermissions(collectionId);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), new Permission(user.getId(), Set.of(new Role(RoleType.UserConfig))));
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      String USER2 = "aaa" + user.getId().substring(3);
      userDao.createUser(new User(USER2, USER2, USER2, Collections.singleton(organization.getId())));

      var notifications = userNotificationFacade.getNotifications();
      assertThat(notifications).isEmpty();

      Collection collection = createCollection(CODE);
      final String collectionId = collection.getId();

      final Permission userPermission = Permission.buildWithRoles(user.getId(), Set.of(new Role(RoleType.UserConfig), new Role(RoleType.Manage)));

      assertThatThrownBy(() -> collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission)))
            .isInstanceOf(NoResourcePermissionException.class);

      setCollectionUserRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.UserConfig)));

      collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission));

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);

      Permission userPermission2 = Permission.buildWithRoles(USER2, Set.of(new Role(RoleType.Read)));
      organizationFacade.updateUserPermissions(organization.getId(), Set.of(userPermission2));
      projectFacade.updateUserPermissions(project.getId(), Set.of(userPermission2));
      collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission2));

      notifications = userNotificationDao.getRecentNotifications(USER2);
      assertThat(notifications).hasSize(3).anyMatch(n ->
            COLOR.equals(n.getData().getString(UserNotification.CollectionShared.COLLECTION_COLOR))
                  && USER2.equals(n.getUserId())
                  && collectionId.equals(n.getData().getString(UserNotification.CollectionShared.COLLECTION_ID)));

      collection = collectionDao.getCollectionById(collection.getId());
      collection.setColor(COLOR2);
      collectionFacade.updateCollection(collectionId, collection);

      notifications = userNotificationDao.getRecentNotifications(USER2);
      assertThat(notifications).hasSize(3).anyMatch(n ->
            COLOR2.equals(n.getData().getString(UserNotification.CollectionShared.COLLECTION_COLOR))
                  && USER2.equals(n.getUserId())
                  && collectionId.equals(n.getData().getString(UserNotification.CollectionShared.COLLECTION_ID)));

      userPermission2 = Permission.buildWithRoles(USER2, Collections.emptySet());
      organizationFacade.updateUserPermissions(organization.getId(), Set.of(userPermission2));
      projectFacade.updateUserPermissions(project.getId(), Set.of(userPermission2));
      collectionFacade.updateUserPermissions(collectionId, Set.of(userPermission2));

      notifications = userNotificationDao.getRecentNotifications(USER2);
      assertThat(notifications).isEmpty();
   }

   @Test
   public void testRemoveUserPermission() {
      String collectionId = createCollection(CODE).getId();

      assertThatThrownBy(() -> collectionFacade.removeUserPermission(collectionId, user.getId()))
            .isInstanceOf(NoResourcePermissionException.class);

      setCollectionUserRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.UserConfig)));
      collectionFacade.removeUserPermission(collectionId, user.getId());

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), this.groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      String collectionId = createCollection(CODE).getId();

      setCollectionGroupRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.UserConfig)));

      Permission groupPermission = Permission.buildWithRoles(group.getId(), Set.of(new Role(RoleType.DataWrite), new Role(RoleType.Manage)));
      collectionFacade.updateGroupPermissions(collectionId, Set.of(groupPermission));

      Permissions permissions = collectionDao.getCollectionByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), this.userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      String collectionId = createCollection(CODE).getId();

      setCollectionGroupRoles(collectionDao.getCollectionById(collectionId), Set.of(new Role(RoleType.UserConfig)));
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

      assertThat(collectionFacade.getCollections()).isEmpty();

      setProjectUserRoles(Set.of(new Role(RoleType.Read, true)));
      assertThat(collectionFacade.getCollections()).hasSize(2);

      setProjectUserRoles(Set.of(new Role(RoleType.Read)));
      assertThat(collectionFacade.getCollections()).isEmpty();

      setCollectionGroupRoles(collectionDao.getCollectionByCode("CD1"), Set.of(new Role(RoleType.Read)));
      assertThat(collectionFacade.getCollections()).hasSize(1);

      setCollectionGroupRoles(collectionDao.getCollectionByCode("CD2"), Set.of(new Role(RoleType.Read)));
      assertThat(collectionFacade.getCollections()).hasSize(2);
   }

   @Test
   public void testAttributeConversion() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE3));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, null, null, null, 0, null),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, null, null, null, 0, null)
            )
      );

      var values = Arrays.asList("New", "In Progress", "To Do", "Done", "Won't fix");
      var rnd = new Random();

      for (int i = 0; i < 100; i++) {
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
                  new Attribute("a1", "Task", null, null, null, null, null, 0, null),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, null, null, null, 0, null)
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
                  new Attribute("a1", "Task", null, null, null, null, null, 0, null),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, null, null, null, 0, null)
            )
      );

      var dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm:ss");
      var d1 = ZonedDateTime.of(LocalDateTime.of(2019, 11, 23, 8, 23, 10), ZoneId.systemDefault());
      var d2 = ZonedDateTime.of(LocalDateTime.of(2019, 2, 28, 23, 34, 12), ZoneId.systemDefault());
      var d3 = ZonedDateTime.of(LocalDateTime.of(1943, 3, 24, 6, 55, 19), ZoneId.systemDefault());
      var d4 = ZonedDateTime.of(LocalDateTime.of(1929, 4, 21, 9, 37, 1), ZoneId.systemDefault());

      var values = Arrays.asList(
            dtf.format(d1),
            dtf.format(d2),
            dtf.format(d3),
            dtf.format(d4)
      );

      var i = new AtomicInteger(1);
      values.forEach(value -> {
         documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-" + i.getAndIncrement()).append("a2", value)));
      });

      var attributes = collectionFacade.getCollection(collection.getId()).getAttributes();
      var attr = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr.setConstraint(new Constraint(ConstraintType.DateTime, new org.bson.Document("format", "DD/MM/YYYY H:mm:ss")));

      collectionFacade.updateCollectionAttribute(collection.getId(), attr.getId(), attr);

      var documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Object> res = new HashMap<>();
      documents.forEach(document -> {
         DataDocument data = dataDao.getData(collection.getId(), document.getId()); // we must skip DocumentFacade because with that, Date gets converted to String for compatibility with UI
         res.put(data.getString("a1"), data.getObject("a2"));
      });

      assertThat(res).contains(
            Map.entry("Task-1", Date.from(d1.toInstant())),
            Map.entry("Task-2", Date.from(d2.toInstant())),
            Map.entry("Task-3", Date.from(d3.toInstant())),
            Map.entry("Task-4", Date.from(d4.toInstant()))
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
            Map.entry("Task-4", "21/04/1929 9:37:01")
      );
   }

   @Test
   public void testPercentageAttributeConversion() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE3));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, null, null, null, 0, null),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, null, null, null, 0, null)
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
         DataDocument data = dataDao.getData(collection.getId(), document.getId()); //  we must skip DocumentFacade because with that, BigDecimal gets converted to String for compatibility with UI
         res.put(data.getString("a1"), data.getObject("a2"));
      });

      assertThat(res).contains(
            Map.entry("Task-1", 10L),
            Map.entry("Task-2", new BigDecimal("0.12")),
            Map.entry("Task-3", new BigDecimal("0.12")),
            Map.entry("Task-4", new BigDecimal("0.0012"))
      );

      // now back to no constraint
      var attr2 = attributes.stream().filter(attribute -> attribute.getName().equals(ATTRIBUTE_STATE)).findFirst().get();
      attr2.setConstraint(new Constraint(ConstraintType.None, null));
      collectionFacade.updateCollectionAttribute(collection.getId(), attr2.getId(), attr2);

      documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Object> res2 = new HashMap<>();
      documents.forEach(document -> {
         DataDocument data = dataDao.getData(collection.getId(), document.getId()); //  we must skip DocumentFacade because with that,, BigDecimal gets converted to String for compatibility with UI
         res2.put(data.getString("a1"), data.getObject("a2"));
      });

      assertThat(res2).contains(
            Map.entry("Task-1", 10L),
            Map.entry("Task-2", new BigDecimal("0.12")),
            Map.entry("Task-3", new BigDecimal("0.12")),
            Map.entry("Task-4", new BigDecimal("0.0012"))
      );
   }

   @Test
   public void testUpdateDocumentWithZapier() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE4));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, null, null, null, 0, null),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, null, null, null, 0, null)
            )
      );

      var values = Arrays.asList("10", "12%", "0.12", "0.12 %");

      var i = new AtomicInteger(1);
      values.forEach(value -> {
         documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-" + i.getAndIncrement()).append("a2", value)));
      });

      zapierFacade.updateDocument(collection.getId(), "a1", new DataDocument("a1", "Task-2").append("a2", "94%"));

      var documents = documentDao.getDocumentsByCollection(collection.getId());

      Map<String, Object> res = new HashMap<>();
      documents.forEach(document -> {
         DataDocument data = dataDao.getData(collection.getId(), document.getId()); //  we must skip DocumentFacade because with that, BigDecimal gets converted to String for compatibility with UI
         res.put(data.getString("a1"), data.getObject("a2"));
      });

      assertThat(res).hasSize(4);
      assertThat(res).contains(Map.entry("Task-2", "94%"));
   }

   @Test
   public void testZapierUpdateReporting() {
      Collection collection = collectionFacade.createCollection(prepareCollection(CODE5));
      collectionFacade.createCollectionAttributes(
            collection.getId(),
            Arrays.asList(
                  new Attribute("a1", "Task", null, null, null, null, null, 0, null),
                  new Attribute("a2", ATTRIBUTE_STATE, null, null, null, null, null, 0, null)
            )
      );
      collection = collectionFacade.getCollection(collection.getId()); // read it back with the attributes

      var oldDocument = documentFacade.createDocument(collection.getId(), new Document(new DataDocument("a1", "Task-1")));
      var newDocument = documentFacade.patchDocumentData(collection.getId(), oldDocument.getId(), new DataDocument("a2", "10"));

      var diffData = ZapierRuleTaskExecutor.getZapierUpdateDocumentMessage(collection, oldDocument, newDocument);

      assertThat(diffData).hasSize(7).containsKey("_id");
      assertThat(diffData.getString("Task")).isEqualTo("Task-1");
      assertThat(diffData.getBoolean("_changed_Task")).isEqualTo(false);
      assertThat(diffData.getString("_previous_Task")).isEqualTo("Task-1");
      assertThat(diffData.getLong("State")).isEqualTo(10L);
      assertThat(diffData.getBoolean("_changed_State")).isEqualTo(true);
      assertThat(diffData.getString("_previous_State")).isEqualTo(null);
   }

   public void testTaskExecutor() throws InterruptedException {
      taskExecutor.submitTask(contextualTaskFactory.getInstance(ListCollectionsIn10SecondsTask.class));
      Thread.sleep(15_000);
   }

}
