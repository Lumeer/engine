package io.lumeer.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
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
         + "    <field name=\"ATTR\">a1</field>\n"
         + "    <value name=\"DOCUMENT\">\n"
         + "      <block type=\"variables_get_5c5b3f01b9437f682e35d3b5_document\" id=\"z?C46ky2;E{|MPe%yg_v\" editable=\"false\">\n"
         + "        <field name=\"VAR\" id=\"htj,W_{y[i#NwzoQ!U$W\" variabletype=\"5c5b3f01b9437f682e35d3b5_document\">oldDocument</field>\n"
         + "      </block>\n"
         + "    </value>\n"
         + "  </block>\n"
         + "  <block type=\"lists_length\" id=\"xtlFH3agvZBzIh4sTKYD\" x=\"37\" y=\"281\">\n"
         + "    <value name=\"VALUE\">\n"
         + "      <block type=\"get_attribute\" id=\"M*`q@i|N]vM?0t@@}Y0M\">\n"
         + "        <field name=\"ATTR\">a1</field>\n"
         + "        <value name=\"DOCUMENT\">\n"
         + "          <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link\" id=\"N:5(]ib+gAzq**Zh9KBE\">\n"
         + "            <value name=\"DOCUMENT\">\n"
         + "              <block type=\"variables_get_5c5b3f01b9437f682e35d3b5_document\" id=\"()?mhL#9R}TUWd))pO-z\" editable=\"false\">\n"
         + "                <field name=\"VAR\" id=\"htj,W_{y[i#NwzoQ!U$W\" variabletype=\"5c5b3f01b9437f682e35d3b5_document\">oldDocument</field>\n"
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
      FunctionXmlParser.parseFunctionXml(xml).forEach(System.out::println);
   }
}