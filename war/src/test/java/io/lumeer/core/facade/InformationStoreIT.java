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

import io.lumeer.api.model.InformationRecord;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.InformationStoreDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class InformationStoreIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private Permission userPermission;
   private User user;
   private Organization organization;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private InformationStoreFacade informationStoreFacade;

   @Inject
   private InformationStoreDao informationStoreDao;

   @BeforeEach
   public void configureOrganization() {
      user = userDao.createUser(new User(USER));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.createOrganization(organization);

      user.setOrganizations(Collections.singleton(this.organization.getId()));
      user = userDao.updateUser(user.getId(), user);

      workspaceKeeper.setOrganization(this.organization);
      informationStoreDao.setOrganization(this.organization);

      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   @Test
   public void testBasicStore() {
      var rec = new InformationRecord(null, user.getId(), null, "", "", "Hello world!");

      rec = informationStoreFacade.addInformation(rec);

      var id = rec.getId();


      Assertions.assertNotNull(rec.getId());
      Assertions.assertNotNull(rec.getDate());
      Assertions.assertEquals(rec.getUserId(), user.getId());

      rec = informationStoreFacade.getInformation(rec.getId());

      Assertions.assertEquals(id, rec.getId());
      Assertions.assertEquals(user.getId(), rec.getUserId());
      Assertions.assertEquals("Hello world!", rec.getData());
   }

   @Test
   public void testStaleData() {
      var staleDate = ZonedDateTime.now().minus(1, ChronoUnit.DAYS);
      var staleRec = new InformationRecord(null, user.getId(), staleDate, "", "", "Hello world!");
      staleRec = informationStoreDao.addInformation(staleRec);
      final var staleRecId = staleRec.getId();

      var freshRec = informationStoreDao.addInformation(new InformationRecord(null, user.getId(), ZonedDateTime.now(), "", "", "I am here"));

      Assertions.assertNotNull(staleRec.getId());

      staleRec = informationStoreDao.findInformation(staleRec.getId(), user.getId());
      Assertions.assertEquals("Hello world!", staleRec.getData());
      Assertions.assertEquals(staleDate.toEpochSecond(), staleRec.getDate().toEpochSecond());

      // storing something else via facade should delete the stale record
      informationStoreFacade.addInformation(new InformationRecord(null, null, null, "", "", "Something else"));

      Assertions.assertThrows(StorageException.class, () -> informationStoreDao.findInformation(staleRecId, user.getId()));

      // while fresh data should be preserved
      freshRec = informationStoreDao.findInformation(freshRec.getId(), user.getId());
      Assertions.assertEquals("I am here", freshRec.getData());
   }
}
