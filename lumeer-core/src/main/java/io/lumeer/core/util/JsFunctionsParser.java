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

import io.lumeer.core.js.JsEngineFactory;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsFunctionsParser implements AutoCloseable {

   public static final String FORMAT_JS_DATE = "formatMomentJsDate";
   public static final String FORMAT_JS_UTC_DATE = "formatMomentJsUtcDate";
   public static final String PARSE_JS_DATE = "parseMomentJsDate";
   public static final String PARSE_JS_UTC_DATE = "parseMomentJsUtcDate";

   private static Context context = null;
   private static Value formatMomentJsDate;
   private static Value formatMomentJsUtcDate;
   private static Value parseMomentJsDate;
   private static Value parseMomentJsUtcDate;
   private static String momentJsCode;
   private static String heJsCode;
   private static String numbroJsCode;
   private static final Engine engine = JsEngineFactory.getEngine();

   static {
      try (
            var stream = JsFunctionsParser.class.getResourceAsStream("/moment-with-locales.min.js");
            var stream2 = JsFunctionsParser.class.getResourceAsStream("/moment-business.min.js");
            var stream3 = JsFunctionsParser.class.getResourceAsStream("/he.min.js");
            var stream4 = JsFunctionsParser.class.getResourceAsStream("/numbro.min.js");
            var stream5 = JsFunctionsParser.class.getResourceAsStream("/numbro-languages.min.js");
      ) {
         momentJsCode = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         momentJsCode += new String(stream2.readAllBytes(), StandardCharsets.UTF_8);
         momentJsCode +=
               "; function " + FORMAT_JS_DATE  + "(time, format, locale) { return moment(time).locale(locale).format((format || '').replace(/'/g, '\\\\\\'')); }" +
               "; function " + FORMAT_JS_DATE  + "(time, format, locale) { return moment(time).locale(locale).format((format || '').replace(/'/g, '\\\\\\'')); }" +
               "; function " + PARSE_JS_DATE + "(date, format, locale) { return moment((date || '').replace(/'/g, '\\\\\\''), (format || '').replace(/'/g, '\\\\\\''), locale).valueOf(); } " +
               "; function " + PARSE_JS_UTC_DATE + "(date, format, locale) { return moment.utc((date || '').replace(/'/g, '\\\\\\''), (format || '').replace(/'/g, '\\\\\\''), locale).valueOf(); } ";
         heJsCode = new String(stream3.readAllBytes(), StandardCharsets.UTF_8);
         numbroJsCode = new String(stream4.readAllBytes(), StandardCharsets.UTF_8);
         numbroJsCode += new String(stream5.readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException ioe) {
         momentJsCode = null;
      }
   }

   public static String getMomentJsCode() {
      return momentJsCode;
   }

   public static String getHeJsCode() {
      return heJsCode;
   }

   public static String getNumbroJsCode() {
      return numbroJsCode;
   }

   public static Long parseMomentJsDate(final String date, final String format, final String locale, final Boolean utc) {
      if (context == null) {
         initContext();
      }

      try {
         var result = utc ? parseMomentJsUtcDate.execute(date, format, locale) : parseMomentJsDate.execute(date, format, locale);

         if (result.isNull()) {
            return null;
         }

         if (result.isString()) {
            return null;
         }

         if (result.isNumber()) {
            double x = result.asDouble();

            if (Double.isNaN(x)) {
               return null;
            }

            if (result.fitsInLong()) {
               return result.asLong();
            }
         }
      } catch (Exception e) {
         return null;
      }

      return null;
   }

   public static String formatMomentJsDate(final long time, final String format, final String locale, final Boolean utc) {
      if (context == null) {
         initContext();
      }

      try {
         var result = utc ? formatMomentJsUtcDate.execute(time, format, locale) : formatMomentJsDate.execute(time, format, locale);

         return result.asString();
      } catch (Exception e) {
         return null;
      }
   }

   private synchronized static void initContext() {
      if (context == null) {

         if (momentJsCode != null) {
            context = Context
                  .newBuilder("js")
                  .engine(engine)
                  .allowAllAccess(true)
                  .build();
            context.initialize("js");

            var result = context.eval("js", momentJsCode);

            formatMomentJsDate = context.getBindings("js").getMember(FORMAT_JS_DATE);
            formatMomentJsUtcDate = context.getBindings("js").getMember(FORMAT_JS_UTC_DATE);
            parseMomentJsDate = context.getBindings("js").getMember(PARSE_JS_DATE);
            parseMomentJsUtcDate = context.getBindings("js").getMember(PARSE_JS_UTC_DATE);
         }
      }
   }

   public void close() {
      context.close();
   }
}
