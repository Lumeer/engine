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

import io.lumeer.core.exception.BadFormatException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.zip.CRC32;

public abstract class Utils {

   public static boolean isCodeSafe(final String code) {
      return code.matches("[A-Za-z0-9_]*");
   }

   public static void checkCodeSafe(final String code) {
      if (!isCodeSafe(code)) {
         throw new BadFormatException("Invalid characters. Only A-Z, a-z, 0-9, _ are allowed in code.");
      }
   }

   public static boolean isEmpty(final String str) {
      return str == null || "".equals(str);
   }

   public static <T> T firstNotNullElement(List<T> list) {
      for (T element : list) {
         if (element != null) {
            return element;
         }
      }
      return null;
   }

   public static String strHexTo36(final String hex) {
      return new BigInteger(hex, 16).toString(36);
   }

   public static String str36toHex(final String str36) {
      return new BigInteger(str36, 36).toString(16);
   }

   public static String strToBase64(final String input) {
      return Base64.getUrlEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8)).replaceFirst("=*$", "");
   }

   public static String base64ToStr(final String input) {
      final byte[] decodedBytes = Base64.getUrlDecoder().decode(input);
      return new String(decodedBytes, StandardCharsets.UTF_8);
   }

   public static String strCrc32(final String input) {
      final CRC32 crc = new CRC32();
      crc.update(input.getBytes(StandardCharsets.UTF_8));
      final long crcNumber = crc.getValue() + Double.valueOf(Math.pow(16, 8) / 2).longValue();
      final String str = String.format("%08x", crcNumber);

      if (str.length() > 8) {
         return str.substring(str.length() - 8);
      }

      return str;
   }

   public static String encodeQueryParam(final String param) {
      final String b64 = strToBase64(param);
      return b64 + strCrc32(b64);
   }

   public static String decodeQueryParam(final String param) {
      final String crc = param.substring(param.length() - 8);
      final String b64 = param.substring(0, param.length() - 8);

      if (crc.equals(strCrc32(b64))) {
         return base64ToStr(b64);
      }

      return "";
   }

   public static <T, R> T computeIfNotNull(final R value, final Function<R, T> function) {
      if (value != null) {
         return function.apply(value);
      }

      return null;
   }
}
