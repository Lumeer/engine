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
package io.lumeer.engine.controller.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.controller.UserRoleFacade;
import io.lumeer.engine.provider.DataStorageProvider;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Tests for UserRoleFacade
 */
public class UserRoleFacadeIntegrationTest {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Inject
   private UserRoleFacade userRoleFacade;

   @Test
   public void projectUserRolesTest() throws Exception {
      systemDataStorage.dropManyDocuments(LumeerConst.Project.UserRoles.COLLECTION_NAME, "{}");

      final String organizationId = "someOrganization";
      final String p1 = "project1";
      final String p2 = "project2";

      final String u1 = "userRole1";
      final String u2 = "userRole2";
      final String u3 = "userRole3";

      final String c1 = "coreRole1";
      final String c2 = "coreRole2";
      final String c3 = "coreRole3";

      Map<String, List<String>> r1 = userRoleFacade.readRoles(organizationId, p1);
      Map<String, List<String>> r2 = userRoleFacade.readRoles(organizationId, p2);
      assertThat(r1).isEmpty();
      assertThat(r2).isEmpty();

      // create test
      userRoleFacade.createRole(organizationId, p1, u1, Arrays.asList(c1, c2));
      userRoleFacade.createRole(organizationId, p1, u2, Arrays.asList(c2, c3));
      userRoleFacade.createRole(organizationId, p2, u2, Arrays.asList(c2, c3));
      userRoleFacade.createRole(organizationId, p2, u3, Collections.singletonList(c3));
      r1 = userRoleFacade.readRoles(organizationId, p1);
      r2 = userRoleFacade.readRoles(organizationId, p2);
      assertThat(r1).containsOnlyKeys(u1, u2);
      assertThat(r1.get(u1)).containsExactly(c1, c2);
      assertThat(r1.get(u2)).containsExactly(c2, c3);
      assertThat(r2).containsOnlyKeys(u2, u3);
      assertThat(r2.get(u2)).containsExactly(c2, c3);
      assertThat(r2.get(u3)).containsExactly(c3);

      // drop test
      userRoleFacade.dropRole(organizationId, p1, u2);
      userRoleFacade.dropRole(organizationId, p2, u2);
      r1 = userRoleFacade.readRoles(organizationId, p1);
      r2 = userRoleFacade.readRoles(organizationId, p2);
      assertThat(r1).containsOnlyKeys(u1);
      assertThat(r2).containsOnlyKeys(u3);

      // add and delete core roles test
      userRoleFacade.addCoreRolesToRole(organizationId, p1, u1, Collections.singletonList(c3));
      r1 = userRoleFacade.readRoles(organizationId, p1);
      assertThat(r1.get(u1)).containsExactly(c1, c2, c3);
      userRoleFacade.removeCoreRolesFromRole(organizationId, p1, u1, Arrays.asList(c1, c2));
      r1 = userRoleFacade.readRoles(organizationId, p1);
      assertThat(r1.get(u1)).containsExactly(c3);
   }

}
