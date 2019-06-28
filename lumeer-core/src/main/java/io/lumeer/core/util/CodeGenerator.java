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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Generates 16 character long hexadecimal unique code based on given token.
 */
public class CodeGenerator {

   static final int LENGTH = 16;

   private static final String ALGORITHM = "SHA-256";

   public static String generate(Set<String> existingCodes, String token) {
      int counter = 0;
      String code = hash(token);
      while (existingCodes.contains(code)) {
         code = hash(token + counter++);
      }
      return code;
   }

   private static String hash(String token) {
      try {
         MessageDigest md = MessageDigest.getInstance(ALGORITHM);
         byte[] binaryHash = md.digest(token.getBytes(StandardCharsets.UTF_8));
         String hash = (new HexBinaryAdapter()).marshal(binaryHash);
         return hash.substring(0, LENGTH).toLowerCase();
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }
}
