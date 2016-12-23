/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.util;

import io.lumeer.engine.api.LumeerConst;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.ParseException;
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

   /**
    * @param date
    *       Date date we want to convert to string
    * @return String representation of Date
    */
   public static String getTimeString(Date date) {
      return dateFormat.format(date);
   }

   /**
    * @param date
    *       date string we want to convert to Date
    * @return Date representation of string
    * @throws ParseException
    *       when string cannot be parsed
    */
   public static Date getDate(String date) throws ParseException {
      return dateFormat.parse(date);
   }

   /**
    * Checks if date in string is in format yyyy.MM.dd HH.mm.ss.SSS
    *
    * @param dateString
    *       string with date to check
    * @return true if date string is in valid format, otherwise false
    */
   public static boolean isValidDateFormat(String dateString) {
      try {
         Date date = dateFormat.parse(dateString);
         if (!dateString.equals(dateFormat.format(date))) {
            return false;
         }
      } catch (ParseException ex) {
         return false;
      }
      return true;
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
