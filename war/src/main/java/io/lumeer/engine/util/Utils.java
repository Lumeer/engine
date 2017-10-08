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
package io.lumeer.engine.util;

import io.lumeer.engine.api.LumeerConst;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
public class Utils {

   private static final String DATE_FORMAT = "yyyy.MM.dd HH.mm.ss.SSS";
   private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

   private Utils() {
      // to prevent initialization
   }

   public static String getCurrentTimeString() {
      return dateFormat.format(new Date());
   }

   public static String normalize(String string) {
      if (string == null) {
         return null;
      }
      String s = string.toLowerCase();
      return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
   }

   public static boolean isAttributeNameValid(String attributeName) {
      return attributeName.equals(LumeerConst.Document.ID) || !(attributeName.startsWith("$") || attributeName.startsWith("_") || attributeName.contains("."));
   }
}
