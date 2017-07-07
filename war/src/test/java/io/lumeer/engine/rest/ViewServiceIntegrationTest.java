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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.ViewMetadata;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.DatabaseInitializer;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.ViewFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class ViewServiceIntegrationTest extends IntegrationTestBase {

   private static final String TARGET_URI = "http://localhost:8080";

   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/ACME/projects/default/views/";

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   @Before
   public void init() {
      // I (Alica) suppose we operate inside some default project which has not been initialized, so we do that here
      databaseInitializer.onProjectCreated(projectFacade.getCurrentProjectCode());
      PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/" + organizationFacade.getOrganizationCode() + "/projects/" + projectFacade.getCurrentProjectCode() + "/views/";
      dataStorage.dropManyDocuments(viewFacade.metadataCollection(), dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void testGetAllViews() throws Exception {
      // #1 No created views so far.
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.readEntity(new GenericType<List<ViewMetadata>>() {
      })).isEmpty();
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      response.close();
      client.close();

      // #2 There is one newly created view.
      final String initialViewName = "initialViewName";

      viewFacade.createView(initialViewName, LumeerConst.View.TYPE_DEFAULT_VALUE, null, null);

      final Client client2 = ClientBuilder.newBuilder().build();

      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<ViewMetadata> viewsByService = response2.readEntity(new GenericType<List<ViewMetadata>>() {
      });
      List<Integer> serviceIds = viewsByService.stream().map(ViewMetadata::getId).collect(Collectors.toList());

      List<ViewMetadata> viewsByFacade = viewFacade.getAllViews();
      Integer[] facadeIds = viewsByFacade.stream().map(ViewMetadata::getId).toArray(Integer[]::new);

      assertThat(serviceIds).containsOnly(facadeIds);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      response2.close();
      client2.close();
   }

   @Test
   public void testCreateAndUpdateView() throws Exception {
      // Checking that there are no saved views in the database so far.
      List<ViewMetadata> noViewsByFacade = viewFacade.getAllViews();
      assertThat(noViewsByFacade).isEmpty();

      // #1 Create simple view.
      final Client client = ClientBuilder.newBuilder().build();
      final int viewId = 1;
      final String viewName = "name";
      final String viewType = LumeerConst.View.TYPE_DEFAULT_VALUE;

      ViewMetadata view = createViewMetadata(viewId, viewName, viewType);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildPost(Entity.entity(view, MediaType.APPLICATION_JSON)).invoke();
      List<ViewMetadata> viewsByFacade = viewFacade.getAllViews();
      int responseViewId = response.readEntity(Integer.class);

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(viewsByFacade).hasSize(1);
      assertThat(viewsByFacade.get(0).getId()).isEqualTo(responseViewId);
      assertThat(viewsByFacade.get(0).getName()).isEqualTo(viewName);
      assertThat(viewsByFacade.get(0).getType()).isEqualTo(viewType);
      assertThat(viewsByFacade.get(0).getConfiguration()).isEmpty();

      response.close();
      client.close();

      // #2 The given view has already existed in the database. It returns Bad Request status code.
      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildPost(Entity.entity(view, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
      response2.close();
      client2.close();

      // #3 Update the given view.
      final Client client3 = ClientBuilder.newBuilder().build();
      final String updatedViewName = "updatedName";
      ViewMetadata updatedView = createViewMetadata(responseViewId, updatedViewName, LumeerConst.View.TYPE_DEFAULT_VALUE);
      Response response3 = client3.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(updatedView, MediaType.APPLICATION_JSON)).invoke();
      List<ViewMetadata> updatedViewsByFacade = viewFacade.getAllViews();

      assertThat(response3.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(updatedViewsByFacade).hasSize(1);
      assertThat(updatedViewsByFacade.get(0).getId()).isEqualTo(responseViewId);
      assertThat(updatedViewsByFacade.get(0).getName()).isEqualTo(updatedViewName);
      assertThat(updatedViewsByFacade.get(0).getType()).isEqualTo(viewType);
      assertThat(updatedViewsByFacade.get(0).getConfiguration()).isNull();

      response3.close();
      client3.close();
   }

   @Test
   public void testReadAndUpdateViewConfiguration() throws Exception {
      final String viewName = "name";
      final String viewType = LumeerConst.View.TYPE_DEFAULT_VALUE;
      final String confAttribute = "conf-attribute";
      final String confValue = "conf-value";
      final DataDocument viewConf = new DataDocument(confAttribute, confValue);

      // inserting the given view to database
      int viewIdByFacade = viewFacade.createView(viewName, viewType, null, viewConf);

      // #1 read view configuration
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + viewIdByFacade + "/configure/" + confAttribute).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      String responseConfValue = response.readEntity(String.class);

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(responseConfValue).isEqualTo(confValue);
      response.close();
      client.close();

      // #2 update view configuration
      final String updatedConfValue = "conf-updated-value";
      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + viewIdByFacade + "/configure/" + confAttribute).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(updatedConfValue, MediaType.APPLICATION_JSON)).invoke();
      String updatedConfValueByFacade = (String) viewFacade.getViewConfigurationAttribute(viewIdByFacade, confAttribute);

      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(updatedConfValueByFacade).isEqualTo(updatedConfValue);
      response2.close();
      client2.close();
   }

   @Test
   public void testCloneView() throws Exception {
      // creating new view
      final String viewName = "name";
      final String viewType = LumeerConst.View.TYPE_DEFAULT_VALUE;
      final int viewIdByFacade = viewFacade.createView(viewName, viewType, null, null);

      // creating a clone
      final String cloneName = "cloneName";
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + viewIdByFacade + "/clone/" + cloneName).request(MediaType.APPLICATION_JSON).buildPost(Entity.json(new DataDocument())).invoke();
      int viewIdByService = response.readEntity(Integer.class);

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(viewIdByService).isEqualTo(viewIdByFacade + 1);
      response.close();
      client.close();

      // checking all created views
      List<ViewMetadata> viewsByFacade = viewFacade.getAllViews();
      assertThat(viewsByFacade).hasSize(2);

      ViewMetadata origView = viewsByFacade.get(0);
      ViewMetadata cloneView = viewsByFacade.get(1);
      assertThat(origView.getName()).isEqualTo(viewName);
      assertThat(cloneView.getName()).isEqualTo(cloneName);
   }

   private void addManageRole(int view) {
      securityFacade.addViewUserRole(projectFacade.getCurrentProjectCode(), view, userFacade.getUserEmail(), LumeerConst.Security.ROLE_MANAGE);
   }

   private void addReadRole(int view) {
      securityFacade.addViewUserRole(projectFacade.getCurrentProjectCode(), view, userFacade.getUserEmail(), LumeerConst.Security.ROLE_READ);
   }

   private ViewMetadata createViewMetadata(int viewId, String viewName, String viewType) {
      DataDocument metadataDocument = new DataDocument(LumeerConst.View.NAME_KEY, viewName)
            .append(LumeerConst.View.ID_KEY, viewId)
            .append(LumeerConst.View.CREATE_USER_KEY, userFacade.getUserEmail())
            .append(LumeerConst.View.CREATE_DATE_KEY, new Date())
            .append(LumeerConst.View.TYPE_KEY, viewType);
      return new ViewMetadata(metadataDocument);
   }
}
