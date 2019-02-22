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

package io.lumeer.core.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import java.util.List;

public class FunctionXmlParserTest {

   private final static String xml = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         + "  <variables>\n"
         + "    <variable type=\"5c5b3f08b9437f682e35d3b7_document\" id=\"vmD$A.@Qm/n7#9H[lf5h\">i</variable>\n"
         + "    <variable type=\"5c5b3f01b9437f682e35d3b5_document\" id=\"htj,W_{y[i#NwzoQ!U$W\">oldDocument</variable>\n"
         + "  </variables>\n"
         + "  <block type=\"get_attribute\" id=\"Y;8!va;ugQIbSzGFFKCp\" x=\"43\" y=\"21\">\n"
         + "    <field name=\"ATTR\">a1</field>\n"
         + "    <value name=\"DOCUMENT\">\n"
         + "      <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link\" id=\";p^MlH|U$AW`:;%(Q;t|\">\n"
         + "        <value name=\"DOCUMENT\">\n"
         + "          <block type=\"variables_get_5c5b3f01b9437f682e35d3b5_document\" id=\".li:)vGxD-oKU.=r:OqW\" editable=\"false\">\n"
         + "            <field name=\"VAR\" id=\"htj,W_{y[i#NwzoQ!U$W\" variabletype=\"5c5b3f01b9437f682e35d3b5_document\">oldDocument</field>\n"
         + "          </block>\n"
         + "        </value>\n"
         + "      </block>\n"
         + "    </value>\n"
         + "  </block>\n"
         + "  <block type=\"statement_container\" id=\"POz{9m#c(nQ6BaCwvv?f\" deletable=\"false\" x=\"108\" y=\"83\"></block>\n"
         + "  <block type=\"get_attribute\" id=\"M-Y4:uqv@TZ7}z?UmJwj\" x=\"153\" y=\"213\">\n"
         + "    <field name=\"ATTR\">a3</field>\n"
         + "    <value name=\"DOCUMENT\">\n"
         + "      <block type=\"variables_get_5c5b3f01b9437f682e35d3b5_document\" id=\"z?C46ky2;E{|MPe%yg_v\" editable=\"false\">\n"
         + "        <field name=\"VAR\" id=\"htj,W_{y[i#NwzoQ!U$W\" variabletype=\"5c5b3f01b9437f682e35d3b5_document\">oldDocument</field>\n"
         + "      </block>\n"
         + "    </value>\n"
         + "  </block>\n"
         + "  <block type=\"lists_length\" id=\"xtlFH3agvZBzIh4sTKYD\" x=\"37\" y=\"281\">\n"
         + "    <value name=\"VALUE\">\n"
         + "      <block type=\"get_attribute\" id=\"M*`q@i|N]vM?0t@@}Y0M\">\n"
         + "        <field name=\"ATTR\">a4</field>\n"
         + "        <value name=\"DOCUMENT\">\n"
         + "          <block type=\"6c5b6a73b9437f682e35d3ba-6c5b3f01b9437f682e35d3b5_6c5b3f08b9437f682e35d3b7_link\" id=\"N:5(]ib+gAzq**Zh9KBE\">\n"
         + "            <value name=\"DOCUMENT\">\n"
         + "              <block type=\"variables_get_6c5b3f01b9437f682e35d3b5_document\" id=\"()?mhL#9R}TUWd))pO-z\" editable=\"false\">\n"
         + "                <field name=\"VAR\" id=\"htj,W_{y[i#NwzoQ!U$W\" variabletype=\"6c5b3f01b9437f682e35d3b5_document\">oldDocument</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </value>\n"
         + "      </block>\n"
         + "    </value>\n"
         + "  </block>\n"
         + "  <block type=\"math_on_list\" id=\";BZp[kMNv!6,##-MS;_+\" x=\"35\" y=\"358\">\n"
         + "    <mutation op=\"SUM\"></mutation>\n"
         + "    <field name=\"OP\">SUM</field>\n"
         + "    <value name=\"LIST\">\n"
         + "      <block type=\"get_attribute\" id=\"1}0R4y*|83|~#b;hKKdS\">\n"
         + "        <field name=\"ATTR\">a2</field>\n"
         + "        <value name=\"DOCUMENT\">\n"
         + "          <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link\" id=\"}/ZHmn$oeWK+)e`OGsY5\">\n"
         + "            <value name=\"DOCUMENT\">\n"
         + "              <block type=\"variables_get_5c5b3f01b9437f682e35d3b5_document\" id=\"UD{W?$BWGH(:.n[NrE2a\" editable=\"false\">\n"
         + "                <field name=\"VAR\" id=\"+I50nE%$pl-gStL+#Ga4\" variabletype=\"5c5b3f01b9437f682e35d3b5_document\">newDocument</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </value>\n"
         + "      </block>\n"
         + "    </value>\n"
         + "  </block>\n"
         + "</xml>\n";

   @Test
   public void parseFunctionXml() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xml);

      assertThat(attributeReferences).hasSize(4);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference("a1", "5c5b3f08b9437f682e35d3b7", "5c5b6a73b9437f682e35d3ba"),
            new FunctionXmlParser.AttributeReference("a3", "5c5b3f01b9437f682e35d3b5", null),
            new FunctionXmlParser.AttributeReference("a4", "6c5b3f08b9437f682e35d3b7", "6c5b6a73b9437f682e35d3ba"),
            new FunctionXmlParser.AttributeReference("a2", "5c5b3f08b9437f682e35d3b7", "5c5b6a73b9437f682e35d3ba")
      );
   }
}