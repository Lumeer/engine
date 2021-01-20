/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
package io.lumeer.core.util.js;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.List;

public class JvmObjectProxyTest {

   private Context context = null;
   private Value fce;
   private String jsCode;
   private Engine engine = Engine
         .newBuilder()
         .allowExperimentalOptions(true)
         .option("js.experimental-foreign-object-prototype", "true")
         .option("js.foreign-object-prototype", "true")
         .build();

   private void initContext() {
      if (context == null) {

         jsCode = "function fce(documents) { return documents[0].creationDate.minute }";

         context = Context
               .newBuilder("js")
               .engine(engine)
               .allowAllAccess(true)
               .build();
         context.initialize("js");

         var result = context.eval("js", jsCode);

         fce = context.getBindings("js").getMember("fce");
      }
   }

   @Test
   public void test() throws JsonProcessingException {
      var l = List.of("user1@lumeerio.com", "user2@lumeerio.com");

      Document d = new Document(new DataDocument("useři", l).append("další", new String[]{ "user1" , "user2" }));
      d.setId("abc123");
      d.setFavorite(true);
      d.setCollectionId("myCollId");
      d.setCreationDate(ZonedDateTime.now());

      initContext();
      var res = fce.execute(List.of(new JvmObjectProxy<>(d, Document.class)));

      assertThat(res.asInt()).isEqualTo(d.getCreationDate().getMinute());

      context.close();
   }
}