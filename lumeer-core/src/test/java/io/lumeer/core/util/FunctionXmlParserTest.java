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

import org.junit.jupiter.api.Test;

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

   private final static String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
         + "<xml xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         + "   <variables>\n"
         + "      <variable type=\"5c6ea86e9e9ffa355dffe274_document\" id=\"`Y.|2l%)p4/S8:c)OX]W\">thisDocument</variable>\n"
         + "   </variables>\n"
         + "   <block type=\"value_container\" id=\"K%pQ5pJt]{RuElJxg-MZ\" deletable=\"false\" x=\"85\" y=\"172\">\n"
         + "      <value name=\"VALUE\">\n"
         + "         <block type=\"math_arithmetic\" id=\"OinGgg~N5T=Y4BHF-B(f\">\n"
         + "            <field name=\"OP\">ADD</field>\n"
         + "            <value name=\"A\">\n"
         + "               <shadow type=\"math_number\" id=\"PfIu4{[AQFtuD9V[52_2\">\n"
         + "                  <field name=\"NUM\">1</field>\n"
         + "               </shadow>\n"
         + "               <block type=\"get_attribute\" id=\",V:g_fM0Ws~,ym=`JDb]\">\n"
         + "                  <field name=\"ATTR\">a2</field>\n"
         + "                  <value name=\"DOCUMENT\">\n"
         + "                     <block type=\"variables_get_5c6ea86e9e9ffa355dffe274_document\" id=\"X8wPwMXQ5}?J#60]`zfu\" editable=\"false\">\n"
         + "                        <field name=\"VAR\" id=\"`Y.|2l%)p4/S8:c)OX]W\" variabletype=\"5c6ea86e9e9ffa355dffe274_document\">thisDocument</field>\n"
         + "                     </block>\n"
         + "                  </value>\n"
         + "               </block>\n"
         + "            </value>\n"
         + "            <value name=\"B\">\n"
         + "               <shadow type=\"math_number\" id=\"`x$#OLP)`rR3L18r/CPM\">\n"
         + "                  <field name=\"NUM\">1</field>\n"
         + "               </shadow>\n"
         + "               <block type=\"math_on_list\" id=\"^yhK,t~Ig3#?lo-|%Zjg\">\n"
         + "                  <mutation op=\"SUM\" />\n"
         + "                  <field name=\"OP\">SUM</field>\n"
         + "                  <value name=\"LIST\">\n"
         + "                     <block type=\"get_attribute\" id=\"]cm`KH:#OBfV=k(2[5Nw\">\n"
         + "                        <field name=\"ATTR\">a4</field>\n"
         + "                        <value name=\"DOCUMENT\">\n"
         + "                           <block type=\"5c6f3b389e9ffa43fe6a2dcc-5becabd79e9ffa0a690775ea_5c6ea86e9e9ffa355dffe274_link\" id=\"MN[2Ktf)HJaC#/^;EUMD\">\n"
         + "                              <value name=\"DOCUMENT\">\n"
         + "                                 <block type=\"variables_get_5c6ea86e9e9ffa355dffe274_document\" id=\"W7sJok~+kMt-AWNh1fGw\" editable=\"false\">\n"
         + "                                    <field name=\"VAR\" id=\"`Y.|2l%)p4/S8:c)OX]W\" variabletype=\"5c6ea86e9e9ffa355dffe274_document\">thisDocument</field>\n"
         + "                                 </block>\n"
         + "                              </value>\n"
         + "                           </block>\n"
         + "                        </value>\n"
         + "                     </block>\n"
         + "                  </value>\n"
         + "               </block>\n"
         + "            </value>\n"
         + "         </block>\n"
         + "      </value>\n"
         + "   </block>\n"
         + "</xml>";

   private static final String xml3 = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">"
         + "<variables>"
         + "<variable type=\"5c5b3f01b9437f682e35d3b5_document\" id=\"[eC`/hJtnp7pu,n[tN}N\">thisDocument</variable>"
         + "</variables>"
         + "<block type=\"value_container\" id=\"kFVlLKE.|V^{mg:+ZLvu\" deletable=\"false\" x=\"36\" y=\"252\">"
         + "<value name=\"VALUE\"><block type=\"math_on_list\" id=\"?$,qOwaz`c.l,o/{a,c)\"><mutation"
         + " op=\"SUM\"></mutation><field name=\"OP\">SUM</field><value name=\"LIST\">"
         + "<block type=\"get_attribute\" id=\"MCfOP6qK@sG=DxDtO.i%\">"
         + "  <field name=\"ATTR\">a2</field>"
         + "  <value name=\"DOCUMENT\">"
         + "    <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link\" id=\"|zHLK3j{ri%ih6mEyC?a\">"
         + "      <value name=\"DOCUMENT\">"
         + "        <block type=\"variables_get_5c5b3f01b9437f682e35d3b5_document\" id=\"),+i~8GeULoD+gIFgvoC\" editable=\"false\">"
         + "           <field name=\"VAR\" id=\"[eC`/hJtnp7pu,n[tN}N\" variabletype=\"5c5b3f01b9437f682e35d3b5_document\">thisDocument</field>"
         + "        </block>"
         + "      </value>"
         + "    </block>"
         + "  </value>"
         + "</block></value></block></value></block></xml>";

   private static final String xml4 = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         + "  <variables>\n"
         + "    <variable type=\"5c5b6a73b9437f682e35d3ba_linkinst\" id=\"_pnvXwp)j1P#p6l@7BQ3\">i</variable>\n"
         + "    <variable type=\"5c5b3f08b9437f682e35d3b7_document\" id=\"0~ocwVN9mrE0u89P4D-c\">newDocument</variable>\n"
         + "  </variables>\n"
         + "  <block type=\"statement_container\" id=\"nOG}9EJB$QvgR]E2ML@i\" deletable=\"false\" x=\"62\" y=\"95\">\n"
         + "    <statement name=\"COMMANDS\">\n"
         + "      <block type=\"foreach_link_array\" id=\"clPR,3gM@bv-AY!|/SEl\">\n"
         + "        <field name=\"VAR\" id=\"_pnvXwp)j1P#p6l@7BQ3\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "        <value name=\"LIST\">\n"
         + "          <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link_instance\" id=\"ZbhD}C*]%4UPQxR=Ea1D\">\n"
         + "            <value name=\"DOCUMENT\">\n"
         + "              <block type=\"variables_get_5c5b3f08b9437f682e35d3b7_document\" id=\"74/lJW5CNqc4S);[l.W*\" editable=\"false\">\n"
         + "                <field name=\"VAR\" id=\"0~ocwVN9mrE0u89P4D-c\" variabletype=\"5c5b3f08b9437f682e35d3b7_document\">newDocument</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </value>\n"
         + "        <statement name=\"DO\">\n"
         + "          <block type=\"set_link_attribute\" id=\"+2nbE|(GY(:9:@OTm%@,\">\n"
         + "            <field name=\"ATTR\">a3</field>\n"
         + "            <value name=\"LINK\">\n"
         + "              <block type=\"variables_get_5c5b6a73b9437f682e35d3ba_linkinst\" id=\"u]_[/d,nu43o0|We($Qu\">\n"
         + "                <field name=\"VAR\" id=\"_pnvXwp)j1P#p6l@7BQ3\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "            <value name=\"VALUE\">\n"
         + "              <block type=\"get_link_attribute\" id=\"6F$k#|%-8srAB,%qzxG`\">\n"
         + "                <field name=\"ATTR\">a2</field>\n"
         + "                <value name=\"LINK\">\n"
         + "                  <block type=\"variables_get_5c5b6a73b9437f682e35d3ba_linkinst\" id=\"5^iVIrYD?sf(QagM;8dh\">\n"
         + "                    <field name=\"VAR\" id=\"_pnvXwp)j1P#p6l@7BQ3\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "                  </block>\n"
         + "                </value>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </statement>\n"
         + "      </block>\n"
         + "    </statement>\n"
         + "  </block>\n"
         + "</xml>";
   private static final String xml5 = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         + "  <variables>\n"
         + "    <variable type=\"5c5b3f08b9437f682e35d3b7_document\" id=\"q(Q/hjabhq[0?v64!kBq\">j</variable>\n"
         + "    <variable type=\"5c5b6a73b9437f682e35d3ba_linkinst\" id=\"vwtI@x)]$3ur2**~%Yt0\">i</variable>\n"
         + "    <variable type=\"5c5b3f08b9437f682e35d3b7_document\" id=\"~6;eI~ONP33SfRQdPuOt\">newDocument</variable>\n"
         + "  </variables>\n"
         + "  <block type=\"statement_container\" id=\"-?AQ.,AxwlPnB15)a4^4\" deletable=\"false\" x=\"83\" y=\"105\">\n"
         + "    <statement name=\"COMMANDS\">\n"
         + "      <block type=\"foreach_link_array\" id=\"zb}U-9g/X_LVrW]3FmD~\">\n"
         + "        <field name=\"VAR\" id=\"vwtI@x)]$3ur2**~%Yt0\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "        <value name=\"LIST\">\n"
         + "          <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link_instance\" id=\"rQnJ|g%,#]Ee:t3vN:y=\">\n"
         + "            <value name=\"DOCUMENT\">\n"
         + "              <block type=\"variables_get_5c5b3f08b9437f682e35d3b7_document\" id=\"#ixZ_Sgz_u^N2aGlGe(N\" editable=\"false\">\n"
         + "                <field name=\"VAR\" id=\"~6;eI~ONP33SfRQdPuOt\" variabletype=\"5c5b3f08b9437f682e35d3b7_document\">newDocument</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </value>\n"
         + "        <statement name=\"DO\">\n"
         + "          <block type=\"set_link_attribute\" id=\"O=Y!GFL4`g]?9ayg,3+D\">\n"
         + "            <field name=\"ATTR\">a8</field>\n"
         + "            <value name=\"LINK\">\n"
         + "              <block type=\"variables_get_5c5b6a73b9437f682e35d3ba_linkinst\" id=\"oDp8D(hSypCNVd4wddX{\">\n"
         + "                <field name=\"VAR\" id=\"vwtI@x)]$3ur2**~%Yt0\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "            <value name=\"VALUE\">\n"
         + "              <block type=\"text_join\" id=\"N(5|BcXLG]Jz-Ve3^CN/\">\n"
         + "                <mutation items=\"2\"></mutation>\n"
         + "                <value name=\"ADD0\">\n"
         + "                  <block type=\"text\" id=\"`,b,?euBoED+n[I_C{Id\">\n"
         + "                    <field name=\"TEXT\">dfadf</field>\n"
         + "                  </block>\n"
         + "                </value>\n"
         + "                <value name=\"ADD1\">\n"
         + "                  <block type=\"get_attribute\" id=\"#X=kCvK={(W[w{ZzE%3l\">\n"
         + "                    <field name=\"ATTR\">a4</field>\n"
         + "                    <value name=\"DOCUMENT\">\n"
         + "                      <block type=\"get_link_document\" id=\"$=fS@zys2h|^G,|gN8:$\">\n"
         + "                        <field name=\"COLLECTION\">5c5b3f01b9437f682e35d3b5</field>\n"
         + "                        <value name=\"LINK\">\n"
         + "                          <block type=\"variables_get_5c5b6a73b9437f682e35d3ba_linkinst\" id=\"HOmT{,^*Dhj+xW{{gc;c\">\n"
         + "                            <field name=\"VAR\" id=\"vwtI@x)]$3ur2**~%Yt0\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "                          </block>\n"
         + "                        </value>\n"
         + "                      </block>\n"
         + "                    </value>\n"
         + "                  </block>\n"
         + "                </value>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </statement>\n"
         + "      </block>\n"
         + "    </statement>\n"
         + "  </block>\n"
         + "  <block type=\"foreach_document_array\" id=\"x32iHg7Is`}t-kV:E135\" x=\"17\" y=\"316\">\n"
         + "    <field name=\"VAR\" id=\"q(Q/hjabhq[0?v64!kBq\" variabletype=\"5c5b3f08b9437f682e35d3b7_document\">j</field>\n"
         + "    <value name=\"LIST\">\n"
         + "      <block type=\"5c5b6a73b9437f682e35d3ba-5c5b3f01b9437f682e35d3b5_5c5b3f08b9437f682e35d3b7_link\" id=\"6;@rMaamIss-=N$(A7Y*\">\n"
         + "        <value name=\"DOCUMENT\">\n"
         + "          <block type=\"get_link_document\" id=\"L3X9J|_gZ7m|BkiJ8U;-\">\n"
         + "            <field name=\"COLLECTION\">5c5b3f01b9437f682e35d3b5</field>\n"
         + "            <value name=\"LINK\">\n"
         + "              <block type=\"variables_get_5c5b6a73b9437f682e35d3ba_linkinst\" id=\"-bF+sIRy3ew13=Iv!C}M\">\n"
         + "                <field name=\"VAR\" id=\"vwtI@x)]$3ur2**~%Yt0\" variabletype=\"5c5b6a73b9437f682e35d3ba_linkinst\">i</field>\n"
         + "              </block>\n"
         + "            </value>\n"
         + "          </block>\n"
         + "        </value>\n"
         + "      </block>\n"
         + "    </value>\n"
         + "  </block>\n"
         + "</xml>";

   public static final String xml6 = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         + "<variables>\n"
         + "  <variable type=\"5d571fcfa9013c4e0ac956a5_document\" id=\"?!813Nf/LTDw3/}+`,-~\">thisDocument</variable>\n"
         + "</variables>\n"
         + "<block type=\"value_container\" id=\"mG|K9:QjP{_?Cm,1c7r7\" deletable=\"false\" x=\"65\" y=\"168\">\n"
         + "  <value name=\"VALUE\">\n"
         + "    <block type=\"math_on_list\" id=\"*#/giN0ErtM-s{]N6PKX\">\n"
         + "      <mutation op=\"SUM\"></mutation>\n"
         + "      <field name=\"OP\">SUM</field>\n"
         + "      <value name=\"LIST\">\n"
         + "        <block type=\"get_link_attribute\" id=\"D4bv!0gf)B`,jzSCgB*!\">\n"
         + "          <field name=\"ATTR\">a1</field>\n"
         + "          <value name=\"LINK\">\n"
         + "            <block type=\"5d81646d652b2314cb092faa-5d571fcfa9013c4e0ac956a5_5d66eb96b08a0d090c2efee6_link_instance\" id=\"C|mkp?aPa+~@=q@`T;^+\">\n"
         + "              <value name=\"DOCUMENT\">\n"
         + "                <block type=\"variables_get_5d571fcfa9013c4e0ac956a5_document\" id=\"]!70YjA8TRvb789Bk,^5\" editable=\"false\">\n"
         + "                  <field name=\"VAR\" id=\"?!813Nf/LTDw3/}+`,-~\" variabletype=\"5d571fcfa9013c4e0ac956a5_document\">thisDocument</field>\n"
         + "                </block>\n"
         + "              </value>\n"
         + "            </block>\n"
         + "          </value>\n"
         + "        </block>\n"
         + "      </value>\n"
         + "    </block>\n"
         + "  </value>\n"
         + "</block>\n"
         + "</xml>";

   public static final String xmlGetRecordsCount = "<xml xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         + "    <variables>\n"
         + "        <variable type=\"621104e99a5c9b46d82c37d7_document\" id=\"*ujL$F,nIoZR0.=d{zRJ\">thisRecord</variable>\n"
         + "    </variables>\n"
         + "    <block type=\"value_container\" id=\"/-b*VyMcre),o[))N7k~\" deletable=\"false\" x=\"126\" y=\"243\">\n"
         + "        <value name=\"VALUE\">\n"
         + "            <block type=\"get_records_count\" id=\"XTBu^2R?7K?+vuZK#]*I\">\n"
         + "                <value name=\"RECORDS\">\n"
         + "                    <block type=\"62a8e6362b86be322c7c8934-621104e99a5c9b46d82c37d7_62a4616a67ebf01f3f8ecc73_link\" id=\"KF2r@y@QH8P+y/nhEHt:\">\n"
         + "                        <value name=\"DOCUMENT\">\n"
         + "                            <block type=\"variables_get_621104e99a5c9b46d82c37d7_document\" id=\"y/Gi.:A^01SS?h,C7F6C\" editable=\"false\">\n"
         + "                                <field name=\"VAR\" id=\"*ujL$F,nIoZR0.=d{zRJ\" variabletype=\"621104e99a5c9b46d82c37d7_document\">thisRecord</field>\n"
         + "                            </block>\n"
         + "                        </value>\n"
         + "                    </block>\n"
         + "                </value>\n"
         + "            </block>\n"
         + "        </value>\n"
         + "    </block>\n"
         + "</xml>";

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

   @Test
   public void parseFunctionXml2() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xml2);

      assertThat(attributeReferences).hasSize(2);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference("a2", "5c6ea86e9e9ffa355dffe274", null),
            new FunctionXmlParser.AttributeReference("a4", "5becabd79e9ffa0a690775ea", "5c6f3b389e9ffa43fe6a2dcc")
      );
   }

   @Test
   public void parseFunctionXml3() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xml3);

      assertThat(attributeReferences).hasSize(1);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference("a2", "5c5b3f08b9437f682e35d3b7", "5c5b6a73b9437f682e35d3ba")
      );
   }

   @Test
   public void parseFunctionXml4() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xml4);

      assertThat(attributeReferences).hasSize(1);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference("a2", null, "5c5b6a73b9437f682e35d3ba")
      );
   }

   @Test
   public void parseFunctionXml5() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xml5);

      assertThat(attributeReferences).hasSize(1);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference("a4", "5c5b3f01b9437f682e35d3b5", "5c5b6a73b9437f682e35d3ba")
      );
   }

   @Test
   public void parseFunctionXml6() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xml6);

      assertThat(attributeReferences).hasSize(1);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference("a1", null, "5d81646d652b2314cb092faa")
      );
   }

   @Test
   public void parseGetRecordsCount() {
      List<FunctionXmlParser.AttributeReference> attributeReferences = FunctionXmlParser.parseFunctionXml(xmlGetRecordsCount);

      assertThat(attributeReferences).hasSize(1);
      assertThat(attributeReferences).contains(
            new FunctionXmlParser.AttributeReference(null, "62a4616a67ebf01f3f8ecc73", "62a8e6362b86be322c7c8934")
      );
   }

}
