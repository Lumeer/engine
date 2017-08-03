/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.CollectionMetadata;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Permission;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.InvalidValueException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class CollectionMetadataFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private UserGroupFacade userGroupFacade;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private DocumentFacade documentFacade;

   private final String organizationCode = "LMR";
   private final String projectCode = "PR";

   @Before
   public void setUp() throws Exception {
      DataFilter filter = dataStorageDialect.documentFilter("{}");
      systemDataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, filter);
      systemDataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, filter);

      organizationFacade.createOrganization(new Organization(organizationCode, "Lumeer"));
      organizationFacade.setOrganizationCode(organizationCode);

      projectFacade.createProject(new Project(projectCode, "project"));
      projectFacade.setCurrentProjectCode(projectCode);

   }

   @Test
   public void testCreateInitialMetadata() throws Exception {
      final String collection = "someCode";
      final String collectionName = "CollectionMetadataFacadeCollectionCreateInitialMetadata";
      collectionMetadataFacade.createInitialMetadata(collection, new Collection(collectionName), "user", Arrays.asList("r1", "r2"));

      CollectionMetadata metadata = collectionMetadataFacade.getCollectionMetadata(collection);

      assertThat(metadata.getName()).isEqualTo(collectionName);
      assertThat(metadata.getCode()).isEqualTo(collection);
      assertThat(metadata.getAttributes()).isEmpty();
      assertThat(metadata.getLastTimeUsed()).isBeforeOrEqualsTo(new Date());
      assertThat(metadata.getRecentlyUsedDocumentIds()).isEmpty();
      assertThat(metadata.getCreateDate()).isBeforeOrEqualsTo(new Date());
      assertThat(metadata.getCreatedBy()).isEqualTo(userFacade.getUserEmail());
      assertThat(metadata.getCustomMetadata()).isEmpty();
      assertThat(metadata.getDocumentCount()).isEqualTo(0);
      assertThat(metadata.getPermissions().get(LumeerConst.Security.USERS_KEY)).hasSize(1);
      assertThat(metadata.getPermissions().get(LumeerConst.Security.GROUP_KEY)).isEmpty();
   }

   @Test
   public void testGetPermissions() throws Exception {
      final String collectionName = "CollectionMetadataFacadeGetPermissions";
      setUpCollection(collectionName);

      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      Map<String, List<Permission>> permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);

      Set<String> roleNamesSet = LumeerConst.Security.RESOURCE_ROLES.get(LumeerConst.Security.COLLECTION_RESOURCE);
      String[] roleNames = roleNamesSet.toArray(new String[roleNamesSet.size()]);
      assertThat(permissions).isNotNull().containsOnlyKeys(LumeerConst.Security.USERS_KEY, LumeerConst.Security.GROUP_KEY);
      assertThat(permissions.get(LumeerConst.Security.USERS_KEY)).extracting("name").containsOnly(userFacade.getUserEmail());
      assertThat(permissions.get(LumeerConst.Security.USERS_KEY).get(0).getRoles()).containsOnly(roleNames);

      assertThat(permissions.get(LumeerConst.Security.GROUP_KEY).isEmpty());
   }

   @Test
   public void testHasRole() throws Exception {
      final String collectionName = "CollectionMetadataFacadeHasRole";
      setUpCollection(collectionName);

      final String role = LumeerConst.Security.ROLE_MANAGE;
      final String user = userFacade.getUserEmail();
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      assertThat(collectionMetadataFacade.hasRole(projectCode, collectionCode, role)).isTrue();

      List<String> roleNames = new ArrayList<>(LumeerConst.Security.RESOURCE_ROLES.get(LumeerConst.Security.COLLECTION_RESOURCE));
      roleNames.remove(role);
      collectionMetadataFacade.setRolesToUser(projectCode, collectionCode, roleNames, user);
      assertThat(collectionMetadataFacade.hasRole(projectCode, collectionCode, role)).isFalse();

      final String g1 = "g1";
      userGroupFacade.addGroups(organizationCode, g1);
      userGroupFacade.addUser(organizationCode, user, g1);

      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role), g1);
      assertThat(collectionMetadataFacade.hasRole(projectCode, collectionCode, role)).isTrue();

      collectionMetadataFacade.setRolesToGroup(projectCode, collectionCode, Collections.emptyList(), g1);
      assertThat(collectionMetadataFacade.hasRole(projectCode, collectionCode, role)).isFalse();
   }

   @Test
   public void testAddUserWithRoles() throws Exception {
      final String collectionName = "CollectionMetadataFacadeAddUserWithRoles";
      setUpCollection(collectionName);

      final String role = LumeerConst.Security.ROLE_MANAGE;
      final String user = userFacade.getUserEmail();
      final String user2 = "user2";
      final String user3 = "user3";
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      Map<String, List<Permission>> permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      List<Permission> usersList = permissions.get(LumeerConst.Security.USERS_KEY);

      assertThat(usersList).hasSize(1).extracting("name").containsOnly(user);

      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, Collections.singletonList(role), user2);
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, Collections.singletonList(role), user3);

      permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      usersList = permissions.get(LumeerConst.Security.USERS_KEY);
      assertThat(usersList).hasSize(3).extracting("name").containsOnly(user, user2, user3);

      for (String u : Arrays.asList(user2, user3)) {
         Permission permission = usersList.stream().filter(p -> p.getName().equals(u)).findFirst().orElse(null);
         assertThat(permission).isNotNull();
         assertThat(permission.getRoles()).containsOnly(role);
      }
   }

   @Test
   public void testAddGroupWithRoles() throws Exception {
      final String collectionName = "CollectionMetadataFacadeAddGroupWithRoles";
      setUpCollection(collectionName);

      final String role = LumeerConst.Security.ROLE_MANAGE;
      final String group = "group";
      final String group2 = "group2";
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      Map<String, List<Permission>> permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      List<Permission> groupList = permissions.get(LumeerConst.Security.GROUP_KEY);

      assertThat(groupList).isEmpty();

      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role), group);
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role), group2);

      permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      groupList = permissions.get(LumeerConst.Security.GROUP_KEY);
      assertThat(groupList).hasSize(2).extracting("name").containsOnly(group, group2);

      for (String g : Arrays.asList(group, group2)) {
         Permission permission = groupList.stream().filter(p -> p.getName().equals(g)).findFirst().orElse(null);
         assertThat(permission).isNotNull();
         assertThat(permission.getRoles()).containsOnly(role);
      }
   }

   @Test
   public void testRemoveUser() throws Exception {
      final String collectionName = "CollectionMetadataFacadeRemoveUser";
      setUpCollection(collectionName);

      final String role = LumeerConst.Security.ROLE_MANAGE;
      final String user = userFacade.getUserEmail();
      final String user2 = "user2";
      final String user3 = "user3";
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, Collections.singletonList(role), user2);
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, Collections.singletonList(role), user3);

      Map<String, List<Permission>> permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      List<Permission> usersList = permissions.get(LumeerConst.Security.USERS_KEY);

      assertThat(usersList).hasSize(3).extracting("name").containsOnly(user, user2, user3);

      collectionMetadataFacade.removeUser(projectCode, collectionCode, user3);
      collectionMetadataFacade.removeUser(projectCode, collectionCode, user);

      permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      usersList = permissions.get(LumeerConst.Security.USERS_KEY);

      assertThat(usersList).hasSize(1).extracting("name").containsOnly(user2);
   }

   @Test
   public void testRemoveGroup() throws Exception {
      final String collectionName = "CollectionMetadataFacadeRemoveGroup";
      setUpCollection(collectionName);

      final String role = LumeerConst.Security.ROLE_MANAGE;
      final String group = "group";
      final String group2 = "group2";
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role), group);
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role), group2);

      Map<String, List<Permission>> permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      List<Permission> groupList = permissions.get(LumeerConst.Security.GROUP_KEY);

      assertThat(groupList).hasSize(2).extracting("name").containsOnly(group, group2);

      collectionMetadataFacade.removeGroup(projectCode, collectionCode, group);

      permissions = collectionMetadataFacade.getPermissions(projectCode, collectionCode);
      groupList = permissions.get(LumeerConst.Security.GROUP_KEY);

      assertThat(groupList).hasSize(1).extracting("name").containsOnly(group2);
   }

   @Test
   public void testSetRolesToUser() throws Exception {
      final String collectionName = "CollectionMetadataFacadeAddRolesToUser";
      setUpCollection(collectionName);

      final String role1 = LumeerConst.Security.ROLE_MANAGE;
      final String role2 = LumeerConst.Security.ROLE_READ;
      final String role3 = LumeerConst.Security.ROLE_WRITE;
      final String user = "someUser";
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, Collections.singletonList(role1), user);

      Permission permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)
                                                      .stream().filter(p -> p.getName().equals(user)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).containsOnly(role1);

      collectionMetadataFacade.setRolesToUser(projectCode, collectionCode, Arrays.asList(role1, role2, role3), user);
      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)
                                           .stream().filter(p -> p.getName().equals(user)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).containsOnly(role1, role2, role3);

      collectionMetadataFacade.setRolesToUser(projectCode, collectionCode, Collections.emptyList(), user);
      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)
                                           .stream().filter(p -> p.getName().equals(user)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).isEmpty();
   }

   @Test
   public void testSetRolesToGroup() throws Exception {
      final String collectionName = "CollectionMetadataFacadeAddRolesToGroup";
      setUpCollection(collectionName);

      final String role1 = LumeerConst.Security.ROLE_MANAGE;
      final String role2 = LumeerConst.Security.ROLE_READ;
      final String role3 = LumeerConst.Security.ROLE_WRITE;
      final String group = "someGroup";
      final String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role1), group);

      Permission permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY)
                                                      .stream().filter(p -> p.getName().equals(group)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).containsOnly(role1);

      collectionMetadataFacade.setRolesToGroup(projectCode, collectionCode, Arrays.asList(role1, role2, role3), group);
      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY)
                                           .stream().filter(p -> p.getName().equals(group)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).containsOnly(role1, role2, role3);

      collectionMetadataFacade.setRolesToGroup(projectCode, collectionCode, Collections.emptyList(), group);
      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY)
                                           .stream().filter(p -> p.getName().equals(group)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).isEmpty();
   }

   @Test
   public void testGetAttributesNames() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionAttributesNames";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      Set<String> attributes = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(attributes).containsOnly(name1, name2);
   }

   @Test
   public void testGetAttributesInfo() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionAttributesInfo";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      List<Attribute> attributesInfo = collectionMetadataFacade.getAttributesInfo(collection);
      assertThat(attributesInfo).isEmpty();

      String name1 = "attribute 1";
      String name2 = "attribute 2";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name2);

      attributesInfo = collectionMetadataFacade.getAttributesInfo(collection);

      assertThat(attributesInfo).hasSize(2);
   }

   @Test
   public void testGetAttributeInfo() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionAttributeInfo";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      // nested attribute
      String parent = "attribute";
      String child = "child";
      String attributeName = parent + "." + child;
      Attribute attribute = collectionMetadataFacade.getAttributeInfo(collection, attributeName);

      // non existing
      assertThat(attribute).isNull();

      collectionMetadataFacade.addOrIncrementAttribute(collection, parent);
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeName);
      attribute = collectionMetadataFacade.getAttributeInfo(collection, attributeName);

      // existing
      assertThat(attribute).isNotNull();
      assertThat(attribute.getName()).isEqualTo(child);
      assertThat(attribute.getFullName()).isEqualTo(attributeName);

      // double nested attribute
      String child2 = "child 2";
      String attributeName2 = attributeName + "." + child2;
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeName2);
      attribute = collectionMetadataFacade.getAttributeInfo(collection, attributeName2);

      assertThat(attribute).isNotNull();
      assertThat(attribute.getName()).isEqualTo(child2);
   }

   @Test
   public void testRenameAttribute() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionRenameAttribute";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String oldName = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldName);

      String newName = "attribute 2";
      collectionMetadataFacade.renameAttribute(collection, oldName, newName);
      Set<String> columns = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(columns).containsOnly(newName);

      String oldName2 = "attribute 3";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldName2);
      // we try to rename attribute to name that already exists in collection
      assertThatThrownBy(() -> collectionMetadataFacade.renameAttribute(collection, oldName2, newName))
            .isInstanceOf(AttributeAlreadyExistsException.class);

      // nested attribute
      String oldNameNested = newName + ".child";
      String newNameNested = newName + ".child new";
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldNameNested);
      collectionMetadataFacade.renameAttribute(collection, oldNameNested, newNameNested);
      Attribute attribute = collectionMetadataFacade.getAttributeInfo(collection, newNameNested);

      assertThat(attribute).isNotNull();
   }

   @Test
   public void testDropAttribute() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionDropAttribute";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      Set<String> columns = collectionMetadataFacade.getAttributesNames(collection);
      assertThat(columns).hasSize(1);

      collectionMetadataFacade.dropAttribute(collection, name);

      columns = collectionMetadataFacade.getAttributesNames(collection);
      assertThat(columns).isEmpty();

      // we try to drop non existing attribute - nothing happens
      collectionMetadataFacade.dropAttribute(collection, "attribute2");

      // nested attribute
      String nestedAttribute = name + ".child";
      assertThat(collectionMetadataFacade.getAttributeInfo(collection, nestedAttribute)).isNull();

      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      columns = collectionMetadataFacade.getAttributesNames(collection);
      assertThat(columns).hasSize(2);

      collectionMetadataFacade.dropAttribute(collection, name);
      columns = collectionMetadataFacade.getAttributesNames(collection);
      assertThat(columns).isEmpty();

      assertThat(collectionMetadataFacade.getAttributeInfo(collection, nestedAttribute)).isNull();
      assertThat(collectionMetadataFacade.getAttributeInfo(collection, name)).isNull();
   }

   @Test
   public void testAddOrIncrementAttribute() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionAddOrIncrementAttribute";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      long count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(1);

      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(2);

      // nested attribute
      String nestedAttribute = name + ".child";
      count = collectionMetadataFacade.getAttributeCount(collection, nestedAttribute);

      assertThat(count).isZero();

      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      count = collectionMetadataFacade.getAttributeCount(collection, nestedAttribute);

      assertThat(count).isEqualTo(1);
   }

   @Test
   public void testDropOrDecrementAttribute() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionDropOrDecrementAttribute";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, name);
      long count = collectionMetadataFacade.getAttributeCount(collection, name);

      assertThat(count).isEqualTo(1);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, name);
      count = collectionMetadataFacade.getAttributeCount(collection, name);
      Set<String> attributeInfo = collectionMetadataFacade.getAttributesNames(collection);

      assertThat(count).isZero();
      assertThat(attributeInfo).isEmpty();

      // nested attribute
      String nestedAttribute = name + ".child";
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);

      collectionMetadataFacade.dropOrDecrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.dropOrDecrementAttribute(collection, nestedAttribute);

      count = collectionMetadataFacade.getAttributeCount(collection, nestedAttribute);

      assertThat(count).isZero();
   }

   @Test
   public void testGetSetDropCustomMetadata() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionSetGetDropCustomMetadata";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      // there is no custom metadata - we should obtain empty list
      assertThat(collectionMetadataFacade.getCustomMetadata(collection)).isEmpty();

      String metaKey1 = "meta key 1";
      String metaValue1 = "value 1";

      // we set one custom value
      collectionMetadataFacade.setCustomMetadata(collection, new DataDocument(metaKey1, metaValue1));
      assertThat(collectionMetadataFacade.getCustomMetadata(collection).get(metaKey1).toString()).isEqualTo(metaValue1);

      // we try to drop non existing key, but dropAttribute in DataStorage does not return value, so we do not know it
      collectionMetadataFacade.dropCustomMetadata(collection, "random key");

      // we drop existing key and after that it is not there
      collectionMetadataFacade.dropCustomMetadata(collection, metaKey1);
      assertThat(collectionMetadataFacade.getCustomMetadata(collection)).doesNotContainKey(metaKey1);
   }

   @Test
   public void testCheckAndConvertAttributesValues() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionCheckAttributesValues";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String attribute = "attribute";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute);
      String constraint1 = "greaterThan:3";
      collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint1);
      int valueValid1 = 4;
      int valueInvalid1 = 2;

      String attribute2 = "attribute2";
      String child = "child";
      String nestedAttribute = attribute2 + "." + child;
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute2);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      String constraint2 = "lessThan:8";
      collectionMetadataFacade.addAttributeConstraint(collection, nestedAttribute, constraint2);
      int valueValid2 = 4;
      int valueInvalid2 = 9;

      String attribute3 = "attribute3";
      String nestedAttribute2 = attribute3 + "." + child;
      String doubleChild = "double child";
      String doubleNestedAttribute = nestedAttribute2 + "." + doubleChild;
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute3);
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute2);
      collectionMetadataFacade.addOrIncrementAttribute(collection, doubleNestedAttribute);
      String constraint3 = "isNumber";
      collectionMetadataFacade.addAttributeConstraint(collection, doubleNestedAttribute, constraint3);
      int valueValid3 = 5;
      String valueInvalid3 = "a";

      DataDocument validDoc = new DataDocument()
            .append(attribute, valueValid1)
            .append(attribute2,
                  new DataDocument(child, valueValid2))
            .append(attribute3,
                  new DataDocument(child,
                        new DataDocument(
                              doubleChild,
                              valueValid3)));

      DataDocument invalidDoc = new DataDocument()
            .append(attribute, valueInvalid1)
            .append(attribute2,
                  new DataDocument(child, valueInvalid2))
            .append(attribute3,
                  new DataDocument(child,
                        new DataDocument(
                              doubleChild,
                              valueInvalid3)));

      DataDocument validAfterConvert = collectionMetadataFacade.checkAndConvertAttributesValues(collection, validDoc);
      assertThatThrownBy(() -> collectionMetadataFacade.checkAndConvertAttributesValues(collection, invalidDoc)).isInstanceOf(InvalidValueException.class).hasMessageContaining("Invalid value");

      // we have to get values as strings, because ConstraintManager always returns strings as a result of validation
      assertThat(validAfterConvert.getInteger(attribute))
            .as("valid attribute")
            .isEqualTo(valueValid1);
      assertThat(validAfterConvert.getDataDocument(attribute2).getInteger(child))
            .as("valid nested attribute")
            .isEqualTo(valueValid2);
      assertThat(validAfterConvert.getDataDocument(attribute3).getDataDocument(child).getInteger(doubleChild))
            .as("valid double nested attribute")
            .isEqualTo(valueValid3);
   }

   @Test
   public void testGetSetLastTimeUsed() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionLastTimeUsed";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      collectionMetadataFacade.setLastTimeUsedNow(collection);
      Date last = collectionMetadataFacade.getLastTimeUsed(collection);

      assertThat(last).isAfterOrEqualsTo(collectionMetadataFacade.getCollectionMetadata(collection).getCreateDate());
      assertThat(new Date()).isAfterOrEqualsTo(last);
   }

   @Test
   public void testGetAddDropConstraint() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionAddAttributeConstraint";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String attribute = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute);

      List<String> constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).isEmpty();

      String constraint1 = "isNumber";
      String constraint2 = "lessThan:3";

      collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint1);
      collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint2);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).containsOnly(constraint1, constraint2);

      collectionMetadataFacade.dropAttributeConstraint(collection, attribute, constraint1);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).containsOnly(constraint2);

      collectionMetadataFacade.dropAttributeConstraint(collection, attribute, constraint2);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attribute);
      assertThat(constraints).isEmpty();

      // we try to add dummy constraint
      String constraint3 = "dummy";
      assertThatThrownBy(() -> collectionMetadataFacade.addAttributeConstraint(collection, attribute, constraint3))
            .isInstanceOf(InvalidConstraintException.class);

      // nested attribute
      String nestedAttribute = attribute + ".child";
      collectionMetadataFacade.addOrIncrementAttribute(collection, nestedAttribute);
      collectionMetadataFacade.addAttributeConstraint(collection, nestedAttribute, constraint1);
      constraints = collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, nestedAttribute);
      assertThat(constraints).containsOnly(constraint1);
   }

   @Test
   public void testGetAddRecentlyUsedDocumentsIds() throws Exception {
      final String collectionName = "CollectionMetadataFacadeCollectionRecentlyUsedDocuments";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      List<String> ids = new ArrayList<>();

      int listSize = configurationFacade.getConfigurationInteger(LumeerConst.NUMBER_OF_RECENT_DOCS_PROPERTY)
                                        .orElse(LumeerConst.Collection.DEFAULT_NUMBER_OF_RECENT_DOCUMENTS);

      // we add so many ids, as is the size of the list
      for (int i = 0; i < listSize; i++) {
         String id = "id" + i;
         ids.add(id);
         collectionMetadataFacade.addRecentlyUsedDocumentId(collection, id);
      }

      List<String> recentlyUsed = collectionMetadataFacade.getRecentlyUsedDocumentsIds(collection);
      assertThat(recentlyUsed).containsOnlyElementsOf(ids); // all ids are there
      assertThat(recentlyUsed.get(0)).isEqualTo(ids.get(ids.size() - 1)); // the last one added is at the beginning of the list

      collectionMetadataFacade.addRecentlyUsedDocumentId(collection, ids.get(1)); // we add id1 again
      List<String> recentlyUsed1 = collectionMetadataFacade.getRecentlyUsedDocumentsIds(collection);
      assertThat(recentlyUsed1.get(0)).isEqualTo(ids.get(1)); // now id1 is at the beginning of the list

      String newId = "new id";
      collectionMetadataFacade.addRecentlyUsedDocumentId(collection, newId); // we add totally new id so we exceed the capacity of the list
      List<String> recentlyUsed2 = collectionMetadataFacade.getRecentlyUsedDocumentsIds(collection);
      assertThat(recentlyUsed2.get(0)).isEqualTo(newId); // new id is at the beginning of the list
      assertThat(recentlyUsed2).doesNotContain(ids.get(0)); // the first (and firstly added) id is no more in the list
   }

   private void setUpCollection(String collectionName) {
      String collectionCode = collectionMetadataFacade.getCollectionCodeFromName(collectionName);
      if (collectionCode != null) {
         dataStorage.dropCollection(collectionCode);
      }
   }

   @Test
   public void testDecrementDocumentCount() throws DbException {
      final String collectionName = "CollectionMetadataFacadeCollectionCheckDocumentCount";
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      List<String> keys = Arrays.asList("keySuper", "key2", "key3", "key4", "key5", "key6", "key7");
      List<String> objects = Arrays.asList("object1", "object2", "object3", "object4", "object5", "object6", "object7");
      List<String> documentIds = new ArrayList<>();

      for (int i = 0; i < keys.size(); i++) {
         String documentID = documentFacade.createDocument(collectionCode, new DataDocument(keys.get(i), objects.get(i)));
         documentIds.add(documentID);
      }

      assertThat(collectionFacade.getCollection(collectionCode).getDocumentCount()).isEqualTo(keys.size());
      documentFacade.dropDocument(collectionCode, documentIds.get(4));
      assertThat(collectionFacade.getCollection(collectionCode).getDocumentCount()).isEqualTo(keys.size() - 1);
      documentFacade.dropDocument(collectionCode, documentIds.get(2));
      assertThat(collectionFacade.getCollection(collectionCode).getDocumentCount()).isEqualTo(keys.size() - 2);
   }
}
