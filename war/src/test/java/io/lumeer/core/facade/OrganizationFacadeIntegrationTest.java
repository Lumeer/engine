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
package io.lumeer.core.facade;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class OrganizationFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private OrganizationFacade organizationFacade;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String NOT_EXISTING_CODE = "NOT_EXISTING_CODE";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Permission USER_PERMISSION;
   private static final Permission GROUP_PERMISSION;

   static {
      USER_PERMISSION = new SimplePermission(USER, Organization.ROLES);
      GROUP_PERMISSION = new SimplePermission(GROUP, Collections.singleton(Role.READ));
   }

   @Test
   public void testGetOrganizations() {
      createOrganization(CODE1);
      createOrganization(CODE2);

      assertThat(organizationFacade.getOrganizations())
            .extracting(Resource::getCode).containsOnly(CODE1, CODE2);
   }

   private String createOrganization(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR, null);
      organization.getPermissions().updateUserPermissions(USER_PERMISSION);
      organization.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return organizationDao.createOrganization(organization).getId();
   }

   @Test
   public void testGetOrganization() {
      createOrganization(CODE1);

      Organization storedOrganization = organizationFacade.getOrganization(CODE1);
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      assertPermissions(storedOrganization.getPermissions().getUserPermissions(), USER_PERMISSION);
   }

   @Test
   public void testGetOrganizationNotExisting() {
      assertThatThrownBy(() -> organizationDao.getOrganizationByCode(NOT_EXISTING_CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testDeleteOrganization() {
      createOrganization(CODE1);
      organizationFacade.deleteOrganization(CODE1);

      assertThatThrownBy(() -> organizationDao.getOrganizationByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = new JsonOrganization(CODE1, NAME, ICON, COLOR, null);

      Organization createdOrganization = organizationFacade.createOrganization(organization);
      assertThat(createdOrganization).isNotNull();
      assertThat(createdOrganization.getId()).isNotNull();

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE1);
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateOrganization() {
      String id = createOrganization(CODE1);

      Organization updatedOrganization = new JsonOrganization(CODE2, NAME, ICON, COLOR, null);

      organizationFacade.updateOrganization(CODE1, updatedOrganization);

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE2);

      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getId()).isEqualTo(id);
      assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
   }

   @Test
   public void testGetOrganizationPermissions() {
      createOrganization(CODE1);

      Permissions permissions = organizationFacade.getOrganizationPermissions(CODE1);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createOrganization(CODE1);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      organizationFacade.updateUserPermissions(CODE1, userPermission);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createOrganization(CODE1);

      organizationFacade.removeUserPermission(CODE1, USER);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createOrganization(CODE1);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      organizationFacade.updateGroupPermissions(CODE1, groupPermission);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createOrganization(CODE1);

      organizationFacade.removeGroupPermission(CODE1, GROUP);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

}
