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

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.dto.Config;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ConfigurationManipulatorIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Test
   public void testGetConfiguration() throws Exception {
      final String col = "configuration1";

      final String id1 = "org/proj/usr";
      final String id2 = "org/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value1");
      Config config12 = new Config("conf1", "value2");
      Config config13 = new Config("conf1", "value3");
      Config config21 = new Config("conf2", "value1");
      Config config22 = new Config("conf2", "value2");

      configurationManipulator.setConfiguration(col, id1, config11);
      configurationManipulator.setConfiguration(col, id2, config12);
      configurationManipulator.setConfiguration(col, id3, config13);
      configurationManipulator.setConfiguration(col, id1, config21);
      configurationManipulator.setConfiguration(col, id2, config22);

      assertThat(configurationManipulator.getConfiguration(col, id1, "conf1"))
            .isNotNull().extracting("value").containsOnly("value1");
      assertThat(configurationManipulator.getConfiguration(col, id2, "conf1"))
            .isNotNull().extracting("value").containsOnly("value2");
      assertThat(configurationManipulator.getConfiguration(col, id3, "conf1"))
            .isNotNull().extracting("value").containsOnly("value3");
      assertThat(configurationManipulator.getConfiguration(col, id1, "conf2"))
            .isNotNull().extracting("value").containsOnly("value1");
      assertThat(configurationManipulator.getConfiguration(col, id2, "conf2"))
            .isNotNull().extracting("value").containsOnly("value2");
      assertThat(configurationManipulator.getConfiguration(col, id3, "conf2"))
            .isNull();
   }

   @Test
   public void testGetConfigurations() throws Exception {
      final String col = "configuration11";

      final String id1 = "org/proj/usr";
      final String id2 = "org/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value1");
      Config config13 = new Config("conf1", "value3");
      Config config21 = new Config("conf2", "value1");
      Config config22 = new Config("conf2", "value2");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config11, config22), true);
      configurationManipulator.setConfigurations(col, id3, Arrays.asList(config13, config21), true);

      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value1", "value2");
      assertThat(configurationManipulator.getConfigurations(col, id2))
            .isEmpty();
      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value3", "value1");
   }

   @Test
   public void testSetConfiguration() throws Exception {
      final String col = "configuration21";

      final String id1 = "org/proj/usr";
      final String id2 = "org/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value1");
      Config config12 = new Config("conf1", "value2");
      Config config13 = new Config("conf1", "value3");
      Config config14 = new Config("conf1", "value4");

      configurationManipulator.setConfiguration(col, id1, config11);
      configurationManipulator.setConfiguration(col, id2, config12);
      configurationManipulator.setConfiguration(col, id3, config13);

      assertThat(configurationManipulator.getConfiguration(col, id1, "conf1"))
            .isNotNull().extracting("value").containsOnly("value1");
      assertThat(configurationManipulator.getConfiguration(col, id2, "conf1"))
            .isNotNull().extracting("value").containsOnly("value2");
      assertThat(configurationManipulator.getConfiguration(col, id3, "conf1"))
            .isNotNull().extracting("value").containsOnly("value3");

      configurationManipulator.setConfiguration(col, id1, config14);
      configurationManipulator.setConfiguration(col, id2, config13);
      configurationManipulator.setConfiguration(col, id3, config12);

      assertThat(configurationManipulator.getConfiguration(col, id1, "conf1"))
            .isNotNull().extracting("value").containsOnly("value4");
      assertThat(configurationManipulator.getConfiguration(col, id2, "conf1"))
            .isNotNull().extracting("value").containsOnly("value3");
      assertThat(configurationManipulator.getConfiguration(col, id3, "conf1"))
            .isNotNull().extracting("value").containsOnly("value2");
   }

   @Test
   public void testSetConfigurations() throws Exception {
      final String col = "configuration31";

      final String id1 = "org/proj/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value11");
      Config config13 = new Config("conf1", "value13");
      Config config14 = new Config("conf1", "value14");
      Config config21 = new Config("conf2", "value21");
      Config config22 = new Config("conf2", "value22");
      Config config31 = new Config("conf3", "value31");
      Config config32 = new Config("conf3", "value32");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config11, config21), false);
      configurationManipulator.setConfigurations(col, id3, Arrays.asList(config13, config22, config32), false);

      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value11", "value21");
      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value13", "value22", "value32");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config14, config31), false);
      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value14", "value31", "value21");

      configurationManipulator.setConfigurations(col, id3, Arrays.asList(config11, config31, config21), false);
      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value11", "value31", "value21");
   }

   @Test
   public void testSetConfigurationsAndReset() throws Exception {
      final String col = "configuration41";

      final String id1 = "org/proj/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value11");
      Config config13 = new Config("conf1", "value13");
      Config config14 = new Config("conf1", "value14");
      Config config21 = new Config("conf2", "value21");
      Config config22 = new Config("conf2", "value22");
      Config config31 = new Config("conf3", "value31");
      Config config32 = new Config("conf3", "value32");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config11, config21), true);
      configurationManipulator.setConfigurations(col, id3, Arrays.asList(config13, config22, config32), true);

      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value11", "value21");
      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value13", "value22", "value32");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config14, config31), true);
      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value14", "value31");

      configurationManipulator.setConfigurations(col, id3, Collections.singletonList(config11), true);
      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value11");
   }

   @Test
   public void testResetConfiguration() throws Exception {
      final String col = "configuration51";

      final String id1 = "org/proj/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value11");
      Config config12 = new Config("conf1", "value12");
      Config config21 = new Config("conf2", "value21");
      Config config31 = new Config("conf3", "value31");
      Config config32 = new Config("conf3", "value32");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config11, config21, config31), true);
      configurationManipulator.setConfigurations(col, id3, Arrays.asList(config12, config32), true);

      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value11", "value21", "value31");
      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value12", "value32");

      configurationManipulator.resetConfiguration(col, id1);
      assertThat(configurationManipulator.getConfigurations(col, id1)).isEmpty();
      assertThat(configurationManipulator.getConfigurations(col, id3)).isNotEmpty();

      configurationManipulator.resetConfiguration(col, id3);
      assertThat(configurationManipulator.getConfigurations(col, id3)).isEmpty();
   }

   @Test
   public void testResetConfigurationAttribute() throws Exception {
      final String col = "configuration61";

      final String id1 = "org/proj/usr";
      final String id3 = "usr";

      systemDataStorage.dropCollection(col);
      systemDataStorage.createCollection(col);

      Config config11 = new Config("conf1", "value11");
      Config config12 = new Config("conf1", "value12");
      Config config21 = new Config("conf2", "value21");
      Config config31 = new Config("conf3", "value31");
      Config config32 = new Config("conf3", "value32");

      configurationManipulator.setConfigurations(col, id1, Arrays.asList(config11, config21, config31), true);
      configurationManipulator.setConfigurations(col, id3, Arrays.asList(config12, config32), true);

      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value11", "value21", "value31");

      configurationManipulator.resetConfigurationAttribute(col, id1, "conf1");
      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value21", "value31");

      configurationManipulator.resetConfigurationAttribute(col, id1, "conf2");
      assertThat(configurationManipulator.getConfigurations(col, id1))
            .extracting("value").containsOnly("value31");

      configurationManipulator.resetConfigurationAttribute(col, id1, "conf3");
      assertThat(configurationManipulator.getConfigurations(col, id1)).isEmpty();


      assertThat(configurationManipulator.getConfigurations(col, id3))
            .extracting("value").containsOnly("value12", "value32");
      configurationManipulator.resetConfigurationAttribute(col, id3, "conf1");
      configurationManipulator.resetConfigurationAttribute(col, id3, "conf3");
      assertThat(configurationManipulator.getConfigurations(col, id3)).isEmpty();

   }

}
