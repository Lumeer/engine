/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
