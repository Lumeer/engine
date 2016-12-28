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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.ViewAlreadyExistsException;
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;
import io.lumeer.engine.rest.dao.ViewDao;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class ViewFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "ViewFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   @Inject
   private ViewFacade viewFacade;

   @Inject
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

      Assert.assertTrue(metadata.containsValue(CREATE_INITIAL_METADATA_VIEW));
      Assert.assertTrue(metadata.containsValue(viewId));
      Assert.assertTrue(metadata.containsValue(getCurrentUser()));
      Assert.assertTrue(metadata.containsKey(LumeerConst.View.VIEW_CREATE_DATE_KEY));
      Assert.assertTrue(metadata.containsKey(LumeerConst.View.VIEW_TYPE_KEY));
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
      Assert.assertTrue(pass);

      int copy = viewFacade.copyView(view, COPIED_VIEW);

      Assert.assertNotEquals(view, copy); // copy should have a new id
      Assert.assertEquals(viewFacade.getViewName(copy), COPIED_VIEW); // copy should have a new name
      Assert.assertNotEquals(
            viewFacade.getViewMetadataValue(view, LumeerConst.View.VIEW_CREATE_DATE_KEY),
            viewFacade.getViewMetadataValue(copy, LumeerConst.View.VIEW_CREATE_DATE_KEY)); // create date should be new for copy
   }

   @Test
   public void testSetGetViewType() throws Exception {
      setUpCollection();

      int view = viewFacade.createView(SET_GET_TYPE_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      Assert.assertEquals(viewFacade.getViewType(view), LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE);

      String newType = "new type";
      viewFacade.setViewType(view, newType);
      Assert.assertEquals(viewFacade.getViewType(view), newType);
   }

   @Test
   public void testSetGetViewName() throws Exception {
      setUpCollection();

      int view = viewFacade.createView(SET_GET_NAME_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      Assert.assertEquals(viewFacade.getViewName(view), SET_GET_NAME_VIEW);

      String newName = "new name";
      viewFacade.setViewName(view, newName);
      Assert.assertEquals(viewFacade.getViewName(view), newName);

      // we create one more view
      viewFacade.createView(SET_GET_NAME_VIEW_2, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      // we try to rename our first view to already existing name (second view)
      boolean pass = false;
      try {
         viewFacade.setViewName(view, SET_GET_NAME_VIEW_2);
      } catch (ViewAlreadyExistsException e) {
         pass = true;
      }
      Assert.assertTrue(pass);
   }

   @Test
   public void testSetGetViewConfiguration() throws Exception {
      setUpCollection();
      int view = viewFacade.createView(SET_GET_CONFIGURATION_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      // we try to get non existing configuration
      DataDocument configuration = viewFacade.getViewConfiguration(view);
      Assert.assertTrue(configuration.isEmpty());

      String attribute = "configuration attribute";
      String value = "configuration value";
      viewFacade.setViewConfiguration(view, new DataDocument(attribute, value));

      configuration = viewFacade.getViewConfiguration(view);

      Assert.assertTrue(configuration.containsKey(attribute));
      Assert.assertTrue(configuration.containsValue(value));
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
      Assert.assertTrue(pass);

      String confValue = (String) viewFacade.getViewConfigurationAttribute(view, attribute);
      Assert.assertEquals(value, confValue);
   }

   @Test
   public void testGetViewMetadata() throws Exception {
      setUpCollection();
      int viewId = viewFacade.createView(GET_METADATA_VIEW, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      DataDocument metadata = viewFacade.getViewMetadata(viewId);
      Assert.assertTrue(metadata.containsValue(GET_METADATA_VIEW));
      Assert.assertTrue(metadata.containsValue(viewId));
   }

   @Test
   public void testGetAllViews() throws Exception {
      setUpCollection();
      int viewId1 = viewFacade.createView(GET_ALL_VIEWS_VIEW_1, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      int viewId2 = viewFacade.createView(GET_ALL_VIEWS_VIEW_2, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      List<ViewDao> views = viewFacade.getAllViews();
      Assert.assertEquals(views.size(), 2);
      Assert.assertTrue(views.get(0).getId() == viewId1 || views.get(0).getId() == viewId2);
   }

   @Test
   public void testGetAllViewsOfType() throws Exception {
      setUpCollection();
      int viewId1 = viewFacade.createView(GET_ALL_VIEWS_OF_TYPE_VIEW_1, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);
      int viewId2 = viewFacade.createView(GET_ALL_VIEWS_OF_TYPE_VIEW_2, LumeerConst.View.VIEW_TYPE_DEFAULT_VALUE, null);

      String type = "type";
      viewFacade.setViewType(viewId1, type);

      List<ViewDao> views = viewFacade.getAllViewsOfType(type);
      Assert.assertEquals(views.size(), 1);
      Assert.assertEquals(views.get(0).getId(), viewId1);

      views = viewFacade.getAllViewsOfType("non existing type");
      Assert.assertEquals(views.size(), 0);
   }

   private void setUpCollection() {
      // we have only one collection with metadata for all views,
      // so we have to drop collection before every test, because we
      // cannot drop it before whole test suite (inject in @BeforeClass does not work)
      dataStorage.dropCollection(collection);
      dataStorage.createCollection(collection);
   }

   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }

}
