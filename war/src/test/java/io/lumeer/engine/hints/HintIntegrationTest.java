/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.hints;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.storage.mongodb.MongoUtils;

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
   private HintFacade hintFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionFacade collectionFacade;

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
      DataDocument dataDocumentDatabase = dataStorage.readDocument(VALUE_DATABASE, dataStorageDialect.documentIdFilter(id));
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
