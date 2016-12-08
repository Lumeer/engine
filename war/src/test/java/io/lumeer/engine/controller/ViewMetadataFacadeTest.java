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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.ViewMetadataNotFoundException;

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
public class ViewMetadataFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "ViewMetadataFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   @Inject
   private ViewMetadataFacade viewMetadataFacade;

   @Inject
   private DataStorage dataStorage;

   private final String collection = viewMetadataFacade.VIEW_METADATA_COLLECTION_NAME;

   // do not change view names, because it can mess up viewId() method
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME_1 = "  Heľľo Woŕlď &-./ 1";
   private final String CREATE_INTERNAL_NAME_ORIGINAL_NAME_2 = "  Heľľo Worlď &-./ 1";
   private final String CREATE_INTERNAL_NAME_INTERNAL_NAME_1 = "view.helloworld__1_0";
   private final String CREATE_INTERNAL_NAME_INTERNAL_NAME_2 = "view.helloworld__1_1";

   private final String CREATE_INITIAL_METADATA_VIEW = "viewCreateInitialMetadata";
   private final String GET_METADATA_VIEW = "viewCreateInitialMetadata";
   private final String GET_METADATA_VALUE_VIEW = "viewGetMetadataValue";

   @Test
   public void testCreateInternalName() throws Exception {
      setUpCollection();
      Assert.assertEquals(viewMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME_1), CREATE_INTERNAL_NAME_INTERNAL_NAME_1);
      viewMetadataFacade.createInitialMetadata(CREATE_INTERNAL_NAME_ORIGINAL_NAME_1);
      Assert.assertEquals(viewMetadataFacade.createInternalName(CREATE_INTERNAL_NAME_ORIGINAL_NAME_2), CREATE_INTERNAL_NAME_INTERNAL_NAME_2);
   }

   @Test
   public void testCreateInitialMetadata() throws Exception {
      setUpCollection();
      viewMetadataFacade.createInitialMetadata(CREATE_INITIAL_METADATA_VIEW);
      List<DataDocument> list = dataStorage.search(collection, null, null, 0, 0);
      Map<String, Object> metadata = list.get(0);
      Assert.assertEquals(metadata.size(), 2);
      Assert.assertTrue(metadata.containsValue(CREATE_INITIAL_METADATA_VIEW));
      Assert.assertTrue(metadata.containsValue(viewId(CREATE_INITIAL_METADATA_VIEW)));
   }

   @Test
   public void testGetViewMetadata() throws Exception {
      setUpCollection();
      String viewId = viewId(GET_METADATA_VIEW);
      viewMetadataFacade.createInitialMetadata(GET_METADATA_VIEW);
      DataDocument metadata = viewMetadataFacade.getViewMetadata(viewId);
      Assert.assertTrue(metadata.containsValue(GET_METADATA_VIEW));
      Assert.assertTrue(metadata.containsValue(viewId));
   }

   @Test
   public void testSetGetViewMetadataValue() throws Exception {
      setUpCollection();
      String viewId = viewId(GET_METADATA_VALUE_VIEW);
      viewMetadataFacade.createInitialMetadata(GET_METADATA_VALUE_VIEW);

      String key = "key";
      String value = "value";

      boolean pass = false;
      try {
         viewMetadataFacade.getViewMetadataValue(viewId, key);
      } catch (ViewMetadataNotFoundException e) {
         pass = true;
      }
      Assert.assertTrue(pass);

      viewMetadataFacade.setViewMetadataValue(viewId, key, value);

      Assert.assertEquals(viewMetadataFacade.getViewMetadataValue(viewId, key), value);
   }

   private void setUpCollection() {
      // we have only one collection with metadata for all views,
      // so we have to drop collection before every test, because we
      // cannot drop it before whole test suite (inject in @BeforeClass does not work)
      dataStorage.dropCollection(collection);
      dataStorage.createCollection(collection);
   }

   private String viewId(String viewName) {
      return "view." + viewName.toLowerCase() + "_0";
   }

}
