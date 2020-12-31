
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
package io.lumeer.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.ResourceType;

import org.junit.Test;

import java.util.Set;

public class FunctionRuleJsParserTest {

   private static final String js = "var lumeer = Polyglot.import('lumeer');\n"
         + "  lumeer.setDocumentAttribute(newRecord, 'a1', lumeer.getDocumentAttribute(newRecord, 'a1'));\n"
         + "  aaa = lumeer.createDocument('5f05d2b65a38bf4801acc395');\n"
         + "  lumeer.showMessage('SUCCESS', 'Jede to')var i_list = lumeer.getLinkedDocuments(aaa, '5f8a243e187f962161f1745b');\n"
         + "  for (var i_index in i_list) {\n"
         + "    i = i_list[i_index];\n"
         + "    lumeer.setDocumentAttribute(i, 'a8', lumeer.getDocumentAttribute(i, 'a1'));\n"
         + "  }\n"
         + "  lumeer.setDocumentAttribute(aaa, 'a1', lumeer.getDocumentAttribute(lumeer.getLinkedDocuments(newRecord, '5fed2ba561fb9225cc4b0a20'), 'a2')[0]);\n"
         + "  var j_list = lumeer.getLinks(aaa, '5f8a243e187f962161f1745b');\n"
         + "  for (var j_index in j_list) {\n"
         + "    j = j_list[j_index];\n"
         + "    lumeer.setLinkAttribute(j, '', lumeer.getLinkAttribute(lumeer.getLinks(newRecord, '5fed2ba561fb9225cc4b0a20'), '')[0]);\n"
         + "  }";

   @Test
   public void functionRuleParserTest() {
      var list = FunctionRuleJsParser.parseRuleFunctionJs(js,
            Set.of("5f05d2b65a38bf4801acc395"),
            Set.of("5f8a243e187f962161f1745b", "5fed2ba561fb9225cc4b0a20")
      );

      assertThat(list).containsExactly(
            new FunctionRuleJsParser.ResourceReference(ResourceType.COLLECTION, "5f05d2b65a38bf4801acc395"),
            new FunctionRuleJsParser.ResourceReference(ResourceType.LINK, "5f8a243e187f962161f1745b"),
            new FunctionRuleJsParser.ResourceReference(ResourceType.LINK, "5fed2ba561fb9225cc4b0a20")
      );
   }
}