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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaOrganization;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MorphiaOrganizationDaoTest extends MongoDbTestBase {

   private static final String USER = "testUser";
   private static final String USER2 = "testUser2";

   private static final String GROUP = "testGroup";
   private static final String GROUP2 = "testGroup2";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";
   private static final MorphiaPermissions PERMISSIONS;
   private static final MorphiaPermission GROUP_PERMISSION;

   private static final String NOT_EXISTING_CODE = "NOT_EXISTING_CODE";
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   static {
      MorphiaPermission userPermission = new MorphiaPermission();
      PERMISSIONS = new MorphiaPermissions();
      GROUP_PERMISSION = new MorphiaPermission();

      userPermission.setId(USER);
      userPermission.setRoles(Organization.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));

      PERMISSIONS.updateUserPermissions(userPermission);

      GROUP_PERMISSION.setId(GROUP);
      GROUP_PERMISSION.setRoles(Collections.singleton(Role.READ.toString()));
      PERMISSIONS.updateGroupPermissions(GROUP_PERMISSION);
   }

   private MorphiaOrganizationDao organizationDao;

   @Before
   public void initOrganizationDao() {
      organizationDao = new MorphiaOrganizationDao();
      organizationDao.setDatabase(database);
      organizationDao.setDatastore(datastore);

      organizationDao.ensureIndexes();
   }

   private MorphiaOrganization prepareOrganization(String code) {
      MorphiaOrganization organization = new MorphiaOrganization();
      organization.setCode(code);
      organization.setName(NAME);
      organization.setColor(COLOR);
      organization.setIcon(ICON);
      organization.setPermissions(new MorphiaPermissions(PERMISSIONS));
      return organization;
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = prepareOrganization(CODE1);

      String id = organizationDao.createOrganization(organization).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Organization storedOrganization = datastore.get(MorphiaOrganization.class, new ObjectId(id));
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
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE2);
      datastore.save(organization2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testGetOrganizationsNoReadRole() {
      Organization organization = prepareOrganization(CODE1);
      Permission userPermission = new MorphiaPermission(USER2, Collections.singleton(Role.CLONE.toString()));
      organization.getPermissions().updateUserPermissions(userPermission);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE2);
      Permission groupPermission = new MorphiaPermission(GROUP2, Collections.singleton(Role.SHARE.toString()));
      organization2.getPermissions().updateGroupPermissions(groupPermission);
      datastore.save(organization2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP2)).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).isEmpty();
   }

   @Test
   public void testGetOrganizationsGroupRole() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      Organization organization2 = prepareOrganization(CODE2);
      datastore.save(organization2);

      DatabaseQuery query = DatabaseQuery.createBuilder(USER2).groups(Collections.singleton(GROUP)).build();
      List<Organization> organizations = organizationDao.getOrganizations(query);
      assertThat(organizations).extracting(Organization::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testDeleteOrganization() {
      Organization organization = prepareOrganization(CODE1);
      datastore.save(organization);
      assertThat(organization.getId()).isNotNull();

      organizationDao.deleteOrganization(organization.getId());

      Organization storedOrganization = datastore.get(MorphiaOrganization.class, new ObjectId(organization.getId()));
      assertThat(storedOrganization).isNull();
   }

   @Test
   public void testDeleteOrganizationNotExisting() {
      assertThatThrownBy(() -> organizationDao.deleteOrganization(NOT_EXISTING_ID))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testUpdateOrganizationCode() {
      MorphiaOrganization organization = prepareOrganization(CODE1);
      String id = datastore.save(organization).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      MorphiaOrganization organization2 = prepareOrganization(CODE2);
      organizationDao.updateOrganization(id, organization2);

      MorphiaOrganization storedOrganization = datastore.get(MorphiaOrganization.class, new ObjectId(id));
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getCode()).isEqualTo(CODE2);
   }

   @Test
   public void testUpdateOrganizationPermissions() {
      MorphiaOrganization organization = prepareOrganization(CODE1);
      String id = datastore.save(organization).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      organization.getPermissions().removeUserPermission(USER);
      organization.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      organizationDao.updateOrganization(id, organization);

      MorphiaOrganization storedOrganization = datastore.get(MorphiaOrganization.class, new ObjectId(id));
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getPermissions().getUserPermissions()).isEmpty();
      assertThat(storedOrganization.getPermissions().getGroupPermissions()).containsExactly(GROUP_PERMISSION);
   }

   @Test
   public void testUpdateOrganizationExistingCode() {
      MorphiaOrganization organization = prepareOrganization(CODE1);
      datastore.save(organization);

      MorphiaOrganization organization2 = prepareOrganization(CODE2);
      datastore.save(organization2);

      organization2.setCode(CODE1);
      assertThatThrownBy(() -> organizationDao.updateOrganization(organization2.getId(), organization2))
            .isInstanceOf(DuplicateKeyException.class);
   }

}
