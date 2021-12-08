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
package io.lumeer.core.pdf

import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PdfCreator {
   companion object {

      @JvmStatic
      @Throws(IOException::class)
      fun createPdf(src: InputStream, dest: OutputStream) {
         val properties = ConverterProperties()
         val writer = PdfWriter(dest, WriterProperties().setFullCompressionMode(true))
         val pdf = PdfDocument(writer)
         pdf.setTagged()
         HtmlConverter.convertToPdf(src, pdf, properties)
      }
   }
}