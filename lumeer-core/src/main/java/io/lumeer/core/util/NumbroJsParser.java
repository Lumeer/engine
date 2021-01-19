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

import io.lumeer.core.constraint.config.NumberConstraintConfig;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NumbroJsParser implements AutoCloseable {

   public static final String FORMAT_NUMBER = "formatNumber";
   public static final String FORMAT_CURRENCY = "formatCurrency";

   private static Context context = null;
   private static Value formatNumberJs;
   private static Value formatCurrencyJs;
   private static String numbroJsCode;
   private static Engine engine = Engine
         .newBuilder()
         .allowExperimentalOptions(true)
         .option("js.experimental-foreign-object-prototype", "true")
         .option("js.foreign-object-prototype", "true")
         .build();

   static {
      try (var stream = NumbroJsParser.class.getResourceAsStream("/numbro.min.js")) {
         numbroJsCode = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         numbroJsCode +=
               "; function " + FORMAT_NUMBER + "(value, config) { return numbro(value).format(JSON.parse(config)); }" +
                     "; function " + FORMAT_CURRENCY + "(value, config, locale) { return numbro(value).formatCurrency(JSON.parse(config)); }";
      } catch (IOException ioe) {
         numbroJsCode = null;
      }
   }

   public static String formatNumber(final BigDecimal number, final NumberConstraintConfig numberConstraintConfig) {
      if (context == null) {
         initContext();
      }

      try {
         var numbroConfig = parseNumbroConfig(numberConstraintConfig);
         var result = formatNumberJs.execute(number.toString(), new JSONObject(numbroConfig).toString());

         return result.asString();
      } catch (Exception e) {
         return null;
      }
   }

   public static String formatCurrency(final BigDecimal number, final NumberConstraintConfig numberConstraintConfig) {
      if (context == null) {
         initContext();
      }

      try {
         var numbroConfig = parseNumbroConfig(numberConstraintConfig);
         var result = formatCurrencyJs.execute(number.toString(), new JSONObject(numbroConfig).toString());

         return result.asString();
      } catch (Exception e) {
         return null;
      }
   }

   private static Map<String, Object> parseNumbroConfig(NumberConstraintConfig numberConstraintConfig) {
      Map<String, Object> config = new HashMap<>();
      if (numberConstraintConfig.forceSign != null) {
         config.put("forceSign", numberConstraintConfig.forceSign);
      }
      if (numberConstraintConfig.separated != null) {
         config.put("thousandSeparated", numberConstraintConfig.separated);
         config.put("spaceSeparated", numberConstraintConfig.separated);
      }
      if (numberConstraintConfig.compact != null) {
         config.put("average", numberConstraintConfig.compact);
      }
      if (numberConstraintConfig.negative != null) {
         config.put("negative", "parenthesis");
      }
      if (numberConstraintConfig.decimals != null && numberConstraintConfig.decimals >= 0) {
         config.put("mantissa", numberConstraintConfig.decimals);
         config.put("trimMantissa", true);
      }
      return config;
   }

   private synchronized static void initContext() {
      if (context == null) {

         if (numbroJsCode != null) {
            context = Context
                  .newBuilder("js")
                  .engine(engine)
                  .allowAllAccess(true)
                  .build();
            context.initialize("js");

            var result = context.eval("js", numbroJsCode);

            formatNumberJs = context.getBindings("js").getMember(FORMAT_NUMBER);
            formatCurrencyJs = context.getBindings("js").getMember(FORMAT_CURRENCY);
         }
      }
   }

   public void close() {
      context.close();
   }
}
