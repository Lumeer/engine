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
package io.lumeer.core.util.pdf

import io.lumeer.core.pdf.PdfCreator
import org.junit.Assert.*
import org.junit.Test
import software.amazon.awssdk.utils.StringInputStream
import java.io.FileOutputStream

class PdfCreatorTest {

   @Test
   fun testPdfCreator() {
      val fos = FileOutputStream("test.pdf")

      fos.use { fileInputStream ->
         val sis = StringInputStream("<html><head><style>\n" +
               "body {\n" +
               "  background-color: linen;\n" +
               "  font-family: Times New Roman, serif;\n" +
               "  font-size: 12px;\n" +
               "}\n" +
               "\n" +
               "h1 {\n" +
               "  color: maroon;\n" +
               "  margin-left: 40px;\n" +
               "}\n" +
               "</style></head><body><h1>Hello</h2><p>World!</p><table><tr><td>Invoice</td><td>takětotoščbudeřždivnéýáíé</td></tr></table></body></html>")

         sis.use { stringInputStream ->
            PdfCreator.createPdf(stringInputStream, fileInputStream)
         }
      }
   }
}