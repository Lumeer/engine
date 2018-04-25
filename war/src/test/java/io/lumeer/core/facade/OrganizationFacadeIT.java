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

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class OrganizationFacadeIT extends IntegrationTestBase {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private UserDao userDao;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String STRANGER_USER = "stranger@nowhere.com";
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String CODE3 = "TORG3";
   private static final String NOT_EXISTING_CODE = "NOT_EXISTING_CODE";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private Permission userPermission;
   private Permission userReadonlyPermission;
   private Permission userStrangerPermission;
   private Permission groupPermission;

   private User user;
   private User strangerUser;

   @Before
   public void configure() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      User user2 = new User(STRANGER_USER);
      this.strangerUser = userDao.createUser(user2);

      userPermission = new SimplePermission(this.user.getId(), Organization.ROLES);
      userReadonlyPermission = new SimplePermission(this.user.getId(), Collections.singleton(Role.READ));
      userStrangerPermission = new SimplePermission(this.strangerUser.getId(), Collections.singleton(Role.MANAGE));
      groupPermission = new SimplePermission(GROUP, Collections.singleton(Role.READ));
   }

   @Test
   public void testGetOrganizations() {
      createOrganization(CODE1);
      createOrganization(CODE2);

      assertThat(organizationFacade.getOrganizations())
            .extracting(Resource::getCode).containsOnly(CODE1, CODE2);
   }

   private String createOrganization(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR, null, null);
      organization.getPermissions().updateUserPermissions(userPermission);
      organization.getPermissions().updateGroupPermissions(groupPermission);
      return organizationDao.createOrganization(organization).getId();
   }

   private void createOrganizationWithReadOnlyPermissions(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR, null, null);
      organization.getPermissions().updateUserPermissions(
            userReadonlyPermission,
            userStrangerPermission);
      organization.getPermissions().updateGroupPermissions(groupPermission);
      organizationDao.createOrganization(organization);
   }

   private void createOrganizationWithStrangerPermissions(final String code) {
      Organization organization = new JsonOrganization(code, NAME, ICON, COLOR, null, null);
      organization.getPermissions().updateUserPermissions(
            userPermission,
            userStrangerPermission);
      organization.getPermissions().updateGroupPermissions(groupPermission);
      organizationDao.createOrganization(organization);
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
      assertions.assertAll();

      assertPermissions(storedOrganization.getPermissions().getUserPermissions(), userPermission);
      assertPermissions(storedOrganization.getPermissions().getGroupPermissions(), this.groupPermission);
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
      Organization organization = new JsonOrganization(CODE1, NAME, ICON, COLOR, null, null);

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
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateOrganization() {
      String id = createOrganization(CODE1);

      Organization updatedOrganization = new JsonOrganization(CODE2, NAME, ICON, COLOR, null, null);

      organizationFacade.updateOrganization(CODE1, updatedOrganization);

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE2);

      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getId()).isEqualTo(id);
      assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testGetOrganizationPermissions() {
      createOrganization(CODE1);
      createOrganizationWithReadOnlyPermissions(CODE2);
      createOrganizationWithStrangerPermissions(CODE3);

      Permissions permissions = organizationFacade.getOrganizationPermissions(CODE1);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);

      permissions = organizationFacade.getOrganizationPermissions(CODE2);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userReadonlyPermission);

      permissions = organizationFacade.getOrganizationPermissions(CODE3);
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).hasSize(2).contains(userPermission, userStrangerPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      createOrganization(CODE1);

      SimplePermission userPermission = new SimplePermission(user.getId(), new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      organizationFacade.updateUserPermissions(CODE1, userPermission);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      createOrganization(CODE1);

      organizationFacade.removeUserPermission(CODE1, user.getId());

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createOrganization(CODE1);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      organizationFacade.updateGroupPermissions(CODE1, groupPermission);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createOrganization(CODE1);

      organizationFacade.removeGroupPermission(CODE1, GROUP);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

}
