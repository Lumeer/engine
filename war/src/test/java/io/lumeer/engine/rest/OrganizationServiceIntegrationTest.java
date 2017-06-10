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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class OrganizationServiceIntegrationTest extends IntegrationTestBase {

   private final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/";

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Before
   public void init() {
      dataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(organizationFacade).isNotNull();
   }

   @Test
   public void testGetOrganizations() throws Exception {
      String org1 = "GetOrganizations1";
      String org2 = "GetOrganizations2";
      String code1 = "GetOrganizations1_id";
      String code2 = "GetOrganizations2_id";
      organizationFacade.createOrganization(new Organization(code1, org1));
      organizationFacade.createOrganization(new Organization(code2, org2));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();

      List<Organization> organizations = response.readEntity(new GenericType<List<Organization>>(List.class) {
      });

      assertThat(organizations).extracting("code").contains(code1, code2);
      assertThat(organizations).extracting("name").contains(org1, org2);
   }

   @Test
   public void testReadOrganization() throws Exception {
      String org = "ReadOrganization";
      String code = "ReadOrganization_id";
      organizationFacade.createOrganization(new Organization(code, org));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(PATH_PREFIX + code)
            .request(MediaType.APPLICATION_JSON)
            .buildGet()
            .invoke();

      Organization organization = response.readEntity(Organization.class);
      assertThat(organization).isNotNull();
   }

   @Test
   public void testUpdateOrganization() throws Exception {
      String org = "UpdateOrganization";
      String code = "UpdateOrganization_id";
      String newOrgName = "UpdateOrganizationNewName";
      organizationFacade.createOrganization(new Organization(code, org));

      assertThat(organizationFacade.readOrganization(code).getName()).isNotEqualTo(newOrgName);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + code)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.json(new Organization(code, newOrgName)))
            .invoke();

      assertThat(organizationFacade.readOrganization(code).getName()).isEqualTo(newOrgName);
   }

   @Test
   public void testGetOrganizationName() throws Exception {
      String org = "GetOrganizationName";
      String code = "GetOrganizationName_id";
      organizationFacade.createOrganization(new Organization(code, org));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/name")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();

      String nameFromResponse = response.readEntity(String.class);

      assertThat(nameFromResponse).isEqualTo(org);
   }

   @Test
   public void testUpdateOrganizationCode() throws Exception {
      String org = "UpdateOrganizationId";
      String code = "UpdateOrganizationId_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      String newId = "UpdateOrganizationId_newId";
      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/code/" + newId)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      String name = organizationFacade.readOrganizationName(newId);

      assertThat(name).isEqualTo(org);
   }

   @Test
   public void testCreateOrganization() throws Exception {
      String org = "CreateOrganization";
      String code = "CreateOrganization_id";

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX)
            .request()
            .buildPost(Entity.json(new Organization(code, org)))
            .invoke();

      List<Organization> organizations = organizationFacade.readOrganizations();
      assertThat(organizations).extracting("code").contains(code);
      assertThat(organizations).extracting("name").contains(org);
   }

   @Test
   public void testRenameOrganization() throws Exception {
      String org = "RenameOrganization";
      String newName = "RenameOrganizationNew";
      String code = "RenameOrganization_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/name/" + newName)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      List<Organization> organizations = organizationFacade.readOrganizations();
      assertThat(organizations).extracting("code").contains(code);
      assertThat(organizations).extracting("name").contains(newName);
   }

   @Test
   public void testDropOrganization() throws Exception {
      String org = "DropOrganization";
      String code = "DropOrganization_id";
      organizationFacade.createOrganization(new Organization(code, org));

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();

      List<Organization> organizations = organizationFacade.readOrganizations();
      assertThat(organizations).extracting("code").doesNotContain(code);
      assertThat(organizations).extracting("code").doesNotContain(org);
   }

   @Test
   public void testReadMetadata() throws Exception {
      String org = "ReadMetadata";
      String code = "ReadMetadata_id";
      organizationFacade.createOrganization(new Organization(code, org));

      String name = "attribute";

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + name)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      String metaValue = response.readEntity(String.class);
      assertThat(metaValue).isNull();

      String value = "value";
      organizationFacade.updateOrganizationMetadata(code, name, value);

      response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + name)
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();
      metaValue = response.readEntity(String.class);
      assertThat(metaValue).isEqualTo(value);
   }

   @Test
   public void testUpdateMetadata() throws Exception {
      String org = "UpdateMetadata";
      String code = "UpdateMetadata_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      String name = "attribute";
      String value = "value";

      // add new value
      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(value, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationMetadata(code, name)).isEqualTo(value);

      String newValue = "new value";

      // update existing value
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(newValue, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationMetadata(code, name)).isEqualTo(newValue);
   }

   @Test
   public void testDropMetadata() throws Exception {
      String org = "DropMetadata";
      String code = "DropMetadata_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      String name = "attribute";
      String value = "value";
      organizationFacade.updateOrganizationMetadata(code, name, value);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(organizationFacade.readOrganizationMetadata(code, name)).isNull();
   }

   @Test
   public void testReadInfo() throws Exception {
      String org = "ReadInfo";
      String code = "ReadInfo_id";
      organizationFacade.createOrganization(new Organization(code, org));

      String name = "attribute";

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/data/" + name)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      String dataValue = response.readEntity(String.class);
      assertThat(dataValue).isNull();

      String value = "value";
      organizationFacade.updateOrganizationInfoData(code, name, value);

      response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/data/" + name)
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();
      dataValue = response.readEntity(String.class);
      assertThat(dataValue).isEqualTo(value);
   }

   @Test
   public void testReadInfoDocument() throws Exception {
      String org = "ReadInfoDoc";
      String code = "ReadInfoDoc_id";
      organizationFacade.createOrganization(new Organization(code, org));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/data")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      DataDocument dataValue = response.readEntity(DataDocument.class);
      assertThat(dataValue).isNull();

      String name1 = "attribute1";
      String name2 = "attribute2";
      String value1 = "value1";
      String value2 = "value2";
      organizationFacade.updateOrganizationInfoData(code, new DataDocument().append(name1, value1).append(name2, value2));

      response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/data")
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();
      dataValue = response.readEntity(DataDocument.class);
      assertThat(dataValue).containsEntry(name1, value1);
      assertThat(dataValue).containsEntry(name2, value2);
   }

   @Test
   public void testUpdateInfo() throws Exception {
      String org = "UpdateInfo";
      String code = "UpdateInfo_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      String name = "attribute";
      String value = "value";

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/data/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(value, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationInfoData(code, name)).isEqualTo(value);

      String newValue = "new value";
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/data/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(newValue, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationInfoData(code, name)).isEqualTo(newValue);
   }

   @Test
   public void testDropInfo() throws Exception {
      String org = "DropInfo";
      String code = "DropInfo_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      String name = "attribute";
      String value = "value";
      organizationFacade.updateOrganizationInfoData(code, name, value);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/data/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(organizationFacade.readOrganizationInfoData(code, name)).isNull();
   }

   @Test
   public void testResetInfo() throws Exception {
      String org = "ResetInfo";
      String code = "ResetInfo_id";
      organizationFacade.createOrganization(new Organization(code, org));

      addManageRole(id);

      String name = "attribute";
      String value = "value";
      organizationFacade.updateOrganizationInfoData(code, name, value);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + code + "/data/")
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();
      assertThat(organizationFacade.readOrganizationInfoData(code)).isEmpty();
   }

   private void addManageRole(String organizationCode) {
      securityFacade.addOrganizationUserRole(organizationCode, userFacade.getUserEmail(), LumeerConst.Security.ROLE_MANAGE);
   }

}
