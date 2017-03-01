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
package io.lumeer.engine.hints;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.mongodb.MongoUtils;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com>Jan Kotrady</a>
 */
@RunWith(Arquillian.class)
public class HintIntegrationTest extends IntegrationTestBase {

   private String VALUE_DATABASE = "hintTestValue";

   @Inject
   public HintFacade hintFacade;
   @Inject
   public DataStorage dataStorage;
   @Inject
   public CollectionFacade collectionFacade;

   @Test
   public void testNormalFormHintDocument() throws Exception {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put("pes", "macka a pes");
      hintFacade.runHint("NormalForm", dataDocument);
      try {
         Thread.sleep(1000);
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
      String response = hintFacade.getHintText();
      System.out.println("====================");
      System.out.println(response);
      assertThat(response).isEqualTo("You have space in: pes");
   }

   @Test
   public void testValueType() throws Exception {
      if (dataStorage.hasCollection(VALUE_DATABASE)) {
         dataStorage.dropCollection(VALUE_DATABASE);
      }
      dataStorage.createCollection(VALUE_DATABASE);

      DataDocument dataDocument = new DataDocument();
      dataDocument.put("number", 10);
      dataDocument.put("numberString", "9");
      dataDocument.put("uvodzovky", "\"");
      String id = dataStorage.createDocument(VALUE_DATABASE, dataDocument);
      DataDocument dataDocumentDatabase = dataStorage.readDocument(VALUE_DATABASE, id);
      System.out.println("=======================");
      System.out.println(MongoUtils.dataDocumentToDocument(dataDocument).toJson().toString());
      hintFacade.runHint("ValueTypeHint", dataDocumentDatabase);
      try {
         Thread.sleep(1000);
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
      }
      String response = hintFacade.getHintText();
      System.out.println("====================");
      System.out.println(response);
      assertThat(response).isEqualTo("You have wrong integer saved in: numberString");
   }

}
