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
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.storage.mongodb.MongoDbStorageTestBase;
import io.lumeer.storage.mongodb.model.MongoOrganization;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.api.SoftAssertions;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;

public class MongoOrganizationDaoTest extends MongoDbStorageTestBase {

   private static final String USER = "testUser";

   private static final String CODE = "TPROJ";
   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";
   private static final MongoPermissions PERMISSIONS;

   static {
      MongoPermission userPermission = new MongoPermission();
      userPermission.setName(USER);
      userPermission.setRoles(Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet()));

      PERMISSIONS = new MongoPermissions();
      PERMISSIONS.updateUserPermissions(userPermission);
   }

   private MongoOrganizationDao organizationDao;

   @Before
   public void initOrganizationDao() {
      organizationDao = new MongoOrganizationDao();
      organizationDao.setDatabase(database);
      organizationDao.setDatastore(datastore);

      organizationDao.ensureIndexes();
   }

   private Organization prepareOrganization() {
      MongoOrganization organization = new MongoOrganization();
      organization.setCode(CODE);
      organization.setName(NAME);
      organization.setColor(COLOR);
      organization.setIcon(ICON);
      organization.setPermissions(PERMISSIONS);
      return organization;
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = prepareOrganization();

      String id = organizationDao.createOrganization(organization).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Organization storedOrganization = datastore.get(MongoOrganization.class, new ObjectId(id));
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getPermissions()).isEqualTo(PERMISSIONS);
      assertions.assertAll();
   }

   @Test
   public void testCreateOrganizationExistingCode() {
      Organization organization = prepareOrganization();
      datastore.save(organization);

      Organization organization2 = prepareOrganization();
      assertThatThrownBy(() -> organizationDao.createOrganization(organization2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testGetProjectByCode() {
      Organization organization = prepareOrganization();
      datastore.save(organization);

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE);
      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getId()).isNotNull().isNotEmpty();
      assertThat(storedOrganization.getCode()).isEqualTo(CODE);
      assertThat(storedOrganization.getName()).isEqualTo(NAME);
   }

   @Test
   public void testGetProjectByCodeNotExisting() {
      Organization organization = organizationDao.getOrganizationByCode("notExistingCode");
      assertThat(organization).isNull();
   }

}
