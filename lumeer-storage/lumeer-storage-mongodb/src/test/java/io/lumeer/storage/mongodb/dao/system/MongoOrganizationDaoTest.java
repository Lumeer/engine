/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.mongodb.dao.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.DatabaseQuery;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoOrganization;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MongoOrganizationDaoTest extends MongoDbTestBase {

   private static final String USER = "testUser";
   private static final String USER2 = "testUser2";

   private static final String GROUP = "testGroup";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";
   private static final MongoPermissions PERMISSIONS;
   private static final MongoPermission GROUP_PERMISSION;

   private static final String NOT_EXISTING_CODE = "NOT_EXISTING_CODE";
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      MongoPermission userPermission = new MongoPermission();
      PERMISSIONS = new MongoPermissions();
      GROUP_PERMISSION = new MongoPermission();

      userPermission.setName(USER);
      userPermission.setRoles(Organization.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));

      PERMISSIONS.updateUserPermissions(userPermission);

      GROUP_PERMISSION.setName(GROUP);
      GROUP_PERMISSION.setRoles(Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);
   }

   private MongoOrganizationDao organizationDao;

   @Before
   public void initOrganizationDao() {
      organizationDao = new MongoOrganizationDao();
      organizationDao.setDatabase(database);
      organizationDao.setDatastore(datastore);

      organizationDao.ensureIndexes();
   }

   private MongoOrganization prepareOrganization(String code) {
      MongoOrganization organization = new MongoOrganization();
      organization.setCode(code);
      organization.setName(NAME);
      organization.setColor(COLOR);
      organization.setIcon(ICON);
      organization.setPermissions(new MongoPermissions(PERMISSIONS));
      return organization;
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = prepareOrganization(CODE1);

      String id = organizationDao.createOrganization(organization).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Organization storedOrganization = datastore.get(MongoOrganization.class, new ObjectId(id));
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getPermissions()).isEqualTo(PERMISSIONS);
      assertions.assertAll();
   }

   @Test
   public void testCreateOrganizationExistingCode() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE1);
      assertThatThrownBy(() -> organizationDao.createOrganization(organization2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testGetOrganizationByCode() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE1);
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getId()).isNotNull().isNotEmpty();
      assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertThat(storedOrganization.getName()).isEqualTo(NAME);
   }

   @Test
   public void testGetOrganizationByCodeNotExisting() {
      assertThatThrownBy(() -> organizationDao.getOrganizationByCode(NOT_EXISTING_CODE))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasFieldOrPropertyWithValue("resourceType", ResourceType.ORGANIZATION);
   }

   @Test
   public void testGetOrganizations() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE2);
      datastore.save(organization2);

      DatabaseQuery query = new DatabaseQuery.Builder(USER).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testGetOrganizationsNoReadRole() {
      Organization organization = prepareOrganization(CODE1);
      Permission userPermission = new MongoPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      organization.getPermissions().updateUserPermissions(userPermission);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE2);
      Permission groupPermission = new MongoPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      organization2.getPermissions().updateGroupPermissions(groupPermission);
      datastore.save(organization2);

      DatabaseQuery query = new DatabaseQuery.Builder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).isEmpty();
   }

   @Test
   public void testGetOrganizationsGroupRole() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE2);
      datastore.save(organization2);

      DatabaseQuery query = new DatabaseQuery.Builder(USER2).groups(Collections.singleton(GROUP)).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testDeleteOrganization() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);
      assertThat(organization.getId()).isNotNull();

      organizationDao.deleteOrganization(organization.getId());

      Organization storedOrganization = datastore.get(MongoOrganization.class, new ObjectId(organization.getId()));
      assertThat(storedOrganization).isNull();
   }

   @Test
   public void testDeleteOrganizationNotExisting() {
      assertThatThrownBy(() -> organizationDao.deleteOrganization(NOT_EXISTING_ID))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testUpdateOrganizationCode() {
      MongoOrganization organization = prepareOrganization(CODE1);
      String id = datastore.save(organization).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      MongoOrganization organization2 = prepareOrganization(CODE2);
      organizationDao.updateOrganization(id, organization2);

      MongoOrganization storedOrganization = datastore.get(MongoOrganization.class, new ObjectId(id));
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getCode()).isEqualTo(CODE2);
   }

   @Test
   public void testUpdateOrganizationPermissions() {
      MongoOrganization organization = prepareOrganization(CODE1);
      String id = datastore.save(organization).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      organization.getPermissions().removeUserPermission(USER);
      organization.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      organizationDao.updateOrganization(id, organization);

      MongoOrganization storedOrganization = datastore.get(MongoOrganization.class, new ObjectId(id));
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedOrganization.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateOrganizationExistingCode() {
      MongoOrganization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      MongoOrganization organization2 = prepareOrganization(CODE2);
      datastore.save(organization2);

      organization2.setCode(CODE1);
      assertThatThrownBy(() -> organizationDao.updateOrganization(organization2.getId(), organization2))
            .isInstanceOf(DuplicateKeyException.class);
   }

}
