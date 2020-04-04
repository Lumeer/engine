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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MomentJsParser implements AutoCloseable {

   private static Context context = null;
   private static Value formatMomentJsDate;
   private static Value parseMomentJsDate;

   public static Long parseMomentJsDate(final String date, final String format, final String locale) {
      if (context == null) {
         initContext();
      }

      try {
         var result = parseMomentJsDate.execute(date.replaceAll("'", "\\'"), format.replaceAll("'", "\\'"), locale);

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

   public static String formatMomentJsDate(final long time, final String format, final String locale) {
      if (context == null) {
         initContext();
      }

      try {
         var result = formatMomentJsDate.execute(time, format.replaceAll("'", "\\'"), locale);

         return result.asString();
      } catch (Exception e) {
         return null;
      }
   }

   private synchronized static void initContext() {
      if (context == null) {

         String js;
         try (var stream = MomentJsParser.class.getResourceAsStream("/moment-with-locales.min.js")) {
            js = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         } catch (IOException ioe) {
            js = null;
         }

         if (js != null) {
            context = Context
                  .newBuilder("js")
                  .engine(Engine
                        .newBuilder()
                        .allowExperimentalOptions(true)
                        .option("js.experimental-foreign-object-prototype", "true")
                        .build())
                  .allowAllAccess(true)
                  .build();
            context.initialize("js");

            var result = context.eval("js", js +
                  "; function formatMomentJsDate(time, format, locale) { return moment(time).locale(locale).format(format); }" +
                  "; function parseMomentJsDate(date, format, locale) { return moment(date, format, locale).valueOf(); } ");

            formatMomentJsDate = context.getBindings("js").getMember("formatMomentJsDate");
            parseMomentJsDate = context.getBindings("js").getMember("parseMomentJsDate");
         }
      }
   }

   public void close() {
      context.close();
   }
}
