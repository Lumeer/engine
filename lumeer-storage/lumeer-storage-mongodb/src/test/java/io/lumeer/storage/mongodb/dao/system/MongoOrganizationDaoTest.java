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
package io.lumeer.storage.mongodb.dao.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

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
   private static final JsonPermissions PERMISSIONS;
   private static final JsonPermission GROUP_PERMISSION;

   private static final String NOT_EXISTING_CODE = "NOT_EXISTING_CODE";
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      PERMISSIONS = new JsonPermissions();

      JsonPermission userPermission = new JsonPermission(USER, Organization.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));
      PERMISSIONS.updateUserPermissions(userPermission);

      GROUP_PERMISSION = new JsonPermission(GROUP, Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);
   }

   private MongoOrganizationDao organizationDao;

   @Before
   public void initOrganizationDao() {
      organizationDao = new MongoOrganizationDao();
      organizationDao.setDatabase(database);

      organizationDao.createOrganizationsRepository();
   }

   private JsonOrganization prepareOrganization(String code) {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(code);
      organization.setName(NAME);
      organization.setColor(COLOR);
      organization.setIcon(ICON);
      organization.setPermissions(new JsonPermissions(PERMISSIONS));
      return organization;
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = prepareOrganization(CODE1);

      String id = organizationDao.createOrganization(organization).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Organization storedOrganization = organizationDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
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
      JsonOrganization organization = prepareOrganization(CODE1);
      organizationDao.databaseCollection().insertOne(organization);

      Organization organization2 = prepareOrganization(CODE1);
      assertThatThrownBy(() -> organizationDao.createOrganization(organization2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetOrganizationByCode() {
      JsonOrganization organization = prepareOrganization(CODE1);
      organizationDao.databaseCollection().insertOne(organization);

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE1);
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getId()).isNotNull().isNotEmpty();
      assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertThat(storedOrganization.getName()).isEqualTo(NAME);
   }

   @Test
   public void testGetOrganizationById() {
      Organization organization = prepareOrganization(CODE1);
      Organization createdOrganization = organizationDao.createOrganization(organization);

      Organization storedOrganization = organizationDao.getOrganizationById(createdOrganization.getId());
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
      JsonOrganization organization = prepareOrganization(CODE1);
      organizationDao.databaseCollection().insertOne(organization);

      JsonOrganization organization2 = prepareOrganization(CODE2);
      organizationDao.databaseCollection().insertOne(organization2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testGetOrganizationsNoReadRole() {
      JsonOrganization organization = prepareOrganization(CODE1);
      Permission userPermission = new JsonPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      organization.getPermissions().updateUserPermissions(userPermission);
      organizationDao.databaseCollection().insertOne(organization);

      JsonOrganization organization2 = prepareOrganization(CODE2);
      Permission groupPermission = new JsonPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      organization2.getPermissions().updateGroupPermissions(groupPermission);
      organizationDao.databaseCollection().insertOne(organization2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).isEmpty();
   }

   @Test
   public void testGetOrganizationsGroupRole() {
      JsonOrganization organization = prepareOrganization(CODE1);
      organizationDao.databaseCollection().insertOne(organization);

      JsonOrganization organization2 = prepareOrganization(CODE2);
      organizationDao.databaseCollection().insertOne(organization2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testDeleteOrganization() {
      JsonOrganization organization = prepareOrganization(CODE1);
      organizationDao.databaseCollection().insertOne(organization);
      assertThat(organization.getId()).isNotNull();

      organizationDao.deleteOrganization(organization.getId());

      Organization storedOrganization = organizationDao.databaseCollection().find(MongoFilters.idFilter(organization.getId())).first();
      assertThat(storedOrganization).isNull();
   }

   @Test
   public void testDeleteOrganizationNotExisting() {
      assertThatThrownBy(() -> organizationDao.deleteOrganization(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateOrganizationCode() {
      JsonOrganization organization = prepareOrganization(CODE1);
      String id = organizationDao.createOrganization(organization).getId();
      assertThat(id).isNotNull().isNotEmpty();

      JsonOrganization organization2 = prepareOrganization(CODE2);
      organizationDao.updateOrganization(id, organization2);

      JsonOrganization storedOrganization = organizationDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getCode()).isEqualTo(CODE2);
   }

   @Test
   public void testUpdateOrganizationPermissions() {
      JsonOrganization organization = prepareOrganization(CODE1);
      String id = organizationDao.createOrganization(organization).getId();
      assertThat(id).isNotNull().isNotEmpty();

      organization.getPermissions().removeUserPermission(USER);
      organization.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      organizationDao.updateOrganization(id, organization);

      JsonOrganization storedOrganization = organizationDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedOrganization.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateOrganizationExistingCode() {
      JsonOrganization organization = prepareOrganization(CODE1);
      organizationDao.databaseCollection().insertOne(organization);

      JsonOrganization organization2 = prepareOrganization(CODE2);
      organizationDao.databaseCollection().insertOne(organization2);

      organization2.setCode(CODE1);
      assertThatThrownBy(() -> organizationDao.updateOrganization(organization2.getId(), organization2))
            .isInstanceOf(StorageException.class);
   }

}
