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

import io.lumeer.core.js.AutomationFunctions;

/** Date formatting and parsing for the Java code (converters, e-mails), with the moment.js semantics automations had. */
public class DateFormats implements AutoCloseable {

   /** moment(date, format, locale).valueOf() (moment.utc when utc), null when invalid. */
   public static Long parse(final String date, final String format, final String locale, final Boolean utc) {
      return AutomationFunctions.parseDateForJava(date, format, locale, Boolean.TRUE.equals(utc));
   }

   /** moment(time).locale(locale).format(format) (moment.utc when utc), null on an error. */
   public static String format(final long time, final String format, final String locale, final Boolean utc) {
      return AutomationFunctions.formatDateForJava(time, format, locale, Boolean.TRUE.equals(utc));
   }

   public void close() {
   }
}
