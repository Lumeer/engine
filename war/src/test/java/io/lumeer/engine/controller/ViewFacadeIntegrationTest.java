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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;
import io.lumeer.engine.provider.DataStorageProvider;
import io.lumeer.engine.rest.dao.ViewDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class ViewFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private ViewFacade viewFacade;

   private DataStorage dataStorage;

   @Inject
   private UserFacade userFacade;

   private final String collection = LumeerConst.View.VIEW_METADATA_COLLECTION_NAME;

   private final String CREATE_INITIAL_METADATA_VIEW = "viewCreateInitialMetadata";
   private final String VIEW_TO_BE_COPIED = "viewToBeCopied";
   private final String COPIED_VIEW = "viewCopied";
   private final String SET_GET_TYPE_VIEW = "viewSetGetType";
   private final String SET_GET_NAME_VIEW = "viewSetGetName";
   private final String SET_GET_NAME_VIEW_2 = "viewSetGetName2";
   private final String SET_GET_CONFIGURATION_VIEW = "viewSetGetConfiguration";
   private final String SET_GET_CONFIGURATION_ATTRIBUTE_VIEW = "viewSetGetConfigurationAttribute";
   private final String GET_METADATA_VIEW = "viewGetMetadata";
   private final String GET_ALL_VIEWS_VIEW_1 = "viewGetAllViews1";
   private final String GET_ALL_VIEWS_VIEW_2 = "viewGetAllViews2";
   private final String GET_ALL_VIEWS_OF_TYPE_VIEW_1 = "viewGetAllViewsOfType1";
   private final String GET_ALL_VIEWS_OF_TYPE_VIEW_2 = "viewGetAllViewsOfType2";

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Before
   public void init() {
      dataStorage = dataStorageProvider.getUserStorage();
   }

   @Test
   public void testCreateView() throws Exception {
      setUpCollection();
      int viewId = viewFacade.createView(CREATE_INITIAL_METADATA_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      List<DataDocument> list = dataStorage.search(collection, null, null, 0, 0);
      Map<String, Object> metadata = null;

      for (DataDocument d : list) { // we find our view in the list
         if (d.getString(LumeerConst.View.VIEW_NAME_KEY).equals(CREATE_INITIAL_METADATA_VIEW)) {
            metadata = d;
         }
      }

      assertThat(metadata).containsValue(CREATE_INITIAL_METADATA_VIEW);
      assertThat(metadata).containsValue(viewId);
      assertThat(metadata).containsValue(getCurrentUser());
      assertThat(metadata).containsKey(LumeerConst.View.VIEW_CREATE_DATE_KEY);
      assertThat(metadata).containsKey(LumeerConst.View.VIEW_TYPE_KEY);
   }

   @Test
   public void testCopyView() throws Exception {
      setUpCollection();
      int view = viewFacade.createView(VIEW_TO_BE_COPIED, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      // we try to copy view with already existing name
      boolean pass = false;
      try {
         viewFacade.copyView(view, VIEW_TO_BE_COPIED);
      } catch (ViewAlreadyExistsException e) {
         pass = true;
      }
      assertThat(pass).isTrue();

      int copy = viewFacade.copyView(view, COPIED_VIEW);

      assertThat(view).isNotEqualTo(copy); // copy should have a new id
      assertThat(viewFacade.getViewName(copy)).isEqualTo(COPIED_VIEW); // copy should have a new name
      assertThat(viewFacade.getViewMetadataValue(view, LumeerConst.View.VIEW_CREATE_DATE_KEY))
            .isNotEqualTo(viewFacade.getViewMetadataValue(copy, LumeerConst.View.VIEW_CREATE_DATE_KEY)); // create date should be new for copy
   }

   @Test
   public void testSetGetViewType() throws Exception {
      setUpCollection();

      int view = viewFacade.createView(SET_GET_TYPE_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      assertThat(viewFacade.getViewType(view)).isEqualTo(LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE);

      String newType = "new type";
      viewFacade.setViewType(view, newType);
      assertThat(viewFacade.getViewType(view)).isEqualTo(newType);
   }

   @Test
   public void testSetGetViewName() throws Exception {
      setUpCollection();

      int view = viewFacade.createView(SET_GET_NAME_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      assertThat(viewFacade.getViewName(view)).isEqualTo(SET_GET_NAME_VIEW);

      String newName = "new name";
      viewFacade.setViewName(view, newName);
      assertThat(viewFacade.getViewName(view)).isEqualTo(newName);

      // we create one more view
      viewFacade.createView(SET_GET_NAME_VIEW_2, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      // we try to rename our first view to already existing name (second view)
      boolean pass = false;
      try {
         viewFacade.setViewName(view, SET_GET_NAME_VIEW_2);
      } catch (ViewAlreadyExistsException e) {
         pass = true;
      }
      assertThat(pass).isTrue();
   }

   @Test
   public void testSetGetViewConfiguration() throws Exception {
      setUpCollection();
      int view = viewFacade.createView(SET_GET_CONFIGURATION_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      // we try to get non existing configuration
      DataDocument configuration = viewFacade.getViewConfiguration(view);
      assertThat(configuration).isEmpty();

      String attribute = "configuration attribute";
      String value = "configuration value";
      viewFacade.setViewConfiguration(view, new DataDocument(attribute, value));

      configuration = viewFacade.getViewConfiguration(view);

      assertThat(configuration).containsKey(attribute);
      assertThat(configuration).containsValue(value);
   }

   @Test
   public void testSetGetViewConfigurationAttribute() throws Exception {
      setUpCollection();
      int view = viewFacade.createView(SET_GET_CONFIGURATION_ATTRIBUTE_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      viewFacade.setViewConfiguration(view, new DataDocument("intro key", "intro value"));
      String attribute = "configuration attribute";
      String value = "configuration value";

      viewFacade.setViewConfigurationAttribute(view, attribute, value);

      // we try to get non existing attribute from configuration
      boolean pass = false;
      try {
         viewFacade.getViewConfigurationAttribute(view, "non existing");
      } catch (ViewMetadataNotFoundException e) {
         pass = true;
      }
      assertThat(pass).isTrue();

      String confValue = (String) viewFacade.getViewConfigurationAttribute(view, attribute);
      assertThat(value).isEqualTo(confValue);
   }

   @Test
   public void testGetViewMetadata() throws Exception {
      setUpCollection();
      int viewId = viewFacade.createView(GET_METADATA_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      DataDocument metadata = viewFacade.getViewMetadata(viewId);
      assertThat(metadata).containsValue(GET_METADATA_VIEW);
      assertThat(metadata).containsValue(viewId);
   }

   @Test
   public void testGetAllViews() throws Exception {
      setUpCollection();
      int viewId1 = viewFacade.createView(GET_ALL_VIEWS_VIEW_1, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      int viewId2 = viewFacade.createView(GET_ALL_VIEWS_VIEW_2, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      List<ViewDao> views = viewFacade.getAllViews();
      assertThat(views).hasSize(2);
      assertThat(views.get(0).getId()).isIn(viewId1, viewId2);
   }

   @Test
   public void testGetAllViewsOfType() throws Exception {
      setUpCollection();
      int viewId1 = viewFacade.createView(GET_ALL_VIEWS_OF_TYPE_VIEW_1, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      viewFacade.createView(GET_ALL_VIEWS_OF_TYPE_VIEW_2, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      String type = "type";
      viewFacade.setViewType(viewId1, type);

      List<ViewDao> views = viewFacade.getAllViewsOfType(type);
      assertThat(views).hasSize(1);
      assertThat(views.get(0).getId()).isEqualTo(viewId1);

      views = viewFacade.getAllViewsOfType("non existing type");
      assertThat(views).isEmpty();
   }

   private void setUpCollection() {
      // we have only one collection with metadata for all views,
      // so we have to drop collection before every test, because we
      // cannot drop it before whole test suite (inject in @BeforeClass does not work)
      dataStorage.dropCollection(collection);
   }

   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }

}
